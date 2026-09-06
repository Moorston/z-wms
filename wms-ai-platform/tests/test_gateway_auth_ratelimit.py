"""
WMS AI Platform - 网关限流+鉴权单元测试（TDD）

测试缝隙（seams）：
  - gateway.main.verify_auth(request) — 双模式鉴权公共函数
  - gateway.main.rate_limit(client_ip) — Redis+Lua 滑动窗口限流公共函数
  - gateway.main.get_http_client() — 全局连接池公共函数

验证行为（不测实现细节）：
  1. 开发环境跳过鉴权，返回 dev_user
  2. API Key 鉴权：有效 key 返回 client 名，无效 key 走下一模式
  3. JWT 鉴权：有效 token 解码 payload，无效 token 返回 401
  4. 无任何凭证返回 401
  5. 限流：Redis 返回 1（允许）时不抛异常
  6. 限流：Redis 返回 0（超限）时抛 HTTPException 429
  7. 限流：Redis 不可用时降级放行（不抛异常）
"""
import asyncio
import os
import sys
import json
import time
from unittest.mock import MagicMock, patch, AsyncMock, PropertyMock
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

# mock pymilvus
_fake_pymilvus = _types.ModuleType("pymilvus")
_fake_pymilvus.MilvusClient = MagicMock
_fake_pymilvus.DataType = MagicMock()
sys.modules["pymilvus"] = _fake_pymilvus

# mock clickhouse
_fake_ch = _types.ModuleType("clickhouse_driver")
_fake_ch.Client = MagicMock()
sys.modules["clickhouse_driver"] = _fake_ch

# mock pymysql
_fake_pymysql = _types.ModuleType("pymysql")
_fake_pymysql.connect = MagicMock()
sys.modules["pymysql"] = _fake_pymysql

# mock aiomysql
_fake_aiomysql = _types.ModuleType("aiomysql")
_fake_aiomysql.connect = MagicMock()
sys.modules["aiomysql"] = _fake_aiomysql


# ========== 辅助：构造 Request mock ==========

def make_request(headers=None, client_ip="127.0.0.1"):
    """构造 FastAPI Request 的 mock 对象"""
    headers = headers or {}
    return SimpleNamespace(
        headers=headers,
        client=SimpleNamespace(host=client_ip),
        url=SimpleNamespace(path="/api/rag/chat"),
        method="GET",
        query_params={},
        state=SimpleNamespace(),
    )


# ========== 1. 鉴权：开发环境跳过 ==========

class TestVerifyAuthDevMode:

    def test_dev_env_skips_auth_returns_dev_user(self):
        """开发环境跳过鉴权，返回 dev_user"""
        with patch("gateway.main.settings") as mock_settings:
            mock_settings.env = "development"
            from gateway.main import verify_auth
            req = make_request(headers={})
            result = verify_auth(req)
        assert result["user_id"] == "dev_user"
        assert result["auth_mode"] == "dev"


# ========== 2. 鉴权：API Key 模式 ==========

class TestVerifyAuthApiKey:

    def test_valid_api_key_returns_client_name(self):
        """有效 API Key 鉴权返回 client 名"""
        with patch("gateway.main.settings") as mock_settings, \
             patch("gateway.main._API_KEYS", {"sk-test-key-123": "wms-core"}):
            mock_settings.env = "production"
            from gateway.main import verify_auth
            req = make_request(headers={"X-API-Key": "sk-test-key-123"})
            result = verify_auth(req)
        assert result["client"] == "wms-core"
        assert result["auth_mode"] == "api_key"

    def test_invalid_api_key_falls_through_to_jwt_check(self):
        """无效 API Key 不匹配时，继续走 JWT 鉴权路径"""
        with patch("gateway.main.settings") as mock_settings, \
             patch("gateway.main._API_KEYS", {"sk-valid": "wms-core"}):
            mock_settings.env = "production"
            from gateway.main import verify_auth
            # 无效 API Key + 无 JWT → 401
            req = make_request(headers={"X-API-Key": "sk-invalid"})
            from fastapi import HTTPException
            with pytest.raises(HTTPException) as exc_info:
                verify_auth(req)
            assert exc_info.value.status_code == 401


# ========== 3. 鉴权：JWT 模式 ==========

class TestVerifyAuthJwt:

    def test_valid_jwt_token_returns_payload(self):
        """有效 JWT Token 解码返回 payload"""
        from jose import jwt as jose_jwt
        secret = "your-jwt-secret-key-change-in-production"
        token = jose_jwt.encode(
            {"user_id": "u100", "username": "alice"},
            secret,
            algorithm="HS256",
        )
        with patch("gateway.main.settings") as mock_settings, \
             patch("gateway.main._API_KEYS", {}):
            mock_settings.env = "production"
            mock_settings.jwt_secret = secret
            mock_settings.jwt_algorithm = "HS256"
            from gateway.main import verify_auth
            req = make_request(headers={"Authorization": f"Bearer {token}"})
            result = verify_auth(req)
        assert result["user_id"] == "u100"
        assert result["username"] == "alice"
        assert result["auth_mode"] == "jwt"

    def test_invalid_jwt_token_raises_401(self):
        """无效 JWT Token 返回 401"""
        with patch("gateway.main.settings") as mock_settings, \
             patch("gateway.main._API_KEYS", {}):
            mock_settings.env = "production"
            mock_settings.jwt_secret = "your-jwt-secret-key-change-in-production"
            mock_settings.jwt_algorithm = "HS256"
            from gateway.main import verify_auth
            from fastapi import HTTPException
            req = make_request(headers={"Authorization": "Bearer invalid.token.here"})
            with pytest.raises(HTTPException) as exc_info:
                verify_auth(req)
            assert exc_info.value.status_code == 401

    def test_no_credentials_raises_401(self):
        """无任何凭证返回 401"""
        with patch("gateway.main.settings") as mock_settings, \
             patch("gateway.main._API_KEYS", {}):
            mock_settings.env = "production"
            from gateway.main import verify_auth
            from fastapi import HTTPException
            req = make_request(headers={})
            with pytest.raises(HTTPException) as exc_info:
                verify_auth(req)
            assert exc_info.value.status_code == 401


# ========== 4. 限流：Redis+Lua 滑动窗口 ==========

class _FakeScript:
    """模拟 Redis Lua 脚本执行结果"""
    def __init__(self, return_value):
        self._return_value = return_value
    def __call__(self, keys=None, args=None):
        return self._return_value


class TestRateLimit:

    def test_rate_limit_allows_when_under_limit(self):
        """Redis 返回 1（未超限）时不抛异常"""
        fake_redis_client = MagicMock()
        fake_redis_client.register_script = MagicMock(return_value=_FakeScript(1))
        fake_data_client = MagicMock()
        fake_data_client.client = fake_redis_client

        with patch("gateway.main._get_redis", return_value=fake_data_client):
            from gateway.main import rate_limit
            # 不应抛异常
            rate_limit("192.168.1.1", limit=100, window=60)

    def test_rate_limit_raises_429_when_over_limit(self):
        """Redis 返回 0（超限）时抛 HTTPException 429"""
        fake_redis_client = MagicMock()
        fake_redis_client.register_script = MagicMock(return_value=_FakeScript(0))
        fake_data_client = MagicMock()
        fake_data_client.client = fake_redis_client

        with patch("gateway.main._get_redis", return_value=fake_data_client):
            from gateway.main import rate_limit
            from fastapi import HTTPException
            with pytest.raises(HTTPException) as exc_info:
                rate_limit("192.168.1.1", limit=100, window=60)
            assert exc_info.value.status_code == 429

    def test_rate_limit_degrades_when_redis_unavailable(self):
        """Redis 不可用时降级放行，不抛异常"""
        with patch("gateway.main._get_redis", side_effect=Exception("connection refused")):
            from gateway.main import rate_limit
            # 不应抛异常（降级放行）
            rate_limit("192.168.1.1", limit=100, window=60)
