"""
WMS AI Platform - 语音拣货模块
功能：语音播报任务/语音确认/异常上报/多轮对话，解放双手
"""
import os
import sys
import json
import asyncio
from typing import Dict, Optional
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException
from pydantic import BaseModel
from loguru import logger

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
from common.config import settings
from common.ai_client import ai_client
from common.data_client import redis_client, data_client
from common.utils import Result, gen_id, now_str
from common.cache_metrics import setup_metrics
from common.tracing import init_tracing, instrument_app

app = FastAPI(title="WMS AI - 语音拣货模块", version="2.0.0")
setup_metrics(app)
init_tracing("voice")
instrument_app(app, "voice")


class VoicePickingService:
    """语音拣货服务"""

    def get_current_task(self, picker_id: str) -> Optional[Dict]:
        """获取当前拣货任务"""
        return redis_client.get_json(f"voice:task:{picker_id}")

    def get_task_from_wms(self, picker_id: str) -> Optional[Dict]:
        """
        从 WMS 查询拣货员当前波次任务（PRD 5.3）
        通过 data_client 查询 WMS MySQL pick_task 表
        查询结果回填 Redis 缓存
        """
        # 1. 先查 Redis 缓存
        cached = redis_client.get_json(f"voice:wms_task:{picker_id}")
        if cached:
            return cached

        # 2. 查询 WMS MySQL
        try:
            rows = data_client.query_mysql(
                "SELECT wave_id, location, sku, sku_name, qty, status "
                "FROM pick_task "
                "WHERE picker_id = %s AND status = 'assigned' "
                "ORDER BY pick_order LIMIT 1",
                params=[picker_id],
            )
            if rows:
                task = rows[0]
                redis_client.set_json(f"voice:wms_task:{picker_id}", task, expire=600)
                return task
        except Exception as e:
            logger.warning(f"从 WMS 查询拣货任务失败: {e}")
        return None

    def get_task_progress(self, picker_id: str) -> Dict:
        """
        获取任务进度（PRD 5.3）
        从 Redis 获取已完成/未完成任务计数，无数据时查 WMS
        """
        progress = redis_client.get_json(f"voice:progress:{picker_id}")
        if progress:
            return progress

        # 查 WMS 实际进度
        try:
            rows = data_client.query_mysql(
                "SELECT "
                "countIf(status = 'completed') AS done, "
                "countIf(status IN ('assigned', 'in_progress')) AS remaining, "
                "count() AS total "
                "FROM pick_task WHERE picker_id = %s AND wave_id = "
                "(SELECT wave_id FROM pick_task WHERE picker_id = %s AND status = 'assigned' LIMIT 1)",
                params=[picker_id, picker_id],
            )
            if rows:
                r = rows[0]
                progress = {
                    "done": int(r["done"]),
                    "remaining": int(r["remaining"]),
                    "total": int(r["total"]),
                }
                redis_client.set_json(f"voice:progress:{picker_id}", progress, expire=60)
                return progress
        except Exception as e:
            logger.warning(f"从 WMS 查询任务进度失败: {e}")

        # 降级默认
        return {"done": 0, "remaining": 0, "total": 0}

    def assign_task(self, picker_id: str, task: Dict):
        """分配任务"""
        task["picker_id"] = picker_id
        task["status"] = "in_progress"
        task["start_time"] = now_str()
        redis_client.set_json(f"voice:task:{picker_id}", task, expire=3600)

    async def generate_voice_prompt(self, task: Dict, step: str = "location") -> str:
        """生成语音播报内容"""
        if step == "location":
            return f"请到{task.get('area', '')}区{task.get('row', '')}排{task.get('layer', '')}层，库位编号{task.get('location', '')}"
        elif step == "confirm_location":
            return f"请复述库位编号{task.get('location', '')}"
        elif step == "product":
            return f"拣选商品{task.get('sku_name', '')}，数量{task.get('qty', '')}件"
        elif step == "complete":
            return f"确认完成，下一个任务：请到{task.get('next_location', '')}"
        return "任务已完成"

    def parse_voice_command(self, text: str) -> Dict:
        """解析语音指令"""
        import re
        text = text.strip()
        # 确认指令
        if re.match(r'确认|完成|ok|对的|是的', text, re.I):
            return {"intent": "confirm"}
        # 数量确认
        m = re.match(r'(\d+)\s*件', text)
        if m:
            return {"intent": "quantity", "qty": int(m.group(1))}
        # 库位复述
        m = re.match(r'([A-Z]\d{3,})', text.upper())
        if m:
            return {"intent": "location_confirm", "location": m.group(1)}
        # 异常上报
        if re.match(r'无货|没有|找不到|缺货', text):
            return {"intent": "no_stock"}
        if re.match(r'破损|坏了|质量问题', text):
            return {"intent": "damaged"}
        if re.match(r'数量不对|多了|少了', text):
            return {"intent": "qty_mismatch"}
        # 查询
        if re.match(r'还有多少|下一个|在哪', text):
            return {"intent": "query", "raw": text}
        return {"intent": "unknown", "raw": text}

    async def execute_command(self, picker_id: str, command: Dict) -> str:
        """执行语音指令，返回语音回复"""
        task = self.get_current_task(picker_id)
        if not task:
            return "当前没有拣货任务"

        intent = command.get("intent")
        if intent == "confirm":
            # 确认当前步骤
            step = task.get("step", "location")
            if step == "location":
                task["step"] = "product"
                redis_client.set_json(f"voice:task:{picker_id}", task, expire=3600)
                return await self.generate_voice_prompt(task, "product")
            elif step == "product":
                task["status"] = "completed"
                redis_client.set_json(f"voice:task:{picker_id}", task, expire=3600)
                # 发送完成事件
                from common.data_client import kafka_client
                kafka_client.send("pick-complete", task)
                return "拣货完成，请前往复核区"
        elif intent == "quantity":
            qty = command.get("qty", 0)
            if qty == task.get("qty"):
                return f"数量正确，{qty}件确认"
            else:
                return f"数量不对，应为{task.get('qty')}件，请重新确认"
        elif intent == "location_confirm":
            if command.get("location") == task.get("location"):
                task["step"] = "product"
                redis_client.set_json(f"voice:task:{picker_id}", task, expire=3600)
                return await self.generate_voice_prompt(task, "product")
            else:
                return f"库位不对，应为{task.get('location')}，请确认位置"
        elif intent in ["no_stock", "damaged", "qty_mismatch"]:
            # 记录异常，通知主管
            task["exception"] = intent
            from common.data_client import kafka_client
            kafka_client.send("pick-exception", {"picker_id": picker_id, "task": task, "exception": intent})
            return f"已记录{intent}异常，主管将尽快处理，请继续下一个任务"
        elif intent == "query":
            # 从 Redis 获取真实任务进度（PRD 5.3）
            progress = self.get_task_progress(picker_id)
            done = progress.get("done", 0)
            remaining = progress.get("remaining", 0)
            return f"当前任务进度：已完成{done}个，还剩{remaining}个"
        return "未识别指令，请重复"


voice_service = VoicePickingService()


@app.websocket("/ws/voice/{picker_id}")
async def voice_websocket(websocket: WebSocket, picker_id: str):
    """语音拣货WebSocket：接收音频→ASR→指令解析→TTS回复"""
    await websocket.accept()
    logger.info(f"拣货员{picker_id}语音连接建立")
    try:
        while True:
            # 接收音频数据
            audio_data = await websocket.receive_bytes()
            # ASR识别
            text = await ai_client.asr_recognize(audio_data)
            logger.info(f"拣货员{picker_id}语音: {text}")
            # 解析指令
            command = voice_service.parse_voice_command(text)
            # 执行指令
            reply_text = await voice_service.execute_command(picker_id, command)
            # TTS合成
            reply_audio = await ai_client.tts_synthesize(reply_text)
            # 发送回复音频
            await websocket.send_bytes(reply_audio)
    except WebSocketDisconnect:
        logger.info(f"拣货员{picker_id}语音连接断开")


@app.post("/voice/task/assign")
async def assign_task(picker_id: str, task: Dict):
    """分配拣货任务"""
    voice_service.assign_task(picker_id, task)
    return Result.success({"picker_id": picker_id, "task": task})


@app.get("/voice/task/{picker_id}")
async def get_task(picker_id: str):
    """获取当前任务"""
    task = voice_service.get_current_task(picker_id)
    if not task:
        raise HTTPException(status_code=404, detail="无当前任务")
    return Result.success(task)


@app.post("/voice/test-tts")
async def test_tts(text: str):
    """测试TTS"""
    audio = await ai_client.tts_synthesize(text)
    from fastapi.responses import Response
    return Response(content=audio, media_type="audio/wav")


@app.get("/health")
async def health():
    return Result.success({
        "status": "ok",
        "module": "voice",
        "version": "2.0.0",
        "mysql": "mock" if data_client.is_mock else "connected",
    })


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8106)
