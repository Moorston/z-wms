"""
测试：RAG 检索参数配置化
验证所有硬编码值已从 config 读取
"""
import pytest
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from common.config import settings


class TestRAGConfig:
    """RAG 检索参数配置测试"""

    def test_vector_ef_config(self):
        """HNSW ef 搜索参数已配置化"""
        assert hasattr(settings, "rag_retrieval_vector_ef")
        assert isinstance(settings.rag_retrieval_vector_ef, int)
        assert settings.rag_retrieval_vector_ef >= 16  # 合理范围

    def test_vector_m_config(self):
        """HNSW M 参数已配置化"""
        assert hasattr(settings, "rag_retrieval_vector_m")
        assert isinstance(settings.rag_retrieval_vector_m, int)
        assert settings.rag_retrieval_vector_m >= 8

    def test_vector_ef_construction_config(self):
        """HNSW efConstruction 已配置化"""
        assert hasattr(settings, "rag_retrieval_vector_ef_construction")
        assert isinstance(settings.rag_retrieval_vector_ef_construction, int)
        assert settings.rag_retrieval_vector_ef_construction >= 40

    def test_rrf_k_config(self):
        """RRF 融合参数已配置化"""
        assert hasattr(settings, "rag_retrieval_rrf_k")
        assert isinstance(settings.rag_retrieval_rrf_k, int)
        assert settings.rag_retrieval_rrf_k > 0

    def test_top_k_config(self):
        """混合召回 Top-K 已配置化"""
        assert hasattr(settings, "rag_retrieval_top_k")
        assert isinstance(settings.rag_retrieval_top_k, int)
        assert settings.rag_retrieval_top_k >= 5

    def test_rerank_top_n_config(self):
        """Rerank Top-N 已配置化"""
        assert hasattr(settings, "rag_retrieval_rerank_top_n")
        assert isinstance(settings.rag_retrieval_rerank_top_n, int)
        assert settings.rag_retrieval_rerank_top_n >= 1

    def test_keyword_mode_config(self):
        """关键词召回模式已配置化"""
        assert hasattr(settings, "rag_retrieval_keyword_mode")
        assert settings.rag_retrieval_keyword_mode in ("bm25", "mysql_fulltext")


class TestConfigDrivenComponents:
    """验证组件从配置读取参数（而非硬编码）"""

    def test_hybrid_retriever_rrf_from_config(self):
        """HybridRetriever 应从 config 读取 rrf_k"""
        from modules.rag.retriever.fusion import HybridRetriever
        retriever = HybridRetriever()
        assert retriever.rrf_k == settings.rag_retrieval_rrf_k

    def test_hybrid_retriever_custom_rrf(self):
        """HybridRetriever 支持自定义 rrf_k 覆盖"""
        from modules.rag.retriever.fusion import HybridRetriever
        retriever = HybridRetriever(rrf_k=100)
        assert retriever.rrf_k == 100

    def test_keyword_retriever_mode_from_config(self):
        """KeywordRetriever 应从 config 读取 mode"""
        from modules.rag.retriever.keyword import KeywordRetriever
        retriever = KeywordRetriever()
        assert retriever._mode == settings.rag_retrieval_keyword_mode
