"""
WMS AI Platform - 无人机拍照盘点模块
功能：自动航线规划/拍照/CV识别/库存比对/差异处理
"""
import os
import sys
import json
from typing import List, Dict, Optional
from fastapi import FastAPI, UploadFile, File, HTTPException, Form
from pydantic import BaseModel
from loguru import logger

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
from common.config import settings
from common.ai_client import ai_client
from common.data_client import redis_client, minio_client, kafka_client, data_client
from common.utils import Result, gen_id, now_str
from common.cache_metrics import setup_metrics
from common.tracing import init_tracing, instrument_app

app = FastAPI(title="WMS AI - 无人机盘点模块", version="2.0.0")
setup_metrics(app)
init_tracing("drone")
instrument_app(app, "drone")


class InventoryTask(BaseModel):
    task_id: Optional[str] = None
    warehouse: str = "WH001"
    area: str = "ALL"  # 盘点区域
    priority: str = "normal"
    scheduled_time: Optional[str] = None


class DroneService:
    """无人机盘点服务"""

    async def create_task(self, task: InventoryTask) -> Dict:
        """创建盘点任务"""
        task_id = gen_id()
        task.task_id = task_id
        task.status = "created"
        task.created_at = now_str()
        # 1. 规划航线
        flight_plan = await self._plan_flight_path(task)
        task.flight_plan = flight_plan
        # 2. 缓存任务
        redis_client.set_json(f"drone:task:{task_id}", task.model_dump(), expire=86400 * 7)
        return task.model_dump()

    async def _plan_flight_path(self, task: InventoryTask) -> Dict:
        """
        规划无人机航线（PRD 5.4）
        基于 WMS 库位坐标生成 Boustrophedon（牛耕式）覆盖航线
        库位坐标从 WMS MySQL location 表查询，失败降级默认网格
        """
        # 1. 从 WMS 查询目标区域的库位坐标
        locations = await self._get_location_coords(task.warehouse, task.area)

        # 2. 降级：默认网格坐标
        if not locations:
            locations = self._generate_default_grid(task.area)

        # 3. 按排分组，牛耕式排序（奇数排正向，偶数排反向）
        waypoints = []
        rows = {}
        for loc in locations:
            rows.setdefault(loc["row"], []).append(loc)
        for row_idx in sorted(rows.keys()):
            row_locs = sorted(rows[row_idx], key=lambda l: l["col"],
                              reverse=(row_idx % 2 == 1))
            for loc in row_locs:
                waypoints.append({
                    "x": loc["x"], "y": loc["y"], "z": 2.5,
                    "action": "photo",
                    "location": loc["code"],
                })

        return {
            "waypoints": waypoints,
            "estimated_time": len(waypoints) * 30,
            "total_locations": len(waypoints),
            "path_strategy": "boustrophedon",
        }

    async def _get_location_coords(self, warehouse: str, area: str) -> List[Dict]:
        """从 WMS 查询库位坐标"""
        try:
            area_filter = f"AND area = '{area}'" if area != "ALL" else ""
            rows = data_client.query_mysql(
                f"SELECT location_code AS code, row_num AS row, col_num AS col, "
                f"x_coord AS x, y_coord AS y "
                f"FROM wms_location WHERE warehouse = %s {area_filter} "
                f"ORDER BY row_num, col_num",
                params=[warehouse],
            )
            return rows if rows else []
        except Exception as e:
            logger.warning(f"WMS 库位坐标查询失败，降级默认网格: {e}")
            return []

    def _generate_default_grid(self, area: str) -> List[Dict]:
        """生成默认 3×4 网格坐标（降级用）"""
        locations = []
        for row in range(1, 4):
            for col in range(1, 5):
                locations.append({
                    "code": f"{area[:1] if area != 'ALL' else 'A'}{row:02d}{col:02d}",
                    "row": row, "col": col,
                    "x": col * 2.0, "y": row * 2.0,
                })
        return locations

    async def start_task(self, task_id: str) -> Dict:
        """启动盘点任务（无人机起飞）"""
        task = redis_client.get_json(f"drone:task:{task_id}")
        if not task:
            raise HTTPException(status_code=404, detail="任务不存在")
        task["status"] = "flying"
        task["start_time"] = now_str()
        redis_client.set_json(f"drone:task:{task_id}", task, expire=86400 * 7)
        # 发送指令给无人机（通过DJI SDK/MQTT）
        kafka_client.send("drone-command", {"task_id": task_id, "command": "takeoff", "flight_plan": task.get("flight_plan")})
        return {"task_id": task_id, "status": "flying"}

    async def process_image(self, task_id: str, location: str, image_bytes: bytes) -> Dict:
        """处理无人机拍摄的图片：识别库位+商品+数量"""
        # 1. 上传图片
        obj_name = f"drone/{task_id}/{location}_{gen_id()}.jpg"
        minio_client.upload_bytes(obj_name, image_bytes, "image/jpeg")
        # 2. CV识别：库位编号+商品+数量
        ocr_result = await ai_client.ocr_recognize(image_bytes)
        detections = await ai_client.object_detect(image_bytes)
        # 3. 解析识别结果
        recognized = self._parse_recognition(ocr_result, detections, location)
        # 4. 与系统库存比对
        diff = await self._compare_inventory(location, recognized)
        # 5. 记录结果
        result = {
            "task_id": task_id, "location": location,
            "recognized": recognized, "diff": diff,
            "image_url": obj_name, "created_at": now_str(),
        }
        redis_client.set_json(f"drone:result:{task_id}:{location}", result, expire=86400 * 7)
        return result

    def _parse_recognition(self, ocr_result: Dict, detections: List[Dict], location: str) -> Dict:
        """解析识别结果"""
        text = ocr_result.get("text", "")
        import re
        # 提取SKU
        skus = re.findall(r'SKU[A-Z0-9]+', text)
        # 提取批次号
        batches = re.findall(r'B\d{8}', text)
        # 商品数量（检测到的商品框数量）
        product_count = len([d for d in detections if d.get("class") == "box"])
        return {
            "location": location,
            "skus": list(set(skus)),
            "batches": list(set(batches)),
            "estimated_qty": product_count,
            "raw_text": text[:200],
        }

    async def _compare_inventory(self, location: str, recognized: Dict) -> Dict:
        """
        与系统库存比对（PRD 5.4）
        通过 data_client 查询 WMS MySQL 库存表，真实差异比对
        """
        # 1. 查询 WMS 系统库存
        system_inventory = []
        try:
            rows = data_client.query_mysql(
                "SELECT sku, qty, batch_no FROM wms_inventory "
                "WHERE location = %s AND qty > 0",
                params=[location],
            )
            system_inventory = rows if rows else []
        except Exception as e:
            logger.warning(f"WMS 库存查询失败: {e}")
            # 降级空列表（不做假比对）

        # 2. 差异比对
        recognized_skus = set(recognized.get("skus", []))
        diffs = []
        for sys_item in system_inventory:
            if sys_item["sku"] not in recognized_skus:
                # 系统有但无人机未识别 → 可能缺货
                diffs.append({
                    "type": "missing",
                    "sku": sys_item["sku"],
                    "system_qty": int(sys_item["qty"]),
                    "recognized_qty": 0,
                })
        # 无人机识别到但系统无记录 → 可能多货/串货
        for sku in recognized_skus:
            if not any(s["sku"] == sku for s in system_inventory):
                diffs.append({
                    "type": "unexpected",
                    "sku": sku,
                    "system_qty": 0,
                    "recognized_qty": recognized.get("estimated_qty", 0),
                })

        return {
            "has_diff": len(diffs) > 0,
            "diffs": diffs,
            "system_inventory": system_inventory,
        }

    async def complete_task(self, task_id: str) -> Dict:
        """完成盘点任务，生成报告"""
        task = redis_client.get_json(f"drone:task:{task_id}")
        if not task:
            raise HTTPException(status_code=404, detail="任务不存在")
        task["status"] = "completed"
        task["end_time"] = now_str()
        # 汇总差异：扫描 Redis 中该任务下所有库位结果
        summary = self._summarize_task(task_id)
        task["summary"] = summary
        redis_client.set_json(f"drone:task:{task_id}", task, expire=86400 * 7)
        return task

    def _summarize_task(self, task_id: str) -> Dict:
        """扫描 Redis 中该任务的所有盘点结果，汇总统计"""
        total_locations = 0
        diff_count = 0
        # 扫描 drone:result:{task_id}:* 键
        try:
            keys = redis_client.scan_iter(f"drone:result:{task_id}:*")
            for key in keys:
                result = redis_client.get_json(key)
                if result:
                    total_locations += 1
                    if result.get("diff", {}).get("has_diff"):
                        diff_count += 1
        except Exception as e:
            logger.warning(f"汇总盘点结果失败: {e}")

        accuracy = round((total_locations - diff_count) / total_locations, 4) if total_locations > 0 else 0.0
        return {
            "total_locations": total_locations,
            "diff_count": diff_count,
            "accuracy": accuracy,
        }

    def get_task(self, task_id: str) -> Dict:
        task = redis_client.get_json(f"drone:task:{task_id}")
        if not task:
            raise HTTPException(status_code=404, detail="任务不存在")
        return task


drone_service = DroneService()


@app.post("/drone/task")
async def create_task(task: InventoryTask):
    """创建盘点任务"""
    return Result.success(await drone_service.create_task(task))


@app.post("/drone/task/{task_id}/start")
async def start_task(task_id: str):
    """启动盘点任务"""
    return Result.success(await drone_service.start_task(task_id))


@app.post("/drone/task/{task_id}/image")
async def upload_image(task_id: str, location: str = Form(...), image: UploadFile = File(...)):
    """上传盘点图片并识别"""
    img_bytes = await image.read()
    return Result.success(await drone_service.process_image(task_id, location, img_bytes))


@app.post("/drone/task/{task_id}/complete")
async def complete_task(task_id: str):
    """完成盘点任务"""
    return Result.success(await drone_service.complete_task(task_id))


@app.get("/drone/task/{task_id}")
async def get_task(task_id: str):
    """获取盘点任务"""
    return Result.success(drone_service.get_task(task_id))


@app.get("/health")
async def health():
    return Result.success({
        "status": "ok",
        "module": "drone",
        "version": "2.0.0",
        "mysql": "mock" if data_client.is_mock else "connected",
    })


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8107)
