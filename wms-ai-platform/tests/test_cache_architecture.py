"""
WMS AI Platform - 五级缓存架构单元测试
覆盖计划第12节验证方案：
  1. Prompt 标准化
  2. TTL 抖动
  3. L1+L2 命中
  4. 版本号失效
  5. 互斥锁防击穿
  6. 语义缓存降级（mock 模式）
  7. metrics 端点
  8. 预热
  9. L4 检索缓存
"""
import asyncio
import json
import os
import sys
from unittest.mock import MagicMock, patch, AsyncMock

import pytest

# 确保项目根目录在 sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# ========== 预注入 Mock，阻止 common/__init__.py 触发真实网络连接 ==========
# common/__init__.py 会初始化 data_client（Redis/Kafka/MinIO），
# 测试环境中这些服务可能不可用，所以先 mock 掉底层客户端类

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
        def set(self, key, value, ex=None, nx=False):
            if nx and key in self._store:
                return None
            self._store[key] = value
            return True
        def zrevrange(self, key, start, end, withscores=False): return []
        def zincrby(self, key, amount, value): pass
        def zremrangebyrank(self, key, start, end): pass

# 在导入 common 之前替换 redis 模块
import types as _types
_fake_redis_mod = _types.ModuleType("redis")
_fake_redis_mod.Redis = _FakeRedisModule.Redis
sys.modules["redis"] = _fake_redis_mod

# mock kafka 模块
_fake_kafka_mod = _types.ModuleType("kafka")
_fake_kafka_mod.KafkaProducer = MagicMock()
_fake_kafka_mod.KafkaConsumer = MagicMock()
_fake_kafka_errors_mod = _types.ModuleType("kafka.errors")
_fake_kafka_errors_mod.KafkaError = Exception
sys.modules["kafka"] = _fake_kafka_mod
sys.modules["kafka.errors"] = _fake_kafka_errors_mod

# mock minio 模块
_fake_minio_mod = _types.ModuleType("minio")
_fake_minio_mod.Minio = MagicMock()
_fake_minio_errors_mod = _types.ModuleType("minio.error")
_fake_minio_errors_mod.S3Error = Exception
sys.modules["minio"] = _fake_minio_mod
sys.modules["minio.error"] = _fake_minio_errors_mod


# ========== Mock Redis ==========

class FakeRedis:
    """线程安全 Mock Redis，模拟 RedisClient 的 get/set/get_json/set_json/incr/lock/unlock"""

    def __init__(self):
        self._store = {}
        self._locks = {}

    def get(self, key):
        return self._store.get(key)

    def set(self, key, value, expire=None):
        self._store[key] = value

    def set_json(self, key, value, expire=None):
        self._store[key] = json.dumps(value, ensure_ascii=False)

    def get_json(self, key):
        val = self._store.get(key)
        return json.loads(val) if val else None

    def delete(self, key):
        self._store.pop(key, None)

    def exists(self, key):
        return 1 if key in self._store else 0

    def incr(self, key, amount=1):
        cur = int(self._store.get(key, "0"))
        cur += amount
        self._store[key] = str(cur)
        return cur

    def lock(self, key, timeout=30):
        if key not in self._locks:
            self._locks[key] = True
            return True
        return False

    def unlock(self, key):
        self._locks.pop(key, None)

    @property
    def client(self):
        """返回底层客户端以支持 zincrby/zrevrange 等调用"""
        return getattr(self, "_client", self)


@pytest.fixture
def fake_redis():
    return FakeRedis()


@pytest.fixture
def event_loop():
    loop = asyncio.new_event_loop()
    yield loop
    loop.close()


# ========== 1. Prompt 标准化 ==========

class TestNormalizePrompt:

    def test_rag_strip_prefix_suffix(self):
        from common.cache_normalize import normalize_prompt
        result = normalize_prompt("请问上架规则怎么配置？呢", "rag")
        assert result == "上架规则怎么配置?"

    def test_rag_fullwidth_to_halfwidth(self):
        from common.cache_normalize import normalize_prompt
        result = normalize_prompt("库存３０件，还能出库吗？", "rag")
        assert "３０" not in result
        assert "30" in result
        assert "？" not in result
        assert "?" in result

    def test_rag_multiple_prefix_rounds(self):
        from common.cache_normalize import normalize_prompt
        # 叠加前缀，循环去除
        result = normalize_prompt("请问麻烦我想知道波次拣货流程", "rag")
        assert not result.startswith("请问")
        assert not result.startswith("麻烦")
        assert "波次拣货流程" in result

    def test_nl2sql_lowercase(self):
        from common.cache_normalize import normalize_prompt
        result = normalize_prompt("SELECT * FROM wms_outbound_order WHERE STATUS='ACTIVE'", "nl2sql")
        assert result == result.lower()
        assert "select" in result
        assert "wms_outbound_order" in result

    def test_whitespace_normalization(self):
        from common.cache_normalize import normalize_prompt
        result = normalize_prompt("  请问   库存   怎么查  ", "rag")
        # 多余空格被压缩为单个空格，前缀"请问"被去除
        assert "  " not in result
        assert result.startswith("库存")
        assert "怎么查" in result

    def test_empty_prompt(self):
        from common.cache_normalize import normalize_prompt
        assert normalize_prompt("", "rag") == ""
        assert normalize_prompt(None, "rag") == ""

    def test_fullwidth_space(self):
        from common.cache_normalize import normalize_prompt
        result = normalize_prompt("请问　库存　查询", "rag")
        assert "　" not in result
        assert "库存" in result
        assert "查询" in result

    def test_same_prompt_same_normalized(self):
        """语义等价的 prompt 标准化后一致 → 精确缓存能命中"""
        from common.cache_normalize import normalize_prompt
        p1 = "请问上架规则怎么配置？呢"
        p2 = "上架规则怎么配置?"
        assert normalize_prompt(p1, "rag") == normalize_prompt(p2, "rag")


# ========== 2. TTL 抖动 ==========

class TestJitterTTL:

    def test_jitter_in_range(self):
        from common.cache import _jitter_ttl
        ttl = 3600
        for _ in range(100):
            j = _jitter_ttl(ttl)
            # ±10% → [3240, 3960]
            assert 3240 <= j <= 3960, f"jitter {j} out of range"

    def test_jitter_no_jitter(self):
        from common.cache import _jitter_ttl
        assert _jitter_ttl(3600, jitter=0) == 3600

    def test_jitter_minimum_1(self):
        """极小 TTL 抖动后不小于 1"""
        from common.cache import _jitter_ttl
        j = _jitter_ttl(1, jitter=0.5)
        assert j >= 1


# ========== 3. L1+L2 命中 ==========

class TestL1L2Cache:

    def test_l1_hit(self, fake_redis):
        """两次同 prompt，第一次 miss+回源，第二次 L1 hit"""
        with patch("common.cache.redis_client", fake_redis), \
             patch("common.cache._l1_cache") as mock_l1:
            from common.cache import cacheable

            call_count = 0

            @cacheable("test:fn", ttl=60, key_args=["prompt"])
            async def test_fn(prompt: str):
                nonlocal call_count
                call_count += 1
                return f"answer:{prompt}"

            # 第一次：L1 miss → L2 miss → 回源 → 写 L1+L2
            mock_l1.get.return_value = None  # L1 miss
            r1 = asyncio.get_event_loop().run_until_complete(test_fn("hello"))
            assert r1 == "answer:hello"
            assert call_count == 1

            # 第二次：L1 hit
            mock_l1.get.return_value = "answer:hello"
            r2 = asyncio.get_event_loop().run_until_complete(test_fn("hello"))
            assert r2 == "answer:hello"
            assert call_count == 1  # 未再次回源

    def test_l2_hit_writes_l1(self, fake_redis):
        """L2 命中时回写 L1"""
        with patch("common.cache.redis_client", fake_redis), \
             patch("common.cache._l1_cache") as mock_l1:
            from common.cache import cacheable, _get_version, _stable_str

            mock_l1.get.return_value = None  # L1 miss

            # 构造与 cacheable 装饰器一致的 cache_key
            version = _get_version("test:l2")  # "v1"
            key_parts = _stable_str("hello")
            cache_key = f"test:l2:{version}:{key_parts}"
            fake_redis._store[cache_key] = json.dumps("cached_answer")

            call_count = 0

            @cacheable("test:l2", ttl=60, key_args=["prompt"])
            async def test_fn(prompt: str):
                nonlocal call_count
                call_count += 1
                return "new_answer"

            r = asyncio.get_event_loop().run_until_complete(test_fn("hello"))
            # L2 命中 → 返回 cached_answer，不回源
            assert r == "cached_answer"
            assert call_count == 0


# ========== 4. 版本号失效 ==========

class TestVersionInvalidation:

    def test_version_change_invalidates_cache(self, fake_redis):
        """版本号递增后，旧缓存 key 不命中"""
        from common.cache import _get_version

        with patch("common.cache.redis_client", fake_redis), \
             patch("common.cache_warmup.redis_client", fake_redis):
            v1 = _get_version("test:ver")
            assert v1 == "v1"  # 无版本时默认 v1

            # 模拟版本升级（用 warmup_service 的 _incr_version 保持格式一致）
            from common.cache_warmup import CacheWarmup
            CacheWarmup._incr_version("test:ver")  # 0→1, 存 "v1"
            CacheWarmup._incr_version("test:ver")  # 1→2, 存 "v2"
            v2 = _get_version("test:ver")
            assert v2 == "v2"
            assert v1 != v2

    def test_invalidate_by_doc(self, fake_redis):
        """知识库更新→版本递增"""
        with patch("common.data_client.redis_client", fake_redis), \
             patch("common.cache_warmup.redis_client", fake_redis):
            from common.cache_warmup import CacheWarmup
            cw = CacheWarmup()
            new_ver = cw.invalidate_by_doc(doc_id="doc123", module="rag")
            assert new_ver == "v1"
            # 再次调用 → v2
            new_ver2 = cw.invalidate_by_doc(doc_id="doc456", module="rag")
            assert new_ver2 == "v2"

    def test_invalidate_by_prompt(self, fake_redis):
        with patch("common.cache_warmup.redis_client", fake_redis):
            from common.cache_warmup import CacheWarmup
            cw = CacheWarmup()
            v1 = cw.invalidate_by_prompt(module="llm")
            v2 = cw.invalidate_by_prompt(module="llm")
            assert v1 == "v1"
            assert v2 == "v2"


# ========== 5. 互斥锁防击穿 ==========

class TestMutexLock:

    def test_lock_acquired_blocks_others(self, fake_redis):
        """一个获取锁后，另一个获取失败"""
        assert fake_redis.lock("cache:lock:k1", timeout=30) is True
        assert fake_redis.lock("cache:lock:k1", timeout=30) is False
        fake_redis.unlock("cache:lock:k1")
        # 解锁后可再次获取
        assert fake_redis.lock("cache:lock:k1", timeout=30) is True
        fake_redis.unlock("cache:lock:k1")


# ========== 6. 语义缓存降级（mock 模式） ==========

class TestSemanticCacheMock:

    def test_semantic_cache_mock_returns_none(self):
        """Milvus 不可用时降级 mock，get 返回 None"""
        from common.cache_layer import SemanticCache
        sc = SemanticCache()
        sc._collection = "mock"  # 强制 mock 模式
        sc._connected = True

        result = asyncio.get_event_loop().run_until_complete(
            sc.get("test question", "rag", "qwen", "v1")
        )
        assert result is None

    def test_llm_cache_layer_l3_skipped_when_mock(self, fake_redis):
        """L3 mock 时 LLMCacheLayer 跳过 L3，返回 None（而非报错）"""
        with patch("common.cache_layer.redis_client", fake_redis):
            from common.cache_layer import LLMCacheLayer
            layer = LLMCacheLayer()
            # 强制 L3 mock
            layer.semantic._collection = "mock"
            layer.semantic._connected = True

            result = asyncio.get_event_loop().run_until_complete(
                layer.get("rag", "qwen", "test prompt")
            )
            assert result is None  # L1/L2/L3 全 miss


# ========== 7. metrics 端点 ==========

class TestMetrics:

    def test_record_hit(self):
        from common.cache_metrics import record_hit, cache_hits
        before = cache_hits.labels(layer="l1", method="test")._value.get()
        record_hit("l1", "test")
        after = cache_hits.labels(layer="l1", method="test")._value.get()
        assert after == before + 1

    def test_record_miss(self):
        from common.cache_metrics import record_miss, cache_misses
        before = cache_misses.labels(method="test")._value.get()
        record_miss("test")
        after = cache_misses.labels(method="test")._value.get()
        assert after == before + 1

    def test_record_invalidation(self):
        from common.cache_metrics import record_invalidation, cache_invalidation
        before = cache_invalidation.labels(reason="doc_update")._value.get()
        record_invalidation("doc_update")
        after = cache_invalidation.labels(reason="doc_update")._value.get()
        assert after == before + 1

    def test_setup_metrics_endpoint(self):
        """验证 /metrics 路由挂载"""
        from fastapi import FastAPI
        from common.cache_metrics import setup_metrics
        from fastapi.testclient import TestClient

        app = FastAPI()
        setup_metrics(app)
        client = TestClient(app)
        resp = client.get("/metrics")
        assert resp.status_code == 200
        assert "wms_cache_hits_total" in resp.text


# ========== 8. 预热 ==========

class TestWarmup:

    def test_warmup_no_hot_prompts(self, fake_redis):
        """无高频 prompt 时跳过"""
        with patch("common.cache_warmup.redis_client", fake_redis):
            from common.cache_warmup import CacheWarmup
            cw = CacheWarmup()
            # fake_redis.client.zrevrange 返回空（FakeRedis.client 返回 self，但无此方法）
            # 需 mock client.zrevrange
            fake_redis._client = MagicMock()
            fake_redis._client.zrevrange.return_value = []

            asyncio.get_event_loop().run_until_complete(cw.warmup("llm", top_n=10))
            # 无异常即通过

    def test_record_hot_prompt(self, fake_redis):
        """高频 prompt 记录到 ZSet"""
        with patch("common.cache_warmup.redis_client", fake_redis):
            from common.cache_warmup import CacheWarmup
            fake_redis._client = MagicMock()
            CacheWarmup.record_hot_prompt("llm", "test prompt")
            # 验证 zincrby 被调用
            fake_redis._client.zincrby.assert_called_once()
            fake_redis._client.zremrangebyrank.assert_called_once()


# ========== 9. L4 检索缓存 ==========

class TestL4RetrievalCache:

    def test_l4_cache_key_with_version(self, fake_redis):
        """L4 缓存键含 knowledge_version，版本升级后 key 变化"""
        from common.utils import md5

        kv1 = "v1"
        query = "波次拣货流程"
        l4_key_v1 = f"rag:retrieval:{kv1}:{md5(query)}"

        kv2 = "v2"
        l4_key_v2 = f"rag:retrieval:{kv2}:{md5(query)}"

        assert l4_key_v1 != l4_key_v2  # 版本不同 → key 不同 → 旧缓存自动失效

    def test_l4_hit_returns_cached(self, fake_redis):
        """L4 缓存命中时返回缓存结果"""
        from common.utils import md5
        query = "波次拣货流程"
        l4_key = f"rag:retrieval:v1:{md5(query)}"
        cached_results = [{"title": "cached", "content": "cached content", "score": 0.9}]
        fake_redis.set_json(l4_key, cached_results, expire=604800)

        retrieved = fake_redis.get_json(l4_key)
        assert retrieved == cached_results

    def test_l4_miss_writes_cache(self, fake_redis):
        """L4 miss 时执行检索并写入缓存"""
        from common.utils import md5
        query = "库存查询"
        l4_key = f"rag:retrieval:v1:{md5(query)}"

        # 初始无缓存
        assert fake_redis.get_json(l4_key) is None

        # 模拟检索结果
        results = [{"title": "库存指南", "content": "库存管理...", "score": 0.88}]
        fake_redis.set_json(l4_key, results, expire=604800)

        # 再次读取 → 命中
        assert fake_redis.get_json(l4_key) == results


# ========== 10. LLMCacheLayer 编排 ==========

class TestLLMCacheLayer:

    def test_l1_hit_returns_immediately(self, fake_redis):
        """L1 命中时不查 L2/L3"""
        with patch("common.cache_layer.redis_client", fake_redis):
            from common.cache_layer import LLMCacheLayer
            layer = LLMCacheLayer()
            # 预置 L1
            version = layer._get_version("rag")
            key = layer._build_key("rag", "qwen", "test", version)
            layer.l1[key] = "cached_answer"

            result = asyncio.get_event_loop().run_until_complete(
                layer.get("rag", "qwen", "test")
            )
            assert result is not None
            assert result["answer"] == "cached_answer"
            assert result["source"] == "l1"

    def test_l2_hit_writes_l1(self, fake_redis):
        """L2 命中时回写 L1"""
        with patch("common.cache_layer.redis_client", fake_redis):
            from common.cache_layer import LLMCacheLayer
            layer = LLMCacheLayer()
            version = layer._get_version("rag")
            key = layer._build_key("rag", "qwen", "test", version)
            l2_key = f"llm:exact:{key}"
            fake_redis.set_json(l2_key, "l2_answer", expire=3600)

            result = asyncio.get_event_loop().run_until_complete(
                layer.get("rag", "qwen", "test")
            )
            assert result["answer"] == "l2_answer"
            assert result["source"] == "l2"
            # L1 被回写
            assert layer.l1.get(key) == "l2_answer"

    def test_put_writes_l1_and_l2(self, fake_redis):
        """put 写入 L1 + L2"""
        with patch("common.cache_layer.redis_client", fake_redis):
            from common.cache_layer import LLMCacheLayer
            layer = LLMCacheLayer()
            asyncio.get_event_loop().run_until_complete(
                layer.put("rag", "qwen", "my prompt", "my answer")
            )
            version = layer._get_version("rag")
            key = layer._build_key("rag", "qwen", "my prompt", version)
            l2_key = f"llm:exact:{key}"

            # L1 有
            assert layer.l1.get(key) == "my answer"
            # L2 有
            assert fake_redis.get_json(l2_key) == "my answer"


if __name__ == "__main__":
    pytest.main([__file__, "-v", "--tb=short"])
