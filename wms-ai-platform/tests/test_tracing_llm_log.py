"""
WMS AI Platform - 链路追踪 + LLM 调用日志单元测试（TDD）

测试缝隙（seams）：
  - common.tracing.get_trace_id(request) — TraceId 3 级优先级
  - common.tracing.LLMCallLogger.log_call() — LLM 调用日志记录
  - common.tracing.LLMCallLogger._get_client() — ClickHouse 客户端懒加载

验证行为（不测实现细节）：
  1. get_trace_id: 请求头 X-Trace-Id 优先返回
  2. get_trace_id: 无请求头时生成新 TraceId
  3. get_trace_id: request=None 时生成新 TraceId
  4. get_trace_id: 返回的 TraceId 是 16 位十六进制字符串
  5. log_call: ClickHouse 不可用时降级（不抛异常）
  6. log_call: ClickHouse 可用时写入 SQL
  7. log_call: 自动估算 token 数（1 token ≈ 4 字符）
  8. log_call: 自动估算成本（$0.002/1K tokens）
  9. log_call: 提供 token 数时不重复估算
  10. _get_client: 懒加载，首次调用创建客户端
  11. _get_client: 后续调用返回同一客户端
"""
import os
import sys
import time
from unittest.mock import MagicMock, patch
from types import SimpleNamespace

import pytest

# 确保项目根目录在 sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# ========== 预注入 Mock，阻止 common/__init__.py 触发真实网络连接 ==========

class _FakeRedisModule:
    class Redis:
        def __init__(self, **kwargs):
            self._store = {}
        def get(self, key): return self._store.get(key)
        def set(self, key, value, ex=None): self._store[key] = value
        def delete(self, key): self._store.pop(key, None)
        def exists(self, key): return 1 if key in self._store else 0
        def incr(self, key, amount=1):
            v = int(self._store.get(key, 0)) + amount
            self._store[key] = v
            return v
        def zrevrange(self, key, start, end, withscores=False): return []
        def zincrby(self, key, amount, value): pass
        def zremrangebyrank(self, key, start, end): pass

import types as _types
_fake_redis_mod = _types.ModuleType("redis")
_fake_redis_mod.Redis = _FakeRedisModule.Redis
sys.modules["redis"] = _fake_redis_mod

_fake_kafka_mod = _types.ModuleType("kafka")
_fake_kafka_mod.KafkaProducer = MagicMock()
_fake_kafka_mod.KafkaConsumer = MagicMock()
_fake_kafka_errors_mod = _types.ModuleType("kafka.errors")
_fake_kafka_errors_mod.KafkaError = Exception
sys.modules["kafka"] = _fake_kafka_mod
sys.modules["kafka.errors"] = _fake_kafka_errors_mod

_fake_minio_mod = _types.ModuleType("minio")
_fake_minio_mod.Minio = MagicMock()
_fake_minio_errors_mod = _types.ModuleType("minio.error")
_fake_minio_errors_mod.S3Error = Exception
sys.modules["minio"] = _fake_minio_mod
sys.modules["minio.error"] = _fake_minio_errors_mod

_fake_pymilvus = _types.ModuleType("pymilvus")
_fake_pymilvus.MilvusClient = MagicMock
_fake_pymilvus.DataType = MagicMock()
sys.modules["pymilvus"] = _fake_pymilvus

_fake_ch = _types.ModuleType("clickhouse_driver")
_fake_ch.Client = MagicMock()
sys.modules["clickhouse_driver"] = _fake_ch

_fake_pymysql = _types.ModuleType("pymysql")
_fake_pymysql.connect = MagicMock()
sys.modules["pymysql"] = _fake_pymysql

_fake_aiomysql = _types.ModuleType("aiomysql")
_fake_aiomysql.connect = MagicMock()
sys.modules["aiomysql"] = _fake_aiomysql


# ========== 辅助：构造 Request mock ==========

def make_request(headers=None):
    """构造 FastAPI Request 的 mock 对象"""
    headers = headers or {}
    return SimpleNamespace(headers=headers)


# ========== 1. get_trace_id 测试 ==========

class TestGetTraceId:

    def test_header_x_trace_id_takes_priority(self):
        """请求头 X-Trace-Id 优先返回"""
        from common.tracing import get_trace_id
        req = make_request({"X-Trace-Id": "abc123def456"})
        result = get_trace_id(req)
        assert result == "abc123def456"

    def test_no_header_generates_new_trace_id(self):
        """无请求头时生成新 TraceId"""
        from common.tracing import get_trace_id
        req = make_request({})
        result = get_trace_id(req)
        # gen_trace_id() 返回 uuid4().hex[:16]，16 位十六进制
        assert len(result) == 16
        # 验证是十六进制字符串
        int(result, 16)  # 不抛异常即通过

    def test_request_none_generates_new_trace_id(self):
        """request=None 时生成新 TraceId"""
        from common.tracing import get_trace_id
        result = get_trace_id(None)
        assert len(result) == 16
        int(result, 16)

    def test_generated_trace_ids_are_unique(self):
        """两次调用生成不同的 TraceId"""
        from common.tracing import get_trace_id
        r1 = get_trace_id(None)
        r2 = get_trace_id(None)
        assert r1 != r2

    def test_empty_header_value_generates_new_id(self):
        """请求头 X-Trace-Id 为空字符串时，falsy 值跳过，自动生成新 TraceId"""
        from common.tracing import get_trace_id
        req = make_request({"X-Trace-Id": ""})
        result = get_trace_id(req)
        # 空字符串是 falsy，走 gen_trace_id() 分支
        assert len(result) == 16
        int(result, 16)  # 不抛异常即通过


# ========== 2. LLMCallLogger 测试 ==========

class TestLLMCallLogger:

    def test_log_call_degrades_when_clickhouse_unavailable(self):
        """ClickHouse 不可用时降级（不抛异常）"""
        from common.tracing import LLMCallLogger
        logger = LLMCallLogger()
        # 强制 _get_client 返回 None（模拟 ClickHouse 不可用）
        logger._get_client = MagicMock(return_value=None)

        # 不应抛异常
        logger.log_call(
            task_type="rag",
            model="deepseek-v4-pro",
            prompt="测试问题",
            answer="测试答案",
            latency_ms=100,
            cache_hit="none",
            status="success",
        )

    def test_log_call_writes_sql_when_clickhouse_available(self):
        """ClickHouse 可用时写入参数化 SQL"""
        from common.tracing import LLMCallLogger
        logger = LLMCallLogger()

        fake_client = MagicMock()
        logger._get_client = MagicMock(return_value=fake_client)

        logger.log_call(
            task_type="rag",
            model="deepseek-v4-pro",
            prompt="测试问题",
            answer="测试答案",
            latency_ms=100,
            cache_hit="none",
            status="success",
        )

        fake_client.query_olap.assert_called_once()
        # 验证 SQL 包含表名和 @param 占位符
        sql = fake_client.query_olap.call_args[0][0]
        assert "llm_call_log" in sql
        assert "@p_model" in sql  # 参数化占位符
        # 验证 params 字典包含正确的值
        params = fake_client.query_olap.call_args[1]["params"]
        assert params["p_model"] == "deepseek-v4-pro"
        assert params["p_task_type"] == "rag"

    def test_log_call_auto_estimates_tokens(self):
        """未提供 token 数时自动估算（1 token ≈ 4 字符）"""
        from common.tracing import LLMCallLogger
        logger = LLMCallLogger()

        fake_client = MagicMock()
        logger._get_client = MagicMock(return_value=fake_client)

        # prompt 长度 40 字符 → 10 tokens
        prompt = "a" * 40
        answer = "b" * 40
        logger.log_call(
            task_type="rag",
            model="deepseek-v4-pro",
            prompt=prompt,
            answer=answer,
            latency_ms=100,
            cache_hit="none",
            status="success",
            # 不提供 token 参数，触发自动估算
        )

        params = fake_client.query_olap.call_args[1]["params"]
        assert params["p_prompt_tokens"] == 10       # 40//4
        assert params["p_completion_tokens"] == 10   # 40//4
        assert params["p_total_tokens"] == 20
        assert params["p_cost"] == round(20 / 1000 * 0.002, 6)  # 4e-05

    def test_log_call_auto_estimates_cost(self):
        """未提供成本时自动估算（$0.002/1K tokens）"""
        from common.tracing import LLMCallLogger
        logger = LLMCallLogger()

        fake_client = MagicMock()
        logger._get_client = MagicMock(return_value=fake_client)

        # 100 tokens total → cost = 100/1000 * 0.002 = 0.0002
        logger.log_call(
            task_type="rag",
            model="deepseek-v4-pro",
            prompt="a" * 400,  # 100 tokens
            answer="b" * 0,     # 0 tokens
            latency_ms=100,
            cache_hit="none",
            status="success",
            prompt_tokens=100,
            completion_tokens=0,
            total_tokens=100,
            # 不提供 cost 参数，触发自动估算
        )

        params = fake_client.query_olap.call_args[1]["params"]
        # cost = round(100/1000 * 0.002, 6) = 0.0002
        assert params["p_cost"] == 0.0002

    def test_log_call_uses_provided_tokens(self):
        """提供 token 数时不重复估算"""
        from common.tracing import LLMCallLogger
        logger = LLMCallLogger()

        fake_client = MagicMock()
        logger._get_client = MagicMock(return_value=fake_client)

        # 明确提供 token 数，prompt 很长但 token 数很小
        logger.log_call(
            task_type="rag",
            model="deepseek-v4-pro",
            prompt="x" * 1000,  # 250 tokens if auto-estimated
            answer="y" * 1000,
            latency_ms=100,
            cache_hit="none",
            status="success",
            prompt_tokens=5,      # 明确指定
            completion_tokens=3,
            total_tokens=8,       # 明确指定
        )

        params = fake_client.query_olap.call_args[1]["params"]
        # 应该使用提供的值 5, 3, 8 而非估算的 250, 250, 500
        assert params["p_prompt_tokens"] == 5
        assert params["p_completion_tokens"] == 3
        assert params["p_total_tokens"] == 8

    def test_log_call_records_error_status(self):
        """错误状态时记录 error_msg"""
        from common.tracing import LLMCallLogger
        logger = LLMCallLogger()

        fake_client = MagicMock()
        logger._get_client = MagicMock(return_value=fake_client)

        logger.log_call(
            task_type="rag",
            model="deepseek-v4-pro",
            prompt="测试问题",
            answer="",
            latency_ms=5000,
            cache_hit="none",
            status="error",
            error_msg="HTTP 500 Internal Server Error",
        )

        params = fake_client.query_olap.call_args[1]["params"]
        assert "error" in params["p_status"]
        assert "HTTP 500" in params["p_error_msg"]

    def test_log_call_records_cache_hit(self):
        """缓存命中时记录 cache_hit 来源"""
        from common.tracing import LLMCallLogger
        logger = LLMCallLogger()

        fake_client = MagicMock()
        logger._get_client = MagicMock(return_value=fake_client)

        logger.log_call(
            task_type="rag",
            model="deepseek-v4-pro",
            prompt="测试问题",
            answer="缓存答案",
            latency_ms=0,
            cache_hit="L1",
            status="success",
        )

        params = fake_client.query_olap.call_args[1]["params"]
        assert params["p_cache_hit"] == "L1"

    def test_log_call_records_user_id(self):
        """提供 user_id 时记录到 SQL"""
        from common.tracing import LLMCallLogger
        logger = LLMCallLogger()

        fake_client = MagicMock()
        logger._get_client = MagicMock(return_value=fake_client)

        logger.log_call(
            task_type="rag",
            model="deepseek-v4-pro",
            prompt="测试问题",
            answer="答案",
            latency_ms=100,
            cache_hit="none",
            status="success",
            user_id="user123",
        )

        params = fake_client.query_olap.call_args[1]["params"]
        assert params["p_user_id"] == "user123"


# ========== 3. _get_client 懒加载测试 ==========

class TestGetClientLazyLoad:

    def test_get_client_returns_none_when_data_client_mock(self):
        """data_client 为 mock 模式时 _get_client 返回 None"""
        from common.tracing import LLMCallLogger
        logger = LLMCallLogger()

        # mock common.data_client 模块为 mock 模式（_get_client 内部 from common.data_client import data_client）
        mock_dc_module = MagicMock()
        mock_dc_module.data_client.is_mock = True

        with patch.dict("sys.modules", {"common.data_client": mock_dc_module}):
            result = logger._get_client()
        assert result is None

    def test_get_client_returns_none_when_import_fails(self):
        """data_client 导入失败时 _get_client 返回 None"""
        from common.tracing import LLMCallLogger
        logger = LLMCallLogger()

        # 让 import 失败
        with patch.dict("sys.modules", {"common.data_client": None}):
            result = logger._get_client()
        assert result is None

    def test_get_client_caches_after_first_call(self):
        """首次调用后缓存客户端，后续调用不再创建"""
        from common.tracing import LLMCallLogger
        logger = LLMCallLogger()

        fake_client = MagicMock()
        logger._ch_client = fake_client  # 预置缓存

        result = logger._get_client()
        assert result is fake_client
