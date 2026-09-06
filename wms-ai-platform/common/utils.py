"""
WMS AI Platform - 通用工具类
"""
import time
import uuid
import hashlib
from datetime import datetime, timedelta
from typing import Any, Dict
from loguru import logger
from functools import wraps


def gen_id() -> str:
    """生成唯一ID（雪花算法简化版）"""
    return str(uuid.uuid4())


def gen_trace_id() -> str:
    """生成链路追踪ID"""
    return uuid.uuid4().hex[:16]


def now_str(fmt: str = "%Y-%m-%d %H:%M:%S") -> str:
    """当前时间字符串"""
    return datetime.now().strftime(fmt)


def date_str(fmt: str = "%Y-%m-%d") -> str:
    """当前日期字符串"""
    return datetime.now().strftime(fmt)


def add_days(days: int, base: datetime = None) -> datetime:
    """日期加减"""
    base = base or datetime.now()
    return base + timedelta(days=days)


def md5(text: str) -> str:
    """MD5哈希"""
    return hashlib.md5(text.encode("utf-8")).hexdigest()


def timing(func):
    """函数耗时装饰器"""
    @wraps(func)
    async def async_wrapper(*args, **kwargs):
        start = time.time()
        result = await func(*args, **kwargs)
        cost = (time.time() - start) * 1000
        logger.info(f"{func.__name__} 耗时: {cost:.2f}ms")
        return result

    @wraps(func)
    def sync_wrapper(*args, **kwargs):
        start = time.time()
        result = func(*args, **kwargs)
        cost = (time.time() - start) * 1000
        logger.info(f"{func.__name__} 耗时: {cost:.2f}ms")
        return result

    import asyncio
    if asyncio.iscoroutinefunction(func):
        return async_wrapper
    return sync_wrapper


def safe_get(data: Dict, key: str, default: Any = None) -> Any:
    """安全获取字典值"""
    if not data:
        return default
    keys = key.split(".")
    val = data
    for k in keys:
        if isinstance(val, dict) and k in val:
            val = val[k]
        else:
            return default
    return val


def chunk_list(lst: list, size: int) -> list:
    """列表分块"""
    return [lst[i:i + size] for i in range(0, len(lst), size)]


class Result:
    """统一返回结果"""

    @staticmethod
    def success(data: Any = None, message: str = "success") -> Dict:
        return {"code": 0, "message": message, "data": data, "timestamp": int(time.time() * 1000)}

    @staticmethod
    def error(code: int = -1, message: str = "error", data: Any = None) -> Dict:
        return {"code": code, "message": message, "data": data, "timestamp": int(time.time() * 1000)}


class BizException(Exception):
    """业务异常"""

    def __init__(self, code: int, message: str):
        self.code = code
        self.message = message
        super().__init__(message)
