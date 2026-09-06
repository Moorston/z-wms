"""
测试：RAG V2 — 生成 Prompt 升级 + 生成质量评估
覆盖 R3（_build_messages + llm_chat_messages）和 R6（RAGEvaluator）
"""
import pytest
import json
import os, sys
from types import SimpleNamespace
from unittest.mock import patch, MagicMock, AsyncMock

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from modules.rag.generator.llm_generator import LLMGenerator


# ========== R3: 生成 Prompt 升级 ==========

class TestLLMChatMessages:
    """ai_client.llm_chat_messages() 多消息格式测试"""

    @pytest.mark.asyncio
    async def test_messages_format(self):
        """llm_chat_messages 接受 messages 列表并传递给 model_service"""
        captured_payload = []

        class MockResp:
            def raise_for_status(self):
                pass
            def json(self):
                return {"content": "这是回答"}

        class MockTransport:
            async def post(self, url, json=None):
                captured_payload.append(json)
                return MockResp()

        # 模拟缓存层：get 返回 None（未命中），put 为 no-op，避免真实 Redis 连接
        fake_cache_layer = MagicMock()
        fake_cache_layer.get = AsyncMock(return_value=None)
        fake_cache_layer.put = AsyncMock(return_value=None)

        messages = [
            {"role": "system", "content": "你是 WMS 助手"},
            {"role": "user", "content": "波次拣货怎么操作？"},
        ]

        # 保持 patch 上下文活跃，确保 llm_chat_messages 调用期间读取的是 mock cache_layer
        # llm_chat_messages 内部是延迟导入 `from common.cache_layer import llm_cache_layer`，
        # 只要 sys.modules["common.cache_layer"] 被替换，函数运行时就会拿到 mock
        with patch.dict("sys.modules", {
            "common.cache_layer": SimpleNamespace(llm_cache_layer=fake_cache_layer),
        }):
            from common.ai_client import AIClient
            client = AIClient()
            client._http = MockTransport()

            result = await client.llm_chat_messages(
                messages, task_type="rag", temperature=0.0,
            )

        assert result == "这是回答"
        # payload 的 messages 字段应直接传递
        assert captured_payload[0]["messages"] == messages
        assert "model" in captured_payload[0]


class TestBuildMessages:
    """LLMGenerator._build_messages() 双消息格式测试"""

    def test_system_user_format(self):
        """_build_messages 返回 system+user 双消息列表"""
        gen = LLMGenerator()
        docs = [{"title": "波次拣货", "content": "登录系统后选择波次"}]
        messages = gen._build_messages("怎么操作？", docs)
        assert len(messages) == 2
        assert messages[0]["role"] == "system"
        assert messages[1]["role"] == "user"
        # system 消息应包含角色定位
        assert "WMS" in messages[0]["content"] or "仓储" in messages[0]["content"]
        # user 消息应包含参考资料和用户问题
        assert "参考资料" in messages[1]["content"]
        assert "怎么操作" in messages[1]["content"]

    def test_chunk_type_style_hint(self):
        """根据 chunk_type 动态生成 style_hint"""
        gen = LLMGenerator()

        # table 类型 → 表格呈现指令
        table_docs = [{"title": "库存表", "content": "SKU 数量", "chunk_type": "table"}]
        msgs = gen._build_messages("库存", table_docs)
        assert "表格" in msgs[0]["content"]

        # heading 类型 → 标题结构指令
        heading_docs = [{"title": "流程", "content": "步骤", "chunk_type": "heading"}]
        msgs = gen._build_messages("流程", heading_docs)
        assert "标题" in msgs[0]["content"]

        # text 类型 → 默认分点说明
        text_docs = [{"title": "说明", "content": "内容", "chunk_type": "text"}]
        msgs = gen._build_messages("说明", text_docs)
        assert "分点" in msgs[0]["content"]

    def test_history_truncation(self):
        """历史消息超过 512 字符时截断并标记"""
        gen = LLMGenerator()
        docs = [{"title": "文档", "content": "内容"}]
        long_content = "x" * 600
        history = [
            {"role": "user", "content": long_content},
            {"role": "assistant", "content": "回答"},
            {"role": "user", "content": "新问题"},
        ]
        messages = gen._build_messages("新问题", docs, history=history)
        user_msg = messages[1]["content"]
        # 截断标记应出现
        assert "截断" in user_msg
        # 原始 600 字符不应完整出现
        assert long_content not in user_msg
