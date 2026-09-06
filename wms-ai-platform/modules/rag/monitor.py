"""
RAG 检索质量监控 — ClickHouse 异步批量写入
asyncio.Queue 批量聚合 → 定时刷入 ClickHouse rag_retrieval_log 表
ClickHouse 不可用时降级为空操作（不丢数据，静默丢弃）
"""
import asyncio
import os
import sys
import time
from dataclasses import dataclass, field, asdict
from datetime import datetime
from typing import Optional, Dict, List

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from loguru import logger
from common.config import settings

# ClickHouse 表名
CH_TABLE = "rag_retrieval_log"


@dataclass
class RetrievalLogEntry:
    """单次检索日志条目"""
    ts: str                          # ISO 8601 时间戳
    query: str                       # 原始查询文本
    refined_query: str               # 改写后查询
    total_docs: int                  # 检索返回文档总数
    vector_docs: int = 0             # 向量召回数
    keyword_docs: int = 0            # 关键词召回数
    latency_ms: int = 0              # 检索耗时（毫秒）
    top_score: float = 0.0           # Top-1 得分
    hit: int = 0                     # 是否有命中（1=命中，0=未命中）
    chunk_ids: List[int] = field(default_factory=list)  # 返回 chunk 索引列表
    session_id: str = ""             # 会话 ID
    user_id: str = ""                # 用户 ID
    feedback: str = ""               # 反馈（空/like/dislike）


class RetrievalMonitor:
    """
    RAG 检索监控器

    - asyncio.Queue 异步批量写入 ClickHouse
    - batch_size / flush_interval 可配
    - ClickHouse 不可用时静默降级，不影响主流程
    """

    def __init__(self, batch_size: int = 50, flush_interval: float = 5.0):
        self._queue: asyncio.Queue = asyncio.Queue(maxsize=1000)
        self._batch_size = batch_size
        self._flush_interval = flush_interval
        self._running = False
        self._flush_task: Optional[asyncio.Task] = None
        self._clickhouse = None       # 懒加载 ClickHouse client
        self._drop_count = 0          # 降级丢弃计数
        self._write_count = 0         # 成功写入计数
        self._init_clickhouse()

    def _init_clickhouse(self):
        """懒加载 ClickHouse 客户端，失败则降级"""
        try:
            import clickhouse_connect
            self._clickhouse = clickhouse_connect.get_client(
                host=settings.clickhouse_host,
                port=settings.clickhouse_port,
                username=settings.clickhouse_user,
                password=settings.clickhouse_password,
                database=settings.clickhouse_database,
            )
            # 连接测试
            self._clickhouse.execute("SELECT 1")
            logger.debug("ClickHouse 监控客户端已连接")
        except Exception as e:
            logger.warning(f"ClickHouse 不可用，监控降级为 no-op: {e}")
            self._clickhouse = None

    async def start(self):
        """启动后台刷入任务"""
        if self._running:
            return
        self._running = True
        self._flush_task = asyncio.create_task(self._flush_loop())
        logger.info(f"RAG 监控启动 (batch_size={self._batch_size}, flush_interval={self._flush_interval}s)")

    async def stop(self):
        """停止并刷入剩余数据"""
        self._running = False
        if self._flush_task:
            try:
                self._flush_task.cancel()
            except asyncio.CancelledError:
                pass
            try:
                await self._flush_task
            except (asyncio.CancelledError, Exception):
                pass
        # 刷入剩余
        await self._flush_batch()
        logger.info(f"RAG 监控停止 (写入={self._write_count}, 丢弃={self._drop_count})")

    async def log(self, entry: RetrievalLogEntry):
        """记录一条检索日志（异步非阻塞）"""
        try:
            self._queue.put_nowait(entry)
        except asyncio.QueueFull:
            self._drop_count += 1
            logger.warning(f"监控队列满，丢弃日志 (累计丢弃={self._drop_count})")

    async def _flush_loop(self):
        """后台定时刷入任务"""
        try:
            while self._running:
                await asyncio.sleep(self._flush_interval)
                await self._flush_batch()
        except asyncio.CancelledError:
            pass
        except Exception as e:
            logger.error(f"监控刷入循环异常: {e}")

    async def _flush_batch(self):
        """从队列中取出最多 batch_size 条，批量写入 ClickHouse"""
        if self._clickhouse is None:
            # 降级：直接清空队列
            while not self._queue.empty():
                try:
                    self._queue.get_nowait()
                    self._drop_count += 1
                except asyncio.QueueEmpty:
                    break
            return

        batch = []
        while self._queue.qsize() >= self._batch_size:
            batch.append(self._queue.get_nowait())
        # 不足一个批次时，取队列中所有可用
        while batch and len(batch) < self._batch_size and not self._queue.empty():
            batch.append(self._queue.get_nowait())

        if not batch:
            return

        self._write_batch(batch)

    def _write_batch(self, batch: List[RetrievalLogEntry]):
        """批量写入 ClickHouse"""
        try:
            rows = [asdict(e) for e in batch]
            self._clickhouse.insert(
                table=CH_TABLE,
                rows=rows,
                column_names=list(rows[0].keys()),
            )
            self._write_count += len(batch)
            logger.debug(f"ClickHouse 写入 {len(batch)} 条检索日志")
        except Exception as e:
            self._drop_count += len(batch)
            logger.warning(f"ClickHouse 批量写入失败，丢弃 {len(batch)} 条: {e}")

    def stats(self) -> Dict:
        """返回监控统计"""
        return {
            "queue_size": self._queue.qsize(),
            "batch_size": self._batch_size,
            "flush_interval": self._flush_interval,
            "clickhouse_available": self._clickhouse is not None,
            "total_written": self._write_count,
            "total_dropped": self._drop_count,
        }


# ========== 全局实例 ==========
retrieval_monitor = RetrievalMonitor()
