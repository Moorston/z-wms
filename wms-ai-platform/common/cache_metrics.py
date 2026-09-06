"""
WMS AI Platform - 缓存监控指标
基于 prometheus-client，暴露 /metrics 端点
指标：
  - wms_cache_hits_total{layer,method}     缓存命中次数（按层级+方法）
  - wms_cache_misses_total{method}          缓存未命中次数（按方法）
  - wms_cache_latency_seconds{layer}        缓存读写延迟（直方图）
  - wms_cache_errors_total{op}              缓存操作错误次数
  - wms_cache_size{layer}                  缓存条目数（Gauge）
  - wms_cache_cost_saved_yuan{method}       节省 API 费用（元，估算）
  - wms_cache_invalidation_total{reason}   主动失效次数（按原因）
"""
from prometheus_client import Counter, Histogram, Gauge, generate_latest

# 缓存命中（按层级 L1/L2/L3/L4/L5 + 方法名）
cache_hits = Counter(
    "wms_cache_hits_total",
    "缓存命中次数",
    ["layer", "method"],
)

# 缓存未命中（按方法名）
cache_misses = Counter(
    "wms_cache_misses_total",
    "缓存未命中次数",
    ["method"],
)

# 缓存读写延迟（按层级）
cache_latency = Histogram(
    "wms_cache_latency_seconds",
    "缓存读写延迟（秒）",
    ["layer"],
    buckets=(0.001, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0),
)

# 缓存操作错误（按操作类型 read/write/lock/embed/search）
cache_errors = Counter(
    "wms_cache_errors_total",
    "缓存操作错误次数",
    ["op"],
)

# 缓存条目数（按层级，Gauge 可增可减）
cache_size = Gauge(
    "wms_cache_size",
    "缓存条目数",
    ["layer"],
)

# 节省 API 费用（估算，元）
cost_saved = Counter(
    "wms_cache_cost_saved_yuan",
    "节省 API 费用（元，估算）",
    ["method"],
)

# 主动失效次数（按原因 doc_update/prompt_update/model_update/feedback）
cache_invalidation = Counter(
    "wms_cache_invalidation_total",
    "缓存主动失效次数",
    ["reason"],
)


def record_hit(layer: str, method: str):
    """记录缓存命中"""
    cache_hits.labels(layer=layer, method=method).inc()


def record_miss(method: str):
    """记录缓存未命中"""
    cache_misses.labels(method=method).inc()


def record_cost_saved(method: str, yuan: float = 0.01):
    """记录节省的 API 费用（默认每次命中省 0.01 元）"""
    cost_saved.labels(method=method).inc(yuan)


def record_invalidation(reason: str):
    """记录主动失效"""
    cache_invalidation.labels(reason=reason).inc()


def setup_metrics(app):
    """
    在 FastAPI app 上挂载 /metrics 端点

    用法:
        from common.cache_metrics import setup_metrics
        setup_metrics(app)
    """
    from fastapi import Response

    @app.get("/metrics", tags=["monitoring"])
    async def metrics():
        return Response(
            generate_latest(),
            media_type="text/plain; version=0.0.4; charset=utf-8",
        )
