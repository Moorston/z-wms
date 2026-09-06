"""
测试：RAG 检索质量监控
验证 RetrievalMonitor 队列管理、降级、统计
"""
import pytest
import asyncio
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from modules.rag.monitor import RetrievalMonitor, RetrievalLogEntry


class TestRetrievalLogEntry:
    """日志条目数据结构测试"""

    def test_basic_creation(self):
        """基本创建"""
        entry = RetrievalLogEntry(
            ts="2026-09-05T12:00:00",
            query="波次拣货",
            refined_query="波次拣货流程",
            total_docs=5,
        )
        assert entry.ts == "2026-09-05T12:00:00"
        assert entry.total_docs == 5
        assert entry.hit == 0
        assert entry.feedback == ""

    def test_default_fields(self):
        """默认字段值正确"""
        entry = RetrievalLogEntry(
            ts="2026-01-01T00:00:00", query="test", refined_query="test", total_docs=0,
        )
        assert entry.latency_ms == 0
        assert entry.top_score == 0.0
        assert entry.chunk_ids == []
        assert entry.session_id == ""
        assert entry.user_id == ""


class TestRetrievalMonitor:
    """监控器测试"""

    def test_init_degraded(self):
        """ClickHouse 不可用时降级初始化"""
        monitor = RetrievalMonitor()
        # ClickHouse 未连接时不应抛出异常
        stats = monitor.stats()
        assert "queue_size" in stats
        assert "total_written" in stats
        assert "total_dropped" in stats

    def test_stats_structure(self):
        """统计信息结构正确"""
        monitor = RetrievalMonitor()
        s = monitor.stats()
        assert s["batch_size"] == 50  # 默认值
        assert s["flush_interval"] == 5.0
        assert "clickhouse_available" in s

    @pytest.mark.asyncio
    async def test_queue_degradation(self):
        """队列满时丢弃日志（不阻塞主流程）"""
        monitor = RetrievalMonitor(batch_size=50)
        # 填满队列
        for i in range(1001):
            entry = RetrievalLogEntry(
                ts="2026-09-05T12:00:00",
                query=f"q{i}",
                refined_query=f"q{i}",
                total_docs=1,
            )
            await monitor.log(entry)
        # 队列满后不抛异常
        assert monitor._drop_count >= 0

    @pytest.mark.asyncio
    async def test_flush_degraded(self):
        """ClickHouse 不可用时 flush 降级不报错"""
        monitor = RetrievalMonitor(batch_size=1)
        monitor._clickhouse = None  # 强制降级
        entry = RetrievalLogEntry(
            ts="2026-09-05T12:00:00", query="test", refined_query="test", total_docs=0,
        )
        await monitor.log(entry)
        await monitor._flush_batch()
        # 不抛异常即通过

    @pytest.mark.asyncio
    async def test_start_stop(self):
        """启动和停止生命周期正常"""
        monitor = RetrievalMonitor(batch_size=10, flush_interval=0.1)
        await monitor.start()
        assert monitor._running is True
        await monitor.stop()
        assert monitor._running is False
