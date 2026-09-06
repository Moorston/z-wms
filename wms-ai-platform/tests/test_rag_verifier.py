"""
测试：R4 — LLM Self-Check 答案验证
覆盖 LLMVerifier + LLMGenerator.generate() 集成 + chat() 返回 verification
"""
import pytest
import os, sys
from types import SimpleNamespace
from unittest.mock import patch, MagicMock, AsyncMock

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


# ========== R4.1: LLMVerifier ==========

class TestLLMVerifier:
    """LLMVerifier.verify() 测试"""

    @pytest.mark.asyncio
    async def test_verify_valid(self):
        """有效答案验证：LLM 返回 valid=true，confidence_adjusted = retrieval*0.6 + score*0.4"""
        from modules.rag.verifier.llm_verifier import LLMVerifier

        verifier = LLMVerifier()
        # Mock ai_client.llm_extract_json 返回有效验证结果
        mock_result = {"valid": True, "issues": [], "score": 0.9}
        with patch.object(verifier, "_ai_client") as mock_ai:
            mock_ai.llm_extract_json = AsyncMock(return_value=mock_result)
            docs = [{"title": "操作手册", "content": "登录系统后选择波次"}]
            result = await verifier.verify("登录系统后选择波次", docs, 0.8)

        assert result["valid"] is True
        assert result["issues"] == []
        # 0.8 * 0.6 + 0.9 * 0.4 = 0.48 + 0.36 = 0.84
        assert result["confidence_adjusted"] == round(0.8 * 0.6 + 0.9 * 0.4, 4)
        assert result["confidence_adjusted"] == 0.84

    @pytest.mark.asyncio
    async def test_verify_invalid(self):
        """无效答案验证：LLM 返回 valid=false + issues"""
        from modules.rag.verifier.llm_verifier import LLMVerifier

        verifier = LLMVerifier()
        mock_result = {"valid": False, "issues": ["要点1无依据"], "score": 0.3}
        with patch.object(verifier, "_ai_client") as mock_ai:
            mock_ai.llm_extract_json = AsyncMock(return_value=mock_result)
            docs = [{"title": "手册", "content": "内容A"}]
            result = await verifier.verify("编造的内容", docs, 0.7)

        assert result["valid"] is False
        assert len(result["issues"]) == 1
        assert "要点1" in result["issues"][0]
        # 0.7 * 0.6 + 0.3 * 0.4 = 0.42 + 0.12 = 0.54
        assert result["confidence_adjusted"] == 0.54

    @pytest.mark.asyncio
    async def test_verify_degraded(self):
        """验证失败降级：LLM 抛异常，返回原始 retrieval_confidence"""
        from modules.rag.verifier.llm_verifier import LLMVerifier

        verifier = LLMVerifier()
        with patch.object(verifier, "_ai_client") as mock_ai:
            mock_ai.llm_extract_json = AsyncMock(side_effect=Exception("LLM 超时"))
            docs = [{"title": "手册", "content": "内容"}]
            result = await verifier.verify("回答", docs, 0.65)

        assert result["valid"] is True
        assert result["issues"] == []
        assert result["confidence_adjusted"] == 0.65

    @pytest.mark.asyncio
    async def test_verify_no_docs(self):
        """context_docs 为空时跳过验证，返回原始置信度"""
        from modules.rag.verifier.llm_verifier import LLMVerifier

        verifier = LLMVerifier()
        with patch.object(verifier, "_ai_client") as mock_ai:
            mock_ai.llm_extract_json = AsyncMock(return_value={"valid": True})
            result = await verifier.verify("回答", [], 0.5)

        assert result["valid"] is True
        assert result["confidence_adjusted"] == 0.5
        # 空文档不应调用 LLM
        mock_ai.llm_extract_json.assert_not_called()


# ========== R4.2: LLMGenerator.generate() 集成 ==========

class TestGeneratorIntegration:
    """LLMGenerator.generate() 集成验证测试"""

    @pytest.mark.asyncio
    async def test_generate_with_verification(self):
        """generate() 返回值包含 verification 字段"""
        from modules.rag.generator.llm_generator import LLMGenerator

        gen = LLMGenerator()
        docs = [{"title": "手册", "content": "登录后选择波次", "relevance_score": 0.8}]

        # Mock ai_client.llm_chat_messages 返回答案
        # Mock LLMVerifier 类（直接 mock verify 方法）
        with patch("modules.rag.generator.llm_generator.ai_client") as mock_ai, \
             patch("modules.rag.generator.llm_generator.LLMVerifier") as mock_verifier_cls:
            mock_ai.llm_chat_messages = AsyncMock(return_value="登录后选择波次")
            mock_instance = MagicMock()
            mock_instance.verify = AsyncMock(return_value={
                "valid": True, "issues": [], "confidence_adjusted": 0.84
            })
            mock_verifier_cls.return_value = mock_instance
            result = await gen.generate("怎么操作？", docs)

        assert "answer" in result
        assert "verification" in result
        assert result["verification"]["valid"] is True
        assert "confidence" in result
        assert "model" in result


# ========== R4.3: chat() 返回 verification ==========

class TestChatIntegration:
    """chat() 返回 verification 字段测试"""

    @pytest.mark.asyncio
    async def test_chat_returns_verification(self):
        """chat() 返回值包含 verification 字段"""
        from modules.rag.main import RAGService, ChatRequest

        svc = RAGService()
        req = ChatRequest(question="怎么操作？", top_k=3)

        # Mock 整条链路：会话、检索、rerank、生成
        with patch.object(svc.session, "create_session", return_value="sess-1"), \
             patch.object(svc.session, "add_message", return_value=42), \
             patch.object(svc.session, "get_context", return_value=[]), \
             patch.object(svc, "_rewrite_question", AsyncMock(return_value="怎么操作")), \
             patch.object(svc, "_expand_query", AsyncMock(return_value=[])), \
             patch.object(svc.retriever, "retrieve_multi", AsyncMock(
                 return_value=[{"title": "手册", "content": "内容", "relevance_score": 0.8}]
             )), \
             patch.object(svc.reranker, "rerank", AsyncMock(
                 return_value=[{"title": "手册", "content": "内容", "relevance_score": 0.8}]
             )), \
             patch.object(svc.generator, "generate", AsyncMock(
                 return_value={
                     "answer": "登录后操作",
                     "confidence": 0.84,
                     "model": "deepseek-v4-pro",
                     "latency_ms": 100,
                     "verification": {"valid": True, "issues": [],
                                      "confidence_adjusted": 0.84},
                 }
             )), \
             patch("modules.rag.main.redis_client") as mock_redis:
            mock_redis.get.return_value = None
            mock_redis.get_json.return_value = None
            mock_redis.incr.return_value = 1
            mock_redis.set_json.return_value = True

            result = await svc.chat(req)

        assert "verification" in result
        assert result["verification"]["valid"] is True
