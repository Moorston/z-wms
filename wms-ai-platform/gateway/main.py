"""
WMS AI Platform - API网关
统一入口：鉴权/限流/路由/日志/链路追踪

PRD V2.0 改动（B5-T1）：
  - 内存字典限流 → Redis + Lua 滑动窗口分布式限流（多实例一致）
  - httpx.AsyncClient 每请求新建 → 全局连接池复用
  - 新增 API Key 双模式鉴权（JWT + API Key，PRD 7.3）
  - 生成 TraceId 透传 header X-Trace-Id（B5-T2 追踪基础）
"""
import os
import sys
import time
import uuid
from typing import Optional

from fastapi import FastAPI, Request, HTTPException
from fastapi.responses import JSONResponse
import httpx
from loguru import logger
from jose import jwt, JWTError

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from common.config import settings
from common.utils import Result, gen_trace_id
from common.cache_metrics import setup_metrics
from common.tracing import init_tracing, instrument_app

app = FastAPI(title="WMS AI Gateway", version="2.0.0")
# 挂载 /metrics 监控端点
setup_metrics(app)
init_tracing("gateway")
instrument_app(app, "gateway")

# 服务路由表
SERVICE_ROUTES = {
    "/api/ocr": os.getenv("OCR_SERVICE_URL", "http://localhost:8101"),
    "/api/forecast": os.getenv("FORECAST_SERVICE_URL", "http://localhost:8102"),
    "/api/report": os.getenv("REPORT_SERVICE_URL", "http://localhost:8103"),
    "/api/rag": os.getenv("RAG_SERVICE_URL", "http://localhost:8104"),
    "/api/aiops": os.getenv("AIOPS_SERVICE_URL", "http://localhost:8105"),
    "/api/voice": os.getenv("VOICE_SERVICE_URL", "http://localhost:8106"),
    "/api/drone": os.getenv("DRONE_SERVICE_URL", "http://localhost:8107"),
    "/api/video": os.getenv("VIDEO_SERVICE_URL", "http://localhost:8108"),
    "/api/model": os.getenv("MODEL_SERVICE_URL", "http://localhost:8001"),
    "/api/data": os.getenv("DATA_SERVICE_URL", "http://localhost:8002"),
}

# ===== 请求体大小限制（默认 50MB，可通过环境变量调整）=====
MAX_BODY_SIZE = int(os.getenv("GATEWAY_MAX_BODY_SIZE", str(50 * 1024 * 1024)))

# ===== 全局 httpx 连接池（复用，避免每请求新建）=====
_http_client: Optional[httpx.AsyncClient] = None


def get_http_client() -> httpx.AsyncClient:
    """获取全局复用的 httpx 连接池"""
    global _http_client
    if _http_client is None or _http_client.is_closed:
        _http_client = httpx.AsyncClient(
            timeout=httpx.Timeout(60.0, connect=5.0),
            limits=httpx.Limits(max_connections=100, max_keepalive_connections=20),
        )
    return _http_client


@app.on_event("shutdown")
async def close_http_client():
    global _http_client
    if _http_client and not _http_client.is_closed:
        await _http_client.aclose()


# ===== Redis 滑动窗口限流（Lua 脚本，原子操作）=====
# 滑动窗口：用 ZSET 记录窗口内每次请求的时间戳，清除过期 + 计数 + 添加
_RATE_LUA_SCRIPT = """
local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
-- 清除窗口外的旧记录
redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
local count = redis.call('ZCARD', key)
if count >= limit then
    return 0
end
redis.call('ZADD', key, now, now .. '-' .. math.random(1000000))
redis.call('EXPIRE', key, window + 1)
return 1
"""

# API Key 集合（从环境变量加载，逗号分隔；格式 api_key:client_name）
_API_KEYS = {}
_raw_keys = os.getenv("GATEWAY_API_KEYS", "")
for _item in _raw_keys.split(","):
    _item = _item.strip()
    if _item:
        if ":" in _item:
            _k, _v = _item.split(":", 1)
            _API_KEYS[_k] = _v
        else:
            _API_KEYS[_item] = "api_client"


def _get_redis():
    """获取 Redis 客户端（从 data_client 复用，懒加载）"""
    from common.data_client import redis_client
    return redis_client


def rate_limit(client_ip: str, limit: int = 100, window: int = 60):
    """
    Redis + Lua 滑动窗口分布式限流（PRD 7.3）
    - 按 IP 限流，滑动窗口精确计数
    - Redis TTL 自动过期，无需手动清理
    - 多实例一致（共享 Redis 计数）
    - Redis 不可用时降级放行（不阻断业务）
    """
    try:
        r = _get_redis()
        # 注册 Lua 脚本（每次注册开销极低，Redis 内部会缓存）
        script = r.client.register_script(_RATE_LUA_SCRIPT)
        key = f"gateway:ratelimit:{client_ip}"
        allowed = script(keys=[key], args=[int(time.time() * 1000), window * 1000, limit])
        if not allowed:
            raise HTTPException(status_code=429, detail="请求过于频繁")
    except HTTPException:
        raise
    except Exception as e:
        # Redis 不可用时降级放行，仅记日志（不阻断业务）
        logger.warning(f"Redis 限流降级放行: {e}")


def verify_auth(request: Request) -> dict:
    """
    双模式鉴权（PRD 7.3）：JWT + API Key
    - Authorization: Bearer <jwt> → JWT 验证
    - X-API-Key: <key> → API Key 验证
    开发环境跳过验证
    """
    # 开发环境跳过
    if settings.env == "development":
        return {"user_id": "dev_user", "username": "developer", "auth_mode": "dev"}

    # 1. 尝试 API Key 鉴权
    api_key = request.headers.get("X-API-Key", "")
    if api_key and api_key in _API_KEYS:
        return {"client": _API_KEYS[api_key], "auth_mode": "api_key"}

    # 2. JWT Token 鉴权
    auth = request.headers.get("Authorization", "")
    if auth.startswith("Bearer "):
        token = auth[7:]
        try:
            payload = jwt.decode(token, settings.jwt_secret, algorithms=[settings.jwt_algorithm])
            payload["auth_mode"] = "jwt"
            return payload
        except JWTError:
            raise HTTPException(status_code=401, detail="Token无效")

    raise HTTPException(status_code=401, detail="未授权：需要 JWT Token 或 API Key")


@app.middleware("http")
async def proxy_middleware(request: Request, call_next):
    """代理中间件：限流+鉴权+路由+日志+TraceId"""
    start = time.time()
    client_ip = request.client.host if request.client else "unknown"

    # 生成/透传 TraceId
    trace_id = request.headers.get("X-Trace-Id") or gen_trace_id()
    request.state.trace_id = trace_id

    # 健康检查和文档直接放行
    path = request.url.path
    if path in ["/health", "/docs", "/openapi.json", "/redoc", "/metrics"]:
        return await call_next(request)

    # 限流
    try:
        rate_limit(client_ip)
    except HTTPException as e:
        return JSONResponse(status_code=429, content={"code": 429, "message": e.detail})

    # 鉴权（双模式）
    try:
        auth_info = verify_auth(request)
        request.state.auth_info = auth_info
    except HTTPException as e:
        return JSONResponse(status_code=401, content={"code": 401, "message": e.detail})

    # 路由匹配
    target_url = None
    for prefix, base_url in SERVICE_ROUTES.items():
        if path.startswith(prefix):
            service_path = path.replace(prefix, "", 1)
            target_url = f"{base_url}{service_path}"
            break

    if not target_url:
        return JSONResponse(status_code=404, content={"code": 404, "message": "服务未找到"})

    # 请求体大小限制（防止 OOM）
    content_length = request.headers.get("content-length")
    if content_length and int(content_length) > MAX_BODY_SIZE:
        return JSONResponse(
            status_code=413,
            content={"code": 413, "message": f"请求体过大，最大 {MAX_BODY_SIZE // (1024 * 1024)}MB"},
        )

    # 转发请求（复用全局连接池）
    try:
        client = get_http_client()
        # 转发请求头（去掉 host，注入 TraceId）
        headers = {k: v for k, v in request.headers.items() if k.lower() != "host"}
        headers["X-Trace-Id"] = trace_id

        if request.method == "GET":
            resp = await client.get(target_url, params=request.query_params, headers=headers)
        elif request.method == "POST":
            body = await request.body()
            if len(body) > MAX_BODY_SIZE:
                return JSONResponse(
                    status_code=413,
                    content={"code": 413, "message": f"请求体过大，最大 {MAX_BODY_SIZE // (1024 * 1024)}MB"},
                )
            resp = await client.post(target_url, content=body, params=request.query_params, headers=headers)
        elif request.method == "PUT":
            body = await request.body()
            if len(body) > MAX_BODY_SIZE:
                return JSONResponse(
                    status_code=413,
                    content={"code": 413, "message": f"请求体过大，最大 {MAX_BODY_SIZE // (1024 * 1024)}MB"},
                )
            resp = await client.put(target_url, content=body, params=request.query_params, headers=headers)
        elif request.method == "DELETE":
            resp = await client.delete(target_url, params=request.query_params, headers=headers)
        else:
            return JSONResponse(status_code=405, content={"code": 405, "message": "方法不支持"})

        cost = (time.time() - start) * 1000
        logger.info(f"[{request.method}] {path} -> {target_url} {resp.status_code} {cost:.0f}ms trace={trace_id}")

        # 响应头注入 TraceId
        content_type = resp.headers.get("content-type", "")
        if content_type.startswith("text/event-stream"):
            # SSE/流式响应：直接返回 StreamingResponse，不解析 JSON
            from fastapi.responses import StreamingResponse
            return StreamingResponse(
                content=resp.aiter_content(),
                status_code=resp.status_code,
                media_type="text/event-stream",
            )
        if content_type.startswith("application/json"):
            content = resp.json()
            if isinstance(content, dict):
                content["trace_id"] = trace_id
            return JSONResponse(status_code=resp.status_code, content=content)
        return JSONResponse(status_code=resp.status_code, content=resp.content)
    except httpx.ConnectError:
        return JSONResponse(status_code=503, content={"code": 503, "message": "后端服务不可用"})
    except Exception as e:
        logger.error(f"网关转发失败: {e}")
        return JSONResponse(status_code=500, content={"code": 500, "message": str(e)})


@app.get("/health")
async def health():
    return Result.success({
        "status": "ok",
        "services": {k: v for k, v in SERVICE_ROUTES.items()},
        "rate_limit": "redis",
    })


@app.get("/api/services")
async def list_services():
    """列出所有已注册服务"""
    return Result.success(SERVICE_ROUTES)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=settings.gateway_port)
