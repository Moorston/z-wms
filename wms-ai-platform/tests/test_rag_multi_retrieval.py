"""
测试：RAG V2 — 多路召回 + Metadata 过滤
覆盖 R1（多路召回）和 R2（warehouse 过滤穿透）
"""
import pytest
import asyncio
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from modules.rag.retriever.fusion import HybridRetriever
from modules.rag.retriever.vector import VectorRetriever
from modules.rag.retriever.keyword import KeywordRetriever


# ========== R2: Metadata 过滤 ==========

class TestMetadataFilter:
    """warehouse 过滤穿透测试"""

    def test_vector_retrieve_accepts_warehouse(self):
        """VectorRetriever.retrieve 接受 warehouse 参数"""
        vr = VectorRetriever()
        # mock collection 为 None → 返回空，但签名必须接受 warehouse
        vr._collection = None  # 强制 mock 模式
        import inspect
        sig = inspect.signature(vr.retrieve)
        assert "warehouse" in sig.parameters, "retrieve 必须接受 warehouse 参数"

    @pytest.mark.asyncio
    async def test_vector_warehouse_filter_mock(self):
        """warehouse 指定时 filter_expr 正确生成（mock 模式下不报错）"""
        vr = VectorRetriever()
        vr._collection = None  # mock 模式
        # 不指定 warehouse
        docs = await vr.retrieve("测试", top_k=5)
        assert docs == []
        # 指定 warehouse
        docs = await vr.retrieve("测试", top_k=5, warehouse="WH01")
        assert docs == []

    def test_keyword_retrieve_accepts_warehouse(self):
        """KeywordRetriever.retrieve 接受 warehouse 参数"""
        kr = KeywordRetriever()
        import inspect
        sig = inspect.signature(kr.retrieve)
        assert "warehouse" in sig.parameters, "retrieve 必须接受 warehouse 参数"

    @pytest.mark.asyncio
    async def test_keyword_warehouse_filter_sql(self):
        """warehouse 指定时 SQL 包含 warehouse 条件"""
        kr = KeywordRetriever()
        # mock mysql_client 捕获 SQL
        captured_sql = []
        captured_params = []

        class MockMysqlClient:
            def query(self, sql, params):
                captured_sql.append(sql)
                captured_params.append(params)
                return []

        import modules.rag.retriever.keyword as kw_mod
        original = kw_mod.mysql_client
        kw_mod.mysql_client = MockMysqlClient()
        try:
            # 指定 warehouse
            await kr.retrieve("波次拣货", top_k=5, warehouse="WH01")
            assert any("warehouse" in sql.lower() for sql in captured_sql), \
                f"SQL 应包含 warehouse 条件: {captured_sql}"
            assert any("WH01" in str(p) for p in captured_params), \
                f"参数应包含 WH01: {captured_params}"

            # 不指定 warehouse
            captured_sql.clear()
            captured_params.clear()
            await kr.retrieve("波次拣货", top_k=5)
            # 不指定时 SQL 不应包含 warehouse 条件
            assert not any("warehouse" in sql.lower() for sql in captured_sql), \
                f"无 warehouse 时 SQL 不应包含条件: {captured_sql}"
        finally:
            kw_mod.mysql_client = original

    def test_fusion_retrieve_accepts_warehouse(self):
        """HybridRetriever.retrieve 接受 warehouse 参数"""
        hr = HybridRetriever()
        import inspect
        sig = inspect.signature(hr.retrieve)
        assert "warehouse" in sig.parameters, "retrieve 必须接受 warehouse 参数"

    @pytest.mark.asyncio
    async def test_fusion_passes_warehouse(self):
        """HybridRetriever.retrieve 将 warehouse 传递给子检索器"""
        passed_warehouse = {"vector": None, "keyword": None}

        class MockVectorRetriever:
            async def retrieve(self, query, top_k=20, warehouse=None):
                passed_warehouse["vector"] = warehouse
                return [{"content": "vec", "doc_id": "d1", "chunk_index": 0, "score": 0.9}]

        class MockKeywordRetriever:
            async def retrieve(self, query, top_k=20, warehouse=None, documents=None):
                passed_warehouse["keyword"] = warehouse
                return [{"content": "kw", "doc_id": "d2", "chunk_index": 0, "score": 0.8}]

        hr = HybridRetriever(
            vector_retriever=MockVectorRetriever(),
            keyword_retriever=MockKeywordRetriever(),
        )
        await hr.retrieve("测试", top_k=5, warehouse="WH01")
        assert passed_warehouse["vector"] == "WH01", \
            f"VectorRetriever 应收到 warehouse=WH01: {passed_warehouse}"
        assert passed_warehouse["keyword"] == "WH01", \
            f"KeywordRetriever 应收到 warehouse=WH01: {passed_warehouse}"


class TestCacheKeyWithWarehouse:
    """L4 缓存键包含 warehouse 维度"""

    def test_cache_key_contains_warehouse(self):
        """缓存键包含 warehouse"""
        # 验证 main.py 中的缓存键格式
        from common.utils import md5
        kv = "v1"
        warehouse = "WH01"
        refined_q = "波次拣货"
        key = f"rag:retrieval:{kv}:{warehouse or ''}:{md5(refined_q)}"
        assert "WH01" in key, f"缓存键应包含 warehouse: {key}"

    def test_cache_key_different_warehouse(self):
        """不同 warehouse 产生不同缓存键"""
        from common.utils import md5
        kv = "v1"
        refined_q = "波次拣货"
        key_wh01 = f"rag:retrieval:{kv}:{'WH01'}:{md5(refined_q)}"
        key_wh02 = f"rag:retrieval:{kv}:{'WH02'}:{md5(refined_q)}"
        assert key_wh01 != key_wh02, "不同仓库应产生不同缓存键"

    def test_cache_key_no_warehouse(self):
        """不指定 warehouse 时缓存键不含 warehouse 后缀（空字符串）"""
        from common.utils import md5
        kv = "v1"
        refined_q = "波次拣货"
        key = f"rag:retrieval:{kv}:{''}:{md5(refined_q)}"
        # 空字符串后缀，不污染缓存
        assert "::" in key  # kv::md5 格式


# ========== R1: 多路召回 ==========

class TestQueryExpansion:
    """查询扩展测试"""

    @pytest.mark.asyncio
    async def test_expand_returns_2_variants(self):
        """_expand_query 返回 2 个变体"""
        from modules.rag.main import RAGService
        service = RAGService()

        # Mock ai_client.llm_chat
        import modules.rag.main as rag_mod
        original_llm_chat = rag_mod.ai_client.llm_chat

        async def mock_llm_chat(prompt, **kwargs):
            return "变体1：波次拣货流程\n变体2：wave picking operation"

        rag_mod.ai_client.llm_chat = mock_llm_chat
        try:
            variants = await service._expand_query("波次拣货")
            assert len(variants) == 2, f"应返回 2 个变体: {variants}"
            assert "波次拣货流程" in variants[0] or "wave picking" in variants[0]
        finally:
            rag_mod.ai_client.llm_chat = original_llm_chat

    @pytest.mark.asyncio
    async def test_expand_failure_degrades(self):
        """_expand_query 失败时返回空列表"""
        from modules.rag.main import RAGService
        service = RAGService()

        import modules.rag.main as rag_mod
        original_llm_chat = rag_mod.ai_client.llm_chat

        async def mock_llm_chat(prompt, **kwargs):
            raise Exception("LLM 不可用")

        rag_mod.ai_client.llm_chat = mock_llm_chat
        try:
            variants = await service._expand_query("波次拣货")
            assert variants == [], f"失败时应返回空列表: {variants}"
        finally:
            rag_mod.ai_client.llm_chat = original_llm_chat

    @pytest.mark.asyncio
    async def test_expand_dedup_with_original(self):
        """变体与原问题相同时去重"""
        from modules.rag.main import RAGService
        service = RAGService()

        import modules.rag.main as rag_mod
        original_llm_chat = rag_mod.ai_client.llm_chat

        async def mock_llm_chat(prompt, **kwargs):
            # 返回与原问题相同的内容
            return "波次拣货\n波次拣货流程"

        rag_mod.ai_client.llm_chat = mock_llm_chat
        try:
            variants = await service._expand_query("波次拣货")
            # "波次拣货" 与原问题相同，应被过滤
            assert "波次拣货" not in variants, f"与原问题相同的变体应被过滤: {variants}"
            assert "波次拣货流程" in variants
        finally:
            rag_mod.ai_client.llm_chat = original_llm_chat


class TestQueryRewrite:
    """问题改写测试（V2: 含对话上下文）"""

    @pytest.mark.asyncio
    async def test_rewrite_with_history(self):
        """_rewrite_question 接受 history 参数"""
        from modules.rag.main import RAGService
        service = RAGService()

        import modules.rag.main as rag_mod
        original_llm_chat = rag_mod.ai_client.llm_chat

        captured_prompt = []

        async def mock_llm_chat(prompt, **kwargs):
            captured_prompt.append(prompt)
            return "改写后的问题"

        rag_mod.ai_client.llm_chat = mock_llm_chat
        try:
            history = [
                {"role": "user", "content": "波次拣货怎么操作？"},
                {"role": "assistant", "content": "首先登录系统..."},
            ]
            result = await service._rewrite_question("他怎么操作的？", history=history)
            assert result == "改写后的问题"
            # prompt 应包含对话历史
            assert "对话历史" in captured_prompt[0] or "用户" in captured_prompt[0], \
                f"prompt 应包含对话上下文: {captured_prompt[0]}"
        finally:
            rag_mod.ai_client.llm_chat = original_llm_chat

    @pytest.mark.asyncio
    async def test_rewrite_without_history(self):
        """_rewrite_question 无 history 时正常工作"""
        from modules.rag.main import RAGService
        service = RAGService()

        import modules.rag.main as rag_mod
        original_llm_chat = rag_mod.ai_client.llm_chat

        async def mock_llm_chat(prompt, **kwargs):
            return "改写后的问题"

        rag_mod.ai_client.llm_chat = mock_llm_chat
        try:
            result = await service._rewrite_question("波次拣货")
            assert result == "改写后的问题"
        finally:
            rag_mod.ai_client.llm_chat = original_llm_chat

    @pytest.mark.asyncio
    async def test_rewrite_failure_fallback(self):
        """改写失败时返回原问题"""
        from modules.rag.main import RAGService
        service = RAGService()

        import modules.rag.main as rag_mod
        original_llm_chat = rag_mod.ai_client.llm_chat

        async def mock_llm_chat(prompt, **kwargs):
            raise Exception("LLM 不可用")

        rag_mod.ai_client.llm_chat = mock_llm_chat
        try:
            result = await service._rewrite_question("波次拣货")
            assert result == "波次拣货", f"失败时应返回原问题: {result}"
        finally:
            rag_mod.ai_client.llm_chat = original_llm_chat


class TestMultiRetrieval:
    """HybridRetriever.retrieve_multi 多查询融合测试"""

    @pytest.mark.asyncio
    async def test_three_queries_fused(self):
        """3 个查询的结果 RRF 融合后去重+排序"""
        class MockVectorRetriever:
            async def retrieve(self, query, top_k=20, warehouse=None):
                # 不同查询返回不同文档
                if "波次" in query:
                    return [{"content": "波次文档", "doc_id": "d1", "chunk_index": 0, "score": 0.9}]
                elif "拣货" in query:
                    return [{"content": "拣货文档", "doc_id": "d2", "chunk_index": 0, "score": 0.85}]
                else:
                    return [{"content": "通用文档", "doc_id": "d1", "chunk_index": 0, "score": 0.8}]

        class MockKeywordRetriever:
            async def retrieve(self, query, top_k=20, warehouse=None, documents=None):
                if "波次" in query:
                    return [{"content": "波次文档", "doc_id": "d1", "chunk_index": 0, "score": 0.7}]
                else:
                    return []

        hr = HybridRetriever(
            vector_retriever=MockVectorRetriever(),
            keyword_retriever=MockKeywordRetriever(),
        )
        queries = ["波次拣货", "波次", "拣货"]
        results = await hr.retrieve_multi(queries, top_k=10)
        # 应去重 d1（两个查询都返回）
        doc_ids = [r["doc_id"] for r in results]
        # d1 应只出现一次（去重）
        assert doc_ids.count("d1") == 1, f"d1 应去重: {doc_ids}"
        # d1 应有 rrf_score 和 sources
        d1 = [r for r in results if r["doc_id"] == "d1"][0]
        assert "rrf_score" in d1
        assert "sources" in d1
        assert len(d1["sources"]) > 0  # 至少来自一个通道

    @pytest.mark.asyncio
    async def test_single_query_fallback(self):
        """只传 1 个查询时退化为正常 retrieve"""
        class MockVectorRetriever:
            async def retrieve(self, query, top_k=20, warehouse=None):
                return [{"content": "文档", "doc_id": "d1", "chunk_index": 0, "score": 0.9}]

        class MockKeywordRetriever:
            async def retrieve(self, query, top_k=20, warehouse=None, documents=None):
                return []

        hr = HybridRetriever(
            vector_retriever=MockVectorRetriever(),
            keyword_retriever=MockKeywordRetriever(),
        )
        results = await hr.retrieve_multi(["单查询"], top_k=5)
        assert len(results) == 1
        assert results[0]["doc_id"] == "d1"

    @pytest.mark.asyncio
    async def test_empty_queries(self):
        """空查询列表返回空结果"""
        hr = HybridRetriever()
        results = await hr.retrieve_multi([], top_k=5)
        assert results == []

    @pytest.mark.asyncio
    async def test_warehouse_passed_to_sub_retrievers(self):
        """retrieve_multi 将 warehouse 传递给子检索器"""
        passed = {"vector": [], "keyword": []}

        class MockVectorRetriever:
            async def retrieve(self, query, top_k=20, warehouse=None):
                passed["vector"].append(warehouse)
                return [{"content": "v", "doc_id": "d1", "chunk_index": 0, "score": 0.9}]

        class MockKeywordRetriever:
            async def retrieve(self, query, top_k=20, warehouse=None, documents=None):
                passed["keyword"].append(warehouse)
                return []

        hr = HybridRetriever(
            vector_retriever=MockVectorRetriever(),
            keyword_retriever=MockKeywordRetriever(),
        )
        await hr.retrieve_multi(["q1", "q2"], top_k=5, warehouse="WH01")
        assert all(w == "WH01" for w in passed["vector"]), \
            f"所有向量召回应收到 warehouse=WH01: {passed}"
        assert all(w == "WH01" for w in passed["keyword"]), \
            f"所有关键词召回应收到 warehouse=WH01: {passed}"
