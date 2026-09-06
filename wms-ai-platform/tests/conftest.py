"""
共享测试配置（tests/ 目录自动加载）
"""
import asyncio

import pytest


@pytest.fixture(autouse=True)
def _current_event_loop():
    """为每个测试准备一个独立的"当前事件循环"。

    原因：tests/test_aiops_fixscripts.py 中的 asyncio.run() 在 finally 里
    执行 events.set_event_loop(None)，会把当前线程的事件循环清空；后续测试
    文件里再调用 asyncio.get_event_loop() 就会抛
    "RuntimeError: There is no current event loop in thread 'MainThread'"。

    这里在测试前显式 set_event_loop、测试后关闭并还原，隔离该副作用。
    """
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    yield loop
    loop.close()
    asyncio.set_event_loop(None)
