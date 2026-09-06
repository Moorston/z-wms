"""
测试：RAG 缺陷修复（R1-R5）+ 新增能力覆盖
R1: delete_document 清理 Milvus 向量 + 缓存失效
R2: chat() 记录检索监控
R3: _insert_to_milvus 返回真实 auto-IDs（upsert）
R4: sources 含 doc_id/chunk_index（精确引用）
R5: 意图路由（问候/管理指令跳过 RAG 流水线）
"""
import pytest
import asyncio
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import inspect
import modules.rag.main as rag_mod
from modules.rag.main import RAGService, ChatRequest


# ========== R1: delete_document 清理 ==========

class TestDeleteDocument:
    """R1: 删除文档后清理 Milvus 向量 + 缓存版本 bump"""

    def test_delete_document_calls_knowledge_delete(self):
        """delete_document 调用 knowledge.delete_document"""
        service = RAGService()
        called = {"knowledge": False, "milvus": False, "cache": False}

        service.knowledge.delete_document = lambda doc_id: called.__setitem__("knowledge", True)

        # Mock Milvus collection
        class MockCollection:
            def delete(self, expr):
                called["milvus"] = True
            def flush(self):
                pass

        class MockVectorRetriever:
            def _get_collection(self):
                return MockCollection()

        service.retriever.vector_retriever = MockVectorRetriever()

        # Mock redis
        original_get = rag_mod.redis_client.get
        original_set = rag_mod.redis_client.set
        rag_mod.redis_client.get = lambda key: "v2"
        rag_mod.redis_client.set = lambda key, val: called.__setitem__("cache", True)

        try:
            service.delete_document("DOC001")
            assert called["knowledge"], "应调用 knowledge.delete_document"
            assert called["milvus"], "应调用 Milvus collection.delete"
            assert called["cache"], "应更新缓存版本"
        finally:
            rag_mod.redis_client.get = original_get
            rag_mod.redis_client.set = original_set

    def test_delete_document_milvus_failure_degrades(self):
        """Milvus 删除失败时降级跳过，不中断主流程"""
        service = RAGService()
        service.knowledge.delete_document = lambda doc_id: None

        class FailingCollection:
            def delete(self, expr):
                raise Exception("Milvus 不可用")
            def flush(self):
                pass

        class MockVectorRetriever:
            def _get_collection(self):
                return FailingCollection()

        service.retriever.vector_retriever = MockVectorRetriever()

        # redis mock
        original_get = rag_mod.redis_client.get
        original_set = rag_mod.redis_client.set
        rag_mod.redis_client.get = lambda key: "v1"
        rag_mod.redis_client.set = lambda key, val: None

        try:
            # 不应抛异常
            service.delete_document("DOC001")
        finally:
            rag_mod.redis_client.get = original_get
            rag_mod.redis_client.set = original_set

    def test_delete_document_collection_none_degrades(self):
        """Milvus collection 为 None 时降级跳过"""
        service = RAGService()
        service.knowledge.delete_document = lambda doc_id: None

        class MockVectorRetriever:
            def _get_collection(self):
                return None

        service.retriever.vector_retriever = MockVectorRetriever()

        original_get = rag_mod.redis_client.get
        original_set = rag_mod.redis_client.set
        rag_mod.redis_client.get = lambda key: "v1"
        rag_mod.redis_client.set = lambda key, val: None

        try:
            service.delete_document("DOC001")  # 不应抛异常
        finally:
            rag_mod.redis_client.get = original_get
            rag_mod.redis_client.set = original_set


# ========== R2: chat() 检索监控记录 ==========

class TestChatLogging:
    """R2: chat() 方法记录检索监控日志"""

    @pytest.mark.asyncio
    async def test_chat_logs_retrieval(self):
        """chat() 调用 _log_retrieval"""
        service = RAGService()
        logged = {"called": False}

        # Mock 关键依赖
        service.session.create_session = lambda **kw: "sess-001"
        service.session.add_message = lambda *a, **kw: 1
        service.session.get_context = lambda sid: []

        async def mock_rewrite(q, **kw):
            return q
        service._rewrite_question = mock_rewrite

        async def mock_expand(q):
            return []
        service._expand_query = mock_expand

        # Mock intent routing
        service._classify_intent = lambda q: "knowledge"

        # Mock retriever
        class MockRetriever:
            async def retrieve_multi(self, queries, top_k=20, warehouse=None):
                return [{"title": "T", "content": "C", "doc_id": "d1", "chunk_index": 0,
                         "relevance_score": 0.9, "warehouse": "WH01"}]
        service.retriever = MockRetriever()

        # Mock reranker
        class MockReranker:
            async def rerank(self, q, docs, top_n=5):
                return docs[:top_n]
        service.reranker = MockReranker()

        # Mock generator
        class MockGenerator:
            async def generate(self, q, docs, history):
                return {"answer": "答案", "confidence": 0.9, "model": "deepseek-v4-pro",
                        "verification": {"passed": True}}
        service.generator = MockGenerator()

        # Mock redis
        original_get = rag_mod.redis_client.get
        original_get_json = rag_mod.redis_client.get_json
        original_set = rag_mod.redis_client.set
        original_set_json = rag_mod.redis_client.set_json
        rag_mod.redis_client.get = lambda key: "v1"
        rag_mod.redis_client.get_json = lambda key: None
        rag_mod.redis_client.set = lambda *a, **kw: None
        rag_mod.redis_client.set_json = lambda *a, **kw: None

        # Mock _log_retrieval
        async def mock_log(req, rq, docs, latency, sid=None):
            logged["called"] = True
            logged["docs_len"] = len(docs)

        service._log_retrieval = mock_log

        try:
            req = ChatRequest(question="测试问题", user_id="u1")
            await service.chat(req)
            assert logged["called"], "chat() 应调用 _log_retrieval"
        finally:
            rag_mod.redis_client.get = original_get
            rag_mod.redis_client.get_json = original_get_json
            rag_mod.redis_client.set = original_set
            rag_mod.redis_client.set_json = original_set_json


# ========== R3: _insert_to_milvus 使用 upsert ==========

class TestMilvusInsert:
    """R3: _insert_to_milvus 使用 upsert 并返回真实 primary_keys"""

    @pytest.mark.asyncio
    async def test_insert_uses_upsert(self):
        """_insert_to_milvus 调用 collection.upsert 而非 insert"""
        service = RAGService()
        call_log = {"method": None, "ids": None}

        class MockUpsertResult:
            primary_keys = [1001, 1002, 1003]

        class MockCollection:
            def upsert(self, data):
                call_log["method"] = "upsert"
                return MockUpsertResult()
            def insert(self, data):
                call_log["method"] = "insert"
                return MockUpsertResult()
            def flush(self):
                pass

        class MockVectorRetriever:
            def _get_collection(self):
                return MockCollection()

        service.retriever.vector_retriever = MockVectorRetriever()

        # Mock embed
        async def mock_embed(texts):
            return [[0.1] * 8, [0.2] * 8, [0.3] * 8]
        rag_mod.ai_client.embed = mock_embed

        try:
            from modules.rag.chunker.base import Chunk
            chunks = [Chunk(content="内容1"), Chunk(content="内容2"), Chunk(content="内容3")]
            ids = await service._insert_to_milvus("DOC001", chunks, "WH01")
            assert call_log["method"] == "upsert", f"应调用 upsert: {call_log['method']}"
            assert ids == ["1001", "1002", "1003"], f"应返回真实 Milvus IDs: {ids}"
        finally:
            pass  # ai_client.embed mock is harmless


# ========== R4: sources 含 doc_id/chunk_index ==========

class TestCitationGrounding:
    """R4: chat/chat_stream 响应 sources 含 doc_id/chunk_index"""

    @pytest.mark.asyncio
    async def test_chat_sources_include_doc_id(self):
        """chat() 返回的 sources 包含 doc_id 和 chunk_index"""
        service = RAGService()

        service.session.create_session = lambda **kw: "sess-001"
        service.session.add_message = lambda *a, **kw: 1
        service.session.get_context = lambda sid: []

        # Mock intent routing — force knowledge intent
        service._classify_intent = lambda q: "knowledge"

        async def mock_rewrite(q, **kw):
            return q
        service._rewrite_question = mock_rewrite

        async def mock_expand(q):
            return []
        service._expand_query = mock_expand

        class MockRetriever:
            async def retrieve_multi(self, queries, top_k=20, warehouse=None):
                return [{"title": "文档A", "content": "内容", "doc_id": "DOC99",
                         "chunk_index": 3, "relevance_score": 0.85, "warehouse": "WH01"}]
        service.retriever = MockRetriever()

        class MockReranker:
            async def rerank(self, q, docs, top_n=5):
                return docs[:top_n]
        service.reranker = MockReranker()

        class MockGenerator:
            async def generate(self, q, docs, history):
                return {"answer": "答案", "confidence": 0.85, "model": "deepseek-v4-pro",
                        "verification": {"passed": True}}
        service.generator = MockGenerator()

        original_get = rag_mod.redis_client.get
        original_get_json = rag_mod.redis_client.get_json
        original_set = rag_mod.redis_client.set
        original_set_json = rag_mod.redis_client.set_json
        rag_mod.redis_client.get = lambda key: "v1"
        rag_mod.redis_client.get_json = lambda key: None
        rag_mod.redis_client.set = lambda *a, **kw: None
        rag_mod.redis_client.set_json = lambda *a, **kw: None

        async def mock_log(*a, **kw):
            pass
        service._log_retrieval = mock_log

        try:
            req = ChatRequest(question="波次拣货流程", user_id="u1")
            result = await service.chat(req)
            assert len(result["sources"]) > 0, "应返回 sources"
            src = result["sources"][0]
            assert "doc_id" in src, f"sources 应包含 doc_id: {src}"
            assert "chunk_index" in src, f"sources 应包含 chunk_index: {src}"
            assert src["doc_id"] == "DOC99"
            assert src["chunk_index"] == 3
        finally:
            rag_mod.redis_client.get = original_get
            rag_mod.redis_client.get_json = original_get_json
            rag_mod.redis_client.set = original_set
            rag_mod.redis_client.set_json = original_set_json

    @pytest.mark.asyncio
    async def test_chat_stream_sources_include_doc_id(self):
        """chat_stream() SSE meta 消息 sources 包含 doc_id/chunk_index"""
        service = RAGService()

        service.session.create_session = lambda **kw: "sess-002"
        service.session.add_message = lambda *a, **kw: 2
        service.session.get_context = lambda sid: []

        service._classify_intent = lambda q: "knowledge"

        async def mock_rewrite(q, **kw):
            return q
        service._rewrite_question = mock_rewrite

        async def mock_expand(q):
            return []
        service._expand_query = mock_expand

        class MockRetriever:
            async def retrieve_multi(self, queries, top_k=20, warehouse=None):
                return [{"title": "文档B", "content": "内容", "doc_id": "DOC88",
                         "chunk_index": 5, "relevance_score": 0.9, "warehouse": "WH02"}]
        service.retriever = MockRetriever()

        class MockReranker:
            async def rerank(self, q, docs, top_n=5):
                return docs[:top_n]
        service.reranker = MockReranker()

        class MockGenerator:
            async def generate_stream(self, q, docs, history):
                yield "答案片段"
            def _estimate_confidence(self, docs):
                return 0.85

        service.generator = MockGenerator()

        original_get = rag_mod.redis_client.get
        original_get_json = rag_mod.redis_client.get_json
        original_set = rag_mod.redis_client.set
        original_set_json = rag_mod.redis_client.set_json
        rag_mod.redis_client.get = lambda key: "v1"
        rag_mod.redis_client.get_json = lambda key: None
        rag_mod.redis_client.set = lambda *a, **kw: None
        rag_mod.redis_client.set_json = lambda *a, **kw: None

        async def mock_log(*a, **kw):
            pass
        service._log_retrieval = mock_log

        try:
            req = ChatRequest(question="波次拣货", user_id="u1")
            chunks = []
            async for chunk in service.chat_stream(req):
                chunks.append(chunk)
            # 找 meta 消息
            meta_chunk = None
            for c in chunks:
                if '"type":"meta"' in c or '"type": "meta"' in c:
                    meta_chunk = c
                    break
            assert meta_chunk is not None, f"应包含 meta SSE 消息: {chunks}"
            assert "DOC88" in meta_chunk, f"meta sources 应包含 doc_id=DOC88: {meta_chunk}"
        finally:
            rag_mod.redis_client.get = original_get
            rag_mod.redis_client.get_json = original_get_json
            rag_mod.redis_client.set = original_set
            rag_mod.redis_client.set_json = original_set_json


# ========== R5: 意图路由 ==========

class TestIntentRouting:
    """R5: 简单问候/管理指令跳过 RAG 流水线"""

    def test_classify_greeting(self):
        """问候语分类为 greeting"""
        service = RAGService()
        assert service._classify_intent("你好") == "greeting"
        assert service._classify_intent("Hello") == "greeting"
        assert service._classify_intent("你好，请问波次拣货怎么操作？") == "knowledge", \
            "含具体问题的问候不应被截断"

    def test_classify_admin(self):
        """管理指令分类为 admin"""
        service = RAGService()
        assert service._classify_intent("列出文档") == "admin"
        assert service._classify_intent("查看知识库") == "admin"

    def test_classify_knowledge(self):
        """知识库查询分类为 knowledge"""
        service = RAGService()
        assert service._classify_intent("波次拣货怎么操作？") == "knowledge"
        assert service._classify_intent("库存盘点流程是什么？") == "knowledge"
        assert service._classify_intent("SKU入库流程") == "knowledge"

    @pytest.mark.asyncio
    async def test_chat_greeting_skips_rag(self):
        """chat() 问候语直接回复，不走 RAG 流水线"""
        service = RAGService()

        service.session.create_session = lambda **kw: "sess-greet"
        service.session.add_message = lambda *a, **kw: 1

        # RAG 相关方法不应被调用
        def should_not_be_called(*a, **kw):
            raise AssertionError("问候语不应触发 RAG 流水线")

        service.retriever = type("X", (), {"retrieve_multi": should_not_be_called})()
        service.reranker = type("X", (), {"rerank": should_not_be_called})()
        service.generator = type("X", (), {"generate": should_not_be_called})()

        req = ChatRequest(question="你好", user_id="u1")
        result = await service.chat(req)
        assert "智能助手" in result["answer"]
        assert result["intent"] == "greeting"
        assert result["sources"] == []
        assert result["confidence"] == 1.0

    @pytest.mark.asyncio
    async def test_chat_stream_greeting_skips_rag(self):
        """chat_stream() 问候语直接回复，不走 RAG 流水线"""
        service = RAGService()

        service.session.create_session = lambda **kw: "sess-greet2"
        service.session.add_message = lambda *a, **kw: 1

        def should_not_be_called(*a, **kw):
            raise AssertionError("问候语不应触发 RAG 流水线")

        service.retriever = type("X", (), {"retrieve_multi": should_not_be_called})()
        service.reranker = type("X", (), {"rerank": should_not_be_called})()
        service.generator = type("X", (), {"generate_stream": should_not_be_called})()

        req = ChatRequest(question="Hello", user_id="u1")
        chunks = []
        async for chunk in service.chat_stream(req):
            chunks.append(chunk)

        all_text = "".join(chunks)
        assert "智能助手" in all_text
        assert "[DONE]" in all_text
