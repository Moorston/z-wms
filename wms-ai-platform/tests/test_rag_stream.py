"""
测试：R5 — 流式输出接通
覆盖 llm_chat_messages_stream + LLMGenerator.generate_stream + RAGService.chat_stream + /rag/chat/stream 端点
"""
import pytest
import os, sys, json
from unittest.mock import patch, MagicMock, AsyncMock

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


# ========== R5.1: llm_chat_messages_stream ==========

class TestLLMChatMessagesStream:
    """ai_client.llm_chat_messages_stream 测试"""

    @pytest.mark.asyncio
    async def test_stream_parses_sse_lines(self):
        """llm_chat_messages_stream 正确解析 SSE data 行并 yield 内容"""
        from common.ai_client import ai_client

        # Mock httpx async stream 响应
        lines = ["data: hello", "data: world", "data: [DONE]"]

        class MockResponse:
            def __init__(self):
                self._lines = iter(lines)
            def __aiter__(self):
                return self
            async def __anext__(self):
                try:
                    return next(self._lines)
                except StopIteration:
                    raise StopAsyncIteration
            async def aiter_lines(self):
                for line in lines:
                    yield line

        mock_stream_ctx = MagicMock()
        mock_stream_ctx.__aenter__ = AsyncMock(return_value=MockResponse())
        mock_stream_ctx.__aexit__ = AsyncMock(return_value=False)

        with patch.object(ai_client._http, "stream", return_value=mock_stream_ctx) as mock_stream:
            mock_stream.return_value.__aenter__ = AsyncMock(return_value=MockResponse())
            mock_stream.return_value.__aexit__ = AsyncMock(return_value=False)

            chunks = []
            async for chunk in ai_client.llm_chat_messages_stream(
                messages=[{"role": "user", "content": "hi"}],
            ):
                chunks.append(chunk)

        # 验证只 yield "data: " 前缀后的内容
        assert chunks == ["hello", "world", "[DONE]"]


# ========== R5.1b: LLMGenerator.generate_stream ==========

class TestGeneratorStream:
    """LLMGenerator.generate_stream 测试"""

    @pytest.mark.asyncio
    async def test_generate_stream_uses_messages_format(self):
        """generate_stream 使用 _build_messages 构建双消息格式"""
        from modules.rag.generator.llm_generator import LLMGenerator

        gen = LLMGenerator()
        docs = [{"title": "手册", "content": "登录系统", "relevance_score": 0.8}]

        with patch("modules.rag.generator.llm_generator.ai_client") as mock_ai:
            async def fake_stream(messages, temperature=None):
                yield "chunk1"
                yield "chunk2"
            mock_ai.llm_chat_messages_stream = MagicMock(side_effect=fake_stream)

            chunks = []
            async for chunk in gen.generate_stream("怎么登录？", docs):
                chunks.append(chunk)

        assert chunks == ["chunk1", "chunk2"]

    @pytest.mark.asyncio
    async def test_generate_stream_empty_docs(self):
        """空文档时 generate_stream 正常工作"""
        from modules.rag.generator.llm_generator import LLMGenerator

        gen = LLMGenerator()
        with patch("modules.rag.generator.llm_generator.ai_client") as mock_ai:
            async def fake_stream(messages, temperature=None):
                yield "暂无相关信息"
            mock_ai.llm_chat_messages_stream = MagicMock(side_effect=fake_stream)

            chunks = []
            async for chunk in gen.generate_stream("不相关问题", []):
                chunks.append(chunk)

        assert chunks == ["暂无相关信息"]


# ========== R5.2: RAGService.chat_stream ==========

class TestChatStream:
    """RAGService.chat_stream SSE 格式测试"""

    @pytest.mark.asyncio
    async def test_chat_stream_sse_format(self):
        """chat_stream 输出 SSE 格式：meta → chunk* → done → [DONE]"""
        from modules.rag.main import RAGService, ChatRequest

        svc = RAGService()
        req = ChatRequest(question="怎么操作？", top_k=3)

        mock_docs = [{"title": "手册", "content": "内容", "relevance_score": 0.8}]

        with patch.object(svc.session, "create_session", return_value="sess-1"), \
             patch.object(svc.session, "add_message", return_value=42), \
             patch.object(svc.session, "get_context", return_value=[]), \
             patch.object(svc, "_rewrite_question", AsyncMock(return_value="怎么操作")), \
             patch.object(svc, "_expand_query", AsyncMock(return_value=[])), \
             patch.object(svc.retriever, "retrieve_multi", AsyncMock(return_value=mock_docs)), \
             patch.object(svc.reranker, "rerank", AsyncMock(return_value=mock_docs)), \
             patch.object(svc, "_get_knowledge_version", return_value=1), \
             patch.object(svc, "_log_retrieval", AsyncMock()), \
             patch("modules.rag.main.redis_client") as mock_redis:
            mock_redis.get_json.return_value = None
            mock_redis.set_json.return_value = True

            with patch.object(svc.generator, "generate_stream") as mock_gen:
                async def fake_stream(question, docs, history=None):
                    yield "你好"
                    yield "，这是答案"
                mock_gen.side_effect = fake_stream

                lines = []
                async for line in svc.chat_stream(req):
                    lines.append(line)

        # 验证 SSE 输出格式
        assert len(lines) >= 3  # meta + chunks + done + [DONE]

        # 第1行: meta
        assert lines[0].startswith("data: ")
        meta = json.loads(lines[0][6:].strip())
        assert meta["type"] == "meta"
        assert meta["session_id"] == "sess-1"
        assert len(meta["sources"]) == 1

        # 中间行: chunk
        chunk_lines = [l for l in lines if '"type": "chunk"' in l or '"type":"chunk"' in l]
        assert len(chunk_lines) == 2

        # 解析 chunk 内容
        chunk_data = [json.loads(l[6:].strip()) for l in chunk_lines]
        assert chunk_data[0]["type"] == "chunk"
        assert chunk_data[0]["content"] == "你好"

        # 倒数第2行: done
        done_lines = [l for l in lines if '"type": "done"' in l or '"type":"done"' in l]
        assert len(done_lines) == 1
        done = json.loads(done_lines[0][6:].strip())
        assert done["type"] == "done"
        assert "answer" in done
        assert done["answer"] == "你好，这是答案"
        assert "confidence" in done
        assert "message_id" in done

        # 最后一行: [DONE]
        assert lines[-1] == "data: [DONE]\n\n"


# ========== R5.3: /rag/chat/stream 端点 ==========

class TestStreamEndpoint:
    """/rag/chat/stream 端点测试"""

    def test_endpoint_exists(self):
        """端点存在于 FastAPI app 路由中"""
        from modules.rag.main import app

        routes = [r.path for r in app.routes]
        assert "/rag/chat/stream" in routes

    @pytest.mark.asyncio
    async def test_stream_endpoint_returns_sse(self):
        """端点返回 text/event-stream 媒体类型"""
        from modules.rag.main import RAGService, ChatRequest, app
        from httpx import AsyncClient, ASGITransport

        svc = RAGService()
        mock_docs = [{"title": "手册", "content": "内容", "relevance_score": 0.8}]

        with patch.object(svc.session, "create_session", return_value="sess-1"), \
             patch.object(svc.session, "add_message", return_value=42), \
             patch.object(svc.session, "get_context", return_value=[]), \
             patch.object(svc, "_rewrite_question", AsyncMock(return_value="怎么操作")), \
             patch.object(svc, "_expand_query", AsyncMock(return_value=[])), \
             patch.object(svc.retriever, "retrieve_multi", AsyncMock(return_value=mock_docs)), \
             patch.object(svc.reranker, "rerank", AsyncMock(return_value=mock_docs)), \
             patch.object(svc, "_get_knowledge_version", return_value=1), \
             patch.object(svc, "_log_retrieval", AsyncMock()), \
             patch("modules.rag.main.redis_client") as mock_redis:
            mock_redis.get_json.return_value = None
            mock_redis.set_json.return_value = True

            # 替换端点函数中的 rag_service 实例
            from modules.rag.main import rag_service
            original_svc = rag_service

            async def fake_stream(req):
                yield "data: {\"type\": \"meta\", \"session_id\": \"sess-1\"}\n\n"
                yield "data: {\"type\": \"chunk\", \"content\": \"答案\"}\n\n"
                yield "data: {\"type\": \"done\", \"answer\": \"答案\", \"confidence\": 0.8}\n\n"
                yield "data: [DONE]\n\n"

            patcher = patch.object(rag_service, "chat_stream", side_effect=fake_stream)
            patcher.start()

            try:
                transport = ASGITransport(app=app)
                async with AsyncClient(transport=transport, base_url="http://test") as client:
                    resp = await client.post("/rag/chat/stream", json={
                        "question": "怎么操作？", "top_k": 3,
                    })
                assert resp.status_code == 200
                assert "text/event-stream" in resp.headers["content-type"]
                body = resp.text
                assert "data: " in body
                assert "[DONE]" in body
            finally:
                patcher.stop()
