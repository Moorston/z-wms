"""
WMS AI Platform - LLM 调用客户端单元测试（TDD）

测试缝隙（seams）：
  - common.ai_client.AIClient.llm_chat(prompt, ...) — LLM 调用公共方法

验证行为（不测实现细节）：
  1. 缓存命中时直接返回缓存答案，不调用真实 LLM
  2. 缓存命中时记录 LLM 调用日志（cache_hit 路径）
  3. 真实调用成功后返回答案
  4. 真实调用成功后回写缓存
  5. 真实调用成功后记录 LLM 调用日志（真实调用路径）
  6. 真实调用失败时抛异常
  7. 真实调用失败时记录 LLM 调用日志（error 路径）
  8. temperature 非 0 时不查缓存（直接走真实调用）
"""
import asyncio
import os
import sys
import json
from unittest.mock import MagicMock, patch, AsyncMock
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


# ========== 辅助：构造 mock httpx response ==========

def make_response(json_data=None, status_code=200, content=b""):
    """构造 httpx.Response mock"""
    resp = MagicMock()
    resp.status_code = status_code
    resp.json.return_value = json_data or {}
    resp.content = content or json.dumps(json_data or {}).encode()
    resp.headers = {"content-type": "application/json"}
    if status_code >= 400:
        resp.raise_for_status.side_effect = Exception(f"HTTP {status_code}")
    else:
        resp.raise_for_status.return_value = None
    return resp


def _make_mock_http(post_return_value=None):
    """创建 mock httpx 客户端，post 返回指定 response"""
    mock_http = MagicMock()
    if post_return_value is not None:
        mock_http.post = AsyncMock(return_value=post_return_value)
    else:
        mock_http.post = AsyncMock(return_value=make_response())
    return mock_http


def _import_and_build_client(fake_cache_layer, mock_http, disable_retry=False):
    """导入 AIClient、创建实例、注入 mock http 客户端"""
    with patch.dict("sys.modules", {
        "common.cache_layer": SimpleNamespace(llm_cache_layer=fake_cache_layer),
    }):
        from common.ai_client import AIClient
        client = AIClient()
        client._http = mock_http
        if disable_retry:
            # 提取 tenacity 包装的底层函数，跳过重试
            # 注意：__wrapped__ 是 unbound function，需用 partial 绑定 self
            if hasattr(AIClient.llm_chat, "__wrapped__"):
                from functools import partial
                client.llm_chat = partial(AIClient.llm_chat.__wrapped__, client)
        return client


@pytest.fixture
def event_loop():
    loop = asyncio.new_event_loop()
    yield loop
    loop.close()


# ========== 1. 缓存命中路径 ==========

class TestLlmChatCacheHit:

    def test_cache_hit_returns_cached_answer_without_llm_call(self):
        """缓存命中时直接返回缓存答案，不调用真实 LLM"""
        cached_result = {"answer": "缓存的答案", "source": "L1", "key": "hash123"}
        fake_cache_layer = MagicMock()
        fake_cache_layer.get = AsyncMock(return_value=cached_result)
        mock_http = _make_mock_http()

        # 保持 patch 上下文活跃，确保 llm_chat 调用期间读取的是 mock cache_layer
        with patch.dict("sys.modules", {
            "common.cache_layer": SimpleNamespace(llm_cache_layer=fake_cache_layer),
        }):
            client = _import_and_build_client(fake_cache_layer, mock_http)
            result = asyncio.run(client.llm_chat("测试问题", temperature=0.0))

        assert result == "缓存的答案"
        # 确认没有调用真实 LLM
        mock_http.post.assert_not_called()

    def test_cache_hit_records_log(self):
        """缓存命中时记录 LLM 调用日志"""
        cached_result = {"answer": "缓存的答案", "source": "L2", "key": "hash456"}
        fake_cache_layer = MagicMock()
        fake_cache_layer.get = AsyncMock(return_value=cached_result)
        mock_http = _make_mock_http()

        fake_logger = MagicMock()
        with patch.dict("sys.modules", {
            "common.cache_layer": SimpleNamespace(llm_cache_layer=fake_cache_layer),
        }), \
        patch("common.tracing.llm_call_logger", fake_logger):
            client = _import_and_build_client(fake_cache_layer, mock_http)
            result = asyncio.run(client.llm_chat("测试问题", temperature=0.0))

        assert result == "缓存的答案"
        # 验证 log_call 被调用，cache_hit 为 "L2"
        fake_logger.log_call.assert_called_once()
        call_args = fake_logger.log_call.call_args
        assert call_args.kwargs["cache_hit"] == "L2"
        assert call_args.kwargs["status"] == "success"


# ========== 2. 真实调用成功路径 ==========

class TestLlmChatRealCall:

    def test_real_call_returns_answer(self):
        """真实调用成功后返回答案"""
        fake_cache_layer = MagicMock()
        fake_cache_layer.get = AsyncMock(return_value=None)
        fake_cache_layer.put = AsyncMock()
        mock_http = _make_mock_http(make_response({"content": "LLM的真实答案"}))

        with patch.dict("sys.modules", {
            "common.cache_layer": SimpleNamespace(llm_cache_layer=fake_cache_layer),
        }):
            client = _import_and_build_client(fake_cache_layer, mock_http)
            result = asyncio.run(client.llm_chat("测试问题", temperature=0.0))

        assert result == "LLM的真实答案"

    def test_real_call_puts_to_cache(self):
        """真实调用成功后回写缓存"""
        fake_cache_layer = MagicMock()
        fake_cache_layer.get = AsyncMock(return_value=None)
        fake_cache_layer.put = AsyncMock()
        mock_http = _make_mock_http(make_response({"content": "LLM的真实答案"}))

        with patch.dict("sys.modules", {
            "common.cache_layer": SimpleNamespace(llm_cache_layer=fake_cache_layer),
        }):
            client = _import_and_build_client(fake_cache_layer, mock_http)
            asyncio.run(client.llm_chat("测试问题", temperature=0.0))

        fake_cache_layer.put.assert_called_once()

    def test_real_call_records_log_success(self):
        """真实调用成功后记录 LLM 调用日志（成功路径）"""
        fake_cache_layer = MagicMock()
        fake_cache_layer.get = AsyncMock(return_value=None)
        fake_cache_layer.put = AsyncMock()
        mock_http = _make_mock_http(make_response({"content": "LLM的真实答案"}))

        fake_logger = MagicMock()
        with patch.dict("sys.modules", {
            "common.cache_layer": SimpleNamespace(llm_cache_layer=fake_cache_layer),
        }), \
        patch("common.tracing.llm_call_logger", fake_logger):
            client = _import_and_build_client(fake_cache_layer, mock_http)
            asyncio.run(client.llm_chat("测试问题", temperature=0.0))

        # 真实调用成功应记录一次日志（status=success, cache_hit=none）
        fake_logger.log_call.assert_called_once()
        call_args = fake_logger.log_call.call_args
        assert call_args.kwargs["status"] == "success"
        assert call_args.kwargs["cache_hit"] == "none"


# ========== 3. 真实调用失败路径 ==========

class TestLlmChatFailure:

    def test_real_call_failure_raises_exception(self):
        """真实调用失败时抛异常"""
        fake_cache_layer = MagicMock()
        fake_cache_layer.get = AsyncMock(return_value=None)
        mock_http = _make_mock_http(make_response(status_code=500))

        with patch.dict("sys.modules", {
            "common.cache_layer": SimpleNamespace(llm_cache_layer=fake_cache_layer),
        }):
            client = _import_and_build_client(fake_cache_layer, mock_http, disable_retry=True)
            with pytest.raises(Exception) as exc_info:
                asyncio.run(client.llm_chat("测试问题", temperature=0.0))
        assert "500" in str(exc_info.value) or "HTTP" in str(exc_info.value)

    def test_real_call_failure_records_log_error(self):
        """真实调用失败时记录 LLM 调用日志（error 路径）"""
        fake_cache_layer = MagicMock()
        fake_cache_layer.get = AsyncMock(return_value=None)
        mock_http = _make_mock_http(make_response(status_code=500))

        fake_logger = MagicMock()
        with patch.dict("sys.modules", {
            "common.cache_layer": SimpleNamespace(llm_cache_layer=fake_cache_layer),
        }), \
        patch("common.tracing.llm_call_logger", fake_logger):
            client = _import_and_build_client(fake_cache_layer, mock_http, disable_retry=True)
            with pytest.raises(Exception):
                asyncio.run(client.llm_chat("测试问题", temperature=0.0))

        # 无重试时 log_call 应被调用 1 次（status=error）
        fake_logger.log_call.assert_called_once()
        call_args = fake_logger.log_call.call_args
        assert call_args.kwargs["status"] == "error"


# ========== 4. 非确定性温度不查缓存 ==========

class TestLlmChatNonDeterministic:

    def test_temperature_1_skips_cache_lookup(self):
        """temperature=1（非确定性）时不查缓存，直接走真实调用"""
        fake_cache_layer = MagicMock()
        fake_cache_layer.get = AsyncMock(return_value=None)
        fake_cache_layer.put = AsyncMock()
        mock_http = _make_mock_http(make_response({"content": "非确定性答案"}))

        with patch.dict("sys.modules", {
            "common.cache_layer": SimpleNamespace(llm_cache_layer=fake_cache_layer),
        }):
            client = _import_and_build_client(fake_cache_layer, mock_http)
            result = asyncio.run(client.llm_chat("测试问题", temperature=1.0))

        assert result == "非确定性答案"
        # 不应调用 cache.get（temperature=1 时不查缓存）
        fake_cache_layer.get.assert_not_called()
        # 不应回写缓存
        fake_cache_layer.put.assert_not_called()
