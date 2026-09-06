"""
WMS AI Platform - 链路追踪配置（PRD V2.0 B5-T2）

功能：
  - OpenTelemetry 配置（OTLP exporter），自动注入 TraceId
  - 网关生成 TraceId → 透传到后端服务（header X-Trace-Id）
  - LLM 调用日志写入 ClickHouse llm_call_log 表
  - 各模块 /metrics 端点暴露 QPS/延迟/错误率/缓存命中率

设计决策：
  - OpenTelemetry 为可选依赖（未安装时降级为 no-op，不影响启动）
  - TraceId 优先从请求头 X-Trace-Id 读取（网关已生成），否则自动生成
  - LLM 日志异步写入 ClickHouse，写入失败降级仅记日志（不阻断业务）
"""
import os
import time
from typing import Optional

from loguru import logger

from common.utils import gen_trace_id, gen_id, now_str


# ===== OpenTelemetry 配置（可选依赖）=====
_tracer = None
_otlp_enabled = False

try:
    from opentelemetry import trace
    from opentelemetry.sdk.trace import TracerProvider
    from opentelemetry.sdk.trace.export import BatchSpanProcessor
    from opentelemetry.sdk.resources import Resource
    from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
    from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor

    _OTLP_AVAILABLE = True
except ImportError:
    _OTLP_AVAILABLE = False


def init_tracing(service_name: str = "wms-ai-platform"):
    """
    初始化 OpenTelemetry 链路追踪（PRD 5.7）

    配置 OTLP exporter 指向 collector（默认 localhost:4318）
    未安装 opentelemetry 包时降级为 no-op

    Args:
        service_name: 服务名（如 gateway / model-service / module-rag）
    """
    global _tracer, _otlp_enabled

    if not _OTLP_AVAILABLE:
        logger.info("OpenTelemetry 未安装，链路追踪降级为 no-op")
        return None

    otlp_endpoint = os.getenv("OTLP_ENDPOINT", "http://localhost:4318/v1/traces")
    _otlp_enabled = os.getenv("OTEL_ENABLED", "false").lower() == "true"

    if not _otlp_enabled:
        logger.info("链路追踪未启用（设置 OTEL_ENABLED=true 启用）")
        return None

    try:
        resource = Resource.create({"service.name": service_name})
        provider = TracerProvider(resource=resource)
        exporter = OTLPSpanExporter(endpoint=otlp_endpoint)
        provider.add_span_processor(BatchSpanProcessor(exporter))
        trace.set_tracer_provider(provider)
        _tracer = trace.get_tracer(service_name)
        logger.info(f"OpenTelemetry 链路追踪已启用: {service_name} → {otlp_endpoint}")
        return _tracer
    except Exception as e:
        logger.warning(f"OpenTelemetry 初始化失败，降级 no-op: {e}")
        _otlp_enabled = False
        return None


def get_tracer():
    """获取 tracer 实例（未启用时返回 None）"""
    return _tracer


def instrument_app(app, service_name: str = None):
    """
    自动注入 FastAPI 应用的链路追踪中间件

    Args:
        app: FastAPI 实例
        service_name: 服务名
    """
    if not _OTLP_AVAILABLE or not _otlp_enabled:
        return

    try:
        FastAPIInstrumentor.instrument_app(app)
        logger.info(f"FastAPI 链路追踪注入完成: {service_name or 'app'}")
    except Exception as e:
        logger.warning(f"FastAPI 链路追踪注入失败: {e}")


# ===== TraceId 透传 =====
def get_trace_id(request=None) -> str:
    """
    获取当前请求的 TraceId

    优先级：
      1. 请求头 X-Trace-Id（网关已生成）
      2. OpenTelemetry 当前 span context
      3. 自动生成新 TraceId

    Args:
        request: FastAPI Request 对象（可选）
    Returns:
        TraceId 字符串
    """
    # 1. 从请求头读取（网关已注入）
    if request is not None:
        header_id = request.headers.get("X-Trace-Id")
        if header_id:
            return header_id

    # 2. 从 OpenTelemetry span context 读取
    if _otlp_enabled and _OTLP_AVAILABLE:
        try:
            span = trace.get_current_span()
            ctx = span.get_span_context()
            if ctx and ctx.is_valid:
                return format(ctx.trace_id, "032x")
        except Exception:
            pass

    # 3. 自动生成
    return gen_trace_id()


# ===== LLM 调用日志 =====
class LLMCallLogger:
    """
    LLM 调用日志记录器（PRD 6.2.5）

    将每次 LLM 调用的成本、延迟、缓存命中、模型路由等信息
    异步写入 ClickHouse llm_call_log 表
    """

    def __init__(self):
        self._ch_client = None
        self._table = "llm_call_log"

    def _get_client(self):
        """懒加载 ClickHouse 客户端"""
        if self._ch_client is not None:
            return self._ch_client
        try:
            from common.data_client import data_client
            # 确认 data_client 可用
            if data_client and not data_client.is_mock:
                self._ch_client = data_client
            return self._ch_client
        except Exception as e:
            logger.debug(f"LLM 日志 ClickHouse 客户端不可用: {e}")
            return None

    def log_call(
        self,
        task_type: str,
        model: str,
        prompt: str,
        answer: str,
        latency_ms: float,
        cache_hit: str = "none",
        cache_key: str = "",
        status: str = "success",
        fallback_used: bool = False,
        prompt_tokens: int = 0,
        completion_tokens: int = 0,
        total_tokens: int = 0,
        cost: float = 0.0,
        error_msg: str = None,
        user_id: str = None,
        module: str = "unknown",
        trace_id: str = None,
    ):
        """
        记录一次 LLM 调用日志

        写入失败时降级为仅记日志，不阻断业务
        """
        call_id = gen_id()
        trace_id = trace_id or gen_trace_id()

        # 估算 token 数（若未提供）：粗略 1 token ≈ 4 字符
        if total_tokens == 0:
            prompt_tokens = prompt_tokens or len(prompt) // 4
            completion_tokens = completion_tokens or len(answer) // 4
            total_tokens = prompt_tokens + completion_tokens

        # 估算成本（若未提供）：DeepSeek ≈ $0.002/1K tokens
        if cost == 0.0 and total_tokens > 0:
            cost = round(total_tokens / 1000 * 0.002, 6)

        try:
            client = self._get_client()
            if client is None:
                # ClickHouse 不可用，降级仅记日志
                logger.debug(
                    f"LLM调用(降级日志): task={task_type} model={model} "
                    f"latency={latency_ms:.0f}ms cache={cache_hit} status={status} "
                    f"trace={trace_id}"
                )
                return

            # 构建参数化查询（使用 ClickHouse @paramName 语法，避免 SQL 注入风险）
            params = {
                "p_call_id": call_id,
                "p_task_type": task_type,
                "p_model": model,
                "p_fallback_used": int(fallback_used),
                "p_prompt_tokens": prompt_tokens,
                "p_completion_tokens": completion_tokens,
                "p_total_tokens": total_tokens,
                "p_cost": cost,
                "p_latency_ms": int(latency_ms),
                "p_cache_hit": cache_hit,
                "p_cache_key": cache_key,
                "p_status": status,
                "p_error_msg": error_msg or "",
                "p_user_id": user_id or "",
                "p_module": module,
                "p_trace_id": trace_id,
            }
            sql = (
                f"INSERT INTO {self._table} "
                f"(call_id, task_type, model, fallback_used, prompt_tokens, "
                f"completion_tokens, total_tokens, cost, latency_ms, cache_hit, "
                f"cache_key, status, error_msg, user_id, module, trace_id, created_at) "
                f"VALUES (@p_call_id, @p_task_type, @p_model, @p_fallback_used, "
                f"@p_prompt_tokens, @p_completion_tokens, @p_total_tokens, "
                f"@p_cost, @p_latency_ms, @p_cache_hit, @p_cache_key, "
                f"@p_status, @p_error_msg, @p_user_id, @p_module, @p_trace_id, "
                f"now())"
            )
            client.query_olap(sql, params=params)
            logger.debug(f"LLM调用已记录: {call_id} trace={trace_id}")
        except Exception as e:
            # 写入失败不阻断业务
            logger.debug(f"LLM调用日志写入失败(降级): {e}")


# 全局单例
llm_call_logger = LLMCallLogger()
