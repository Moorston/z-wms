"""
WMS AI Platform - 调度服务
统一任务调度：定时任务(APScheduler) / 事件驱动(Kafka消费者) / 流式处理(Flink)
"""
import os
import sys
import json
import asyncio
from typing import Dict, List, Callable
from datetime import datetime
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from loguru import logger
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.cron import CronTrigger

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from common.config import settings
from common.data_client import kafka_client
from common.utils import Result, now_str
from common.cache_metrics import setup_metrics
from common.tracing import init_tracing, instrument_app

app = FastAPI(title="WMS AI Scheduler", version="2.0.0")
setup_metrics(app)
init_tracing("scheduler")
instrument_app(app, "scheduler")

# 调度器
scheduler = AsyncIOScheduler()

# 任务注册表
_task_registry: Dict[str, Dict] = {}


class TaskInfo(BaseModel):
    task_id: str
    name: str
    type: str  # cron/event/stream
    cron: str = None  # cron表达式
    topic: str = None  # Kafka topic
    handler: str = None  # 处理函数路径
    enabled: bool = True
    last_run: str = None
    next_run: str = None


def register_cron_task(task_id: str, name: str, cron: str, func: Callable):
    """注册定时任务"""
    _task_registry[task_id] = {
        "task_id": task_id, "name": name, "type": "cron",
        "cron": cron, "func": func, "enabled": True,
    }
    scheduler.add_job(func, CronTrigger.from_crontab(cron), id=task_id, replace_existing=True)
    logger.info(f"注册定时任务: {task_id} ({cron})")


def register_event_task(task_id: str, name: str, topic: str, handler: Callable):
    """注册事件驱动任务"""
    _task_registry[task_id] = {
        "task_id": task_id, "name": name, "type": "event",
        "topic": topic, "handler": handler, "enabled": True,
    }
    # 启动Kafka消费者
    asyncio.create_task(_kafka_consumer(task_id, topic, handler))
    logger.info(f"注册事件任务: {task_id} (topic={topic})")


async def _kafka_consumer(task_id: str, topic: str, handler: Callable):
    """Kafka消费者循环"""
    try:
        consumer = kafka_client.get_consumer(topic, group_id=f"scheduler-{task_id}")
        for message in consumer:
            try:
                await handler(message.value)
                consumer.commit()
            except Exception as e:
                logger.error(f"任务{task_id}处理失败: {e}")
    except Exception as e:
        logger.error(f"Kafka消费者{task_id}异常: {e}")


# ========== 内置定时任务 ==========
async def daily_forecast_job():
    """每日预测任务（凌晨2点）"""
    logger.info("执行每日预测任务")
    try:
        import httpx
        async with httpx.AsyncClient(timeout=300) as client:
            await client.post("http://localhost:8102/forecast/daily-job")
    except Exception as e:
        logger.error(f"每日预测任务失败: {e}")


async def daily_report_job():
    """每日报表任务（早上8点）"""
    logger.info("执行每日报表任务")
    try:
        import httpx
        async with httpx.AsyncClient(timeout=120) as client:
            await client.post("http://localhost:8103/report/generate", json={
                "report_type": "daily", "push_channels": ["wechat"]
            })
    except Exception as e:
        logger.error(f"每日报表任务失败: {e}")


async def hourly_aiops_check():
    """每小时AIOps巡检"""
    logger.info("执行AIOps巡检")


async def handle_ocr_batch(msg):
    """
    ocr-batch Kafka 消费者处理函数
    调用 OCR 模块异步处理批量单据
    消息格式（PRD 8.2 对齐）：{task_id, object_name, doc_type, po_no?, order_no?}

    PRD V2.0 修复：MinIO 下载改为直接通过 minio_client 访问（不再调用不存在的 HTTP 端点）
    """
    task_id = msg.get("task_id", "unknown")
    object_name = msg.get("object_name")
    if not object_name:
        logger.warning(f"ocr-batch消息缺少object_name: task_id={task_id}")
        return
    try:
        # 直接通过 MinIO 客户端下载（不依赖 data_service HTTP 端点）
        from common.data_client import minio_client
        image_bytes = minio_client.download_bytes(object_name)

        import httpx
        async with httpx.AsyncClient(timeout=120) as client:
            # 调用 OCR 服务上传识别
            files = {"image": ("batch.jpg", image_bytes, "image/jpeg")}
            data = {
                "doc_type": msg.get("doc_type", ""),
                "po_no": msg.get("po_no", ""),
                "order_no": msg.get("order_no", ""),
            }
            await client.post("http://localhost:8101/ocr/upload", files=files, data=data)
            logger.info(f"ocr-batch处理完成: task_id={task_id}")
    except Exception as e:
        logger.error(f"ocr-batch消费处理失败: task_id={task_id}, error={e}")


# ========== API接口 ==========
@app.on_event("startup")
async def startup():
    """启动时注册任务"""
    scheduler.start()
    # 注册内置定时任务
    register_cron_task("daily_forecast", "每日需求预测", "0 2 * * *", daily_forecast_job)
    register_cron_task("daily_report", "每日运营报表", "0 8 * * *", daily_report_job)
    register_cron_task("hourly_aiops", "每小时AIOps巡检", "0 * * * *", hourly_aiops_check)
    # 注册 Kafka 事件驱动任务（PRD 8.2 Topic: ocr-batch）
    register_event_task("ocr_batch_consumer", "OCR批量单据消费", "ocr-batch", handle_ocr_batch)
    logger.info("调度服务启动，已注册任务")


@app.on_event("shutdown")
async def shutdown():
    scheduler.shutdown()


@app.get("/scheduler/tasks")
async def list_tasks():
    """列出所有任务"""
    tasks = []
    for task_id, info in _task_registry.items():
        job = scheduler.get_job(task_id)
        tasks.append({
            "task_id": task_id,
            "name": info["name"],
            "type": info["type"],
            "cron": info.get("cron"),
            "topic": info.get("topic"),
            "enabled": info["enabled"],
            "next_run": str(job.next_run_time) if job and info["type"] == "cron" else None,
        })
    return Result.success(tasks)


@app.post("/scheduler/task/{task_id}/run")
async def run_task(task_id: str):
    """手动触发任务"""
    if task_id not in _task_registry:
        raise HTTPException(status_code=404, detail="任务不存在")
    info = _task_registry[task_id]
    if info["type"] == "cron":
        job = scheduler.get_job(task_id)
        if job:
            job.modify(next_run_time=datetime.now())
    return Result.success({"task_id": task_id, "status": "triggered"})


@app.post("/scheduler/task/{task_id}/toggle")
async def toggle_task(task_id: str, enabled: bool):
    """启用/禁用任务"""
    if task_id not in _task_registry:
        raise HTTPException(status_code=404, detail="任务不存在")
    info = _task_registry[task_id]
    info["enabled"] = enabled
    if info["type"] == "cron":
        job = scheduler.get_job(task_id)
        if job:
            job.pause() if not enabled else job.resume()
    return Result.success({"task_id": task_id, "enabled": enabled})


@app.get("/health")
async def health():
    return Result.success({"status": "ok", "module": "scheduler", "tasks": len(_task_registry)})


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8003)
