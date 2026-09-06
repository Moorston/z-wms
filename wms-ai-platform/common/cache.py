"""
WMS AI Platform - AI接口缓存（增强版）
装饰器：在 ai_client 方法上透明加多级缓存
- L1 本地缓存（cachetools.TTLCache）→ L2 Redis 精确缓存 → 原方法
- 命中即返回，未命中执行原方法后回写 L1+L2
- Prompt 标准化（key_args 含 "prompt" 时自动归一化）
- 版本号注入（cache key 含 version，版本升级→旧缓存自动失效）
- TTL 随机抖动（防雪崩，±10%）
- 互斥锁防击穿（热点 key 未命中时只允许一个回源）
- Redis/L1 不可用时静默降级，不影响主流程
- bytes 参数取 md5，其他参数取 str() 参与缓存 key
"""
import hashlib
import asyncio
import inspect
import random
from functools import wraps
from loguru import logger

try:
    from cachetools import TTLCache
except ImportError:
    TTLCache = None  # cachetools 未安装时降级为无 L1

from common.config import settings
from common.data_client import redis_client

# ========== L1 本地缓存（进程级单例）==========
if TTLCache is not None:
    _l1_cache = TTLCache(maxsize=settings.cache_l1_max_size, ttl=settings.cache_l1_ttl)
else:
    _l1_cache = None


def _md5(data) -> str:
    """bytes/bytearray → 32位md5（用于图片等大对象参与缓存key）"""
    if isinstance(data, str):
        data = data.encode("utf-8")
    return hashlib.md5(data).hexdigest()


def _stable_str(val) -> str:
    """非bytes参数转稳定字符串（list/dict走json保证顺序无关）"""
    if isinstance(val, (list, dict, tuple)):
        import json
        return json.dumps(val, sort_keys=True, ensure_ascii=False, default=str)
    return str(val)


def _jitter_ttl(ttl: int, jitter: float = None) -> int:
    """
    TTL 随机抖动（防雪崩）
    actual_ttl = ttl * (1 - jitter + 2 * jitter * random)
    输出范围 [ttl*(1-jitter), ttl*(1+jitter)]
    """
    if jitter is None:
        jitter = settings.cache_ttl_jitter
    if jitter <= 0:
        return ttl
    factor = 1 - jitter + 2 * jitter * random.random()
    return max(1, int(ttl * factor))


def _get_version(prefix: str) -> str:
    """
    从 Redis 读缓存版本号（版本升级→旧 key 自动失效，无需批量删除）
    Redis 不可用时返回 "v1"
    """
    try:
        v = redis_client.get(f"cache:version:{prefix}")
        return v if v else "v1"
    except Exception:
        return "v1"


def cacheable(prefix: str, ttl: int, key_args: list, use_lock: bool = False):
    """
    AI接口缓存装饰器（增强版：L1本地 + L2 Redis + 标准化 + 版本 + 抖动 + 锁）

    Args:
        prefix:    缓存key前缀，如 "ai:llm"
        ttl:       过期秒数（写入时加随机抖动）
        key_args:  参与计算key的参数名列表（bytes型自动取md5，list/dict走json排序）
        use_lock:  是否启用互斥锁防击穿（热点key场景，默认 False）

    用法:
        @cacheable("ai:llm", ttl=3600, key_args=["prompt", "model"])
        async def llm_chat(self, prompt, model=None, ...): ...
    """
    def decorator(func):
        @wraps(func)
        async def wrapper(*args, **kwargs):
            # 1. 构造缓存key：unwrap穿透 @retry 等外层装饰器，拿到原函数形参名
            raw = inspect.unwrap(func)
            bound = raw.__code__.co_varnames[:raw.__code__.co_argcount]
            arg_map = dict(zip(bound, args))
            arg_map.update(kwargs)

            # 2. Prompt 标准化（key_args 含 "prompt" 时）
            from common.cache_normalize import normalize_prompt
            key_parts = []
            for an in key_args:
                val = arg_map.get(an)
                if val is None:
                    val = ""
                # prompt 参数走标准化（默认 rag 场景）
                if an == "prompt" and isinstance(val, str):
                    val = normalize_prompt(val, "rag")
                key_parts.append(_md5(val) if isinstance(val, (bytes, bytearray)) else _stable_str(val))

            # 3. 版本号注入
            version = _get_version(prefix)
            cache_key = f"{prefix}:{version}:{':'.join(key_parts)}"
            l1_key = f"l1:{cache_key}"

            # 4. L1 本地缓存
            if _l1_cache is not None:
                try:
                    cached = _l1_cache.get(l1_key)
                    if cached is not None:
                        logger.debug(f"L1缓存命中: {prefix}")
                        try:
                            from common.cache_metrics import record_hit
                            record_hit("l1", prefix)
                        except Exception:
                            pass
                        return cached
                except Exception as e:
                    logger.warning(f"L1缓存读取失败，跳过: {e}")

            # 5. L2 Redis 精确缓存（同步redis丢线程池，避免阻塞事件循环）
            try:
                cached = await asyncio.to_thread(redis_client.get_json, cache_key)
                if cached is not None:
                    logger.debug(f"L2缓存命中: {prefix}")
                    # 回写 L1
                    if _l1_cache is not None:
                        try:
                            _l1_cache[l1_key] = cached
                        except Exception:
                            pass
                    try:
                        from common.cache_metrics import record_hit
                        record_hit("l2", prefix)
                    except Exception:
                        pass
                    return cached
            except Exception as e:
                logger.warning(f"L2缓存读取失败，跳过: {e}")

            # 6. 互斥锁防击穿（可选）
            lock_key = f"cache:lock:{cache_key}"
            acquired = False
            if use_lock:
                try:
                    acquired = await asyncio.to_thread(
                        redis_client.lock, lock_key, timeout=settings.cache_lock_timeout
                    )
                except Exception:
                    acquired = False  # Redis 不可用时跳过锁，直接回源

                if not acquired:
                    # 未抢到锁：短暂等待后重读缓存（其他线程可能已回写）
                    logger.debug(f"未抢到锁，等待重读: {prefix}")
                    await asyncio.sleep(0.05)
                    try:
                        cached = await asyncio.to_thread(redis_client.get_json, cache_key)
                        if cached is not None:
                            if _l1_cache is not None:
                                try:
                                    _l1_cache[l1_key] = cached
                                except Exception:
                                    pass
                            return cached
                    except Exception:
                        pass
                    # 重读仍无：放行回源（降级，不阻塞业务）

            try:
                from common.cache_metrics import record_miss
                record_miss(prefix)
            except Exception:
                pass

            # 7. 未命中：执行原方法
            result = await func(*args, **kwargs)

            # 8. 写缓存（bytes结果跳过，只缓存可JSON序列化的）
            try:
                if not isinstance(result, (bytes, bytearray)):
                    jittered_ttl = _jitter_ttl(ttl)
                    # 写 L2
                    await asyncio.to_thread(redis_client.set_json, cache_key, result, jittered_ttl)
                    # 写 L1
                    if _l1_cache is not None:
                        try:
                            _l1_cache[l1_key] = result
                        except Exception:
                            pass
                    logger.debug(f"缓存写入: {prefix} (TTL={jittered_ttl}s)")
            except Exception as e:
                logger.warning(f"缓存写入失败，跳过: {e}")
            finally:
                # 释放锁
                if use_lock and acquired:
                    try:
                        await asyncio.to_thread(redis_client.unlock, lock_key)
                    except Exception:
                        pass

            return result
        return wrapper
    return decorator
