"""
混合检索器 — 向量 + 关键词 RRF 融合去重
RRF (Reciprocal Rank Fusion): score = sum(1 / (k + rank_i))
"""
import os, sys
import asyncio
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from typing import List, Dict, Optional
from loguru import logger

from common.config import settings
from modules.rag.retriever.vector import VectorRetriever
from modules.rag.retriever.keyword import KeywordRetriever


class HybridRetriever:
    """混合检索器（向量 + 关键词 RRF 融合）"""

    def __init__(self, vector_retriever: VectorRetriever = None,
                 keyword_retriever: KeywordRetriever = None,
                 rrf_k: int = None):
        self.vector_retriever = vector_retriever or VectorRetriever()
        self.keyword_retriever = keyword_retriever or KeywordRetriever()
        # RRF 公式参数，默认从配置读取（经典默认值 60）
        self.rrf_k = rrf_k if rrf_k is not None else settings.rag_retrieval_rrf_k

    async def retrieve(self, query: str, top_k: int = 20,
                       warehouse: str = None) -> List[Dict]:
        """
        混合召回：并行调向量+关键词，RRF 融合去重

        Args:
            query:     查询文本
            top_k:     最终返回数量
            warehouse: 可选，仓库编号过滤（穿透到子检索器）
        Returns:
            融合排序后的文档列表
        """
        fetch_k = top_k * 2  # 每路多召回一些供融合

        # 并行召回
        try:
            vector_results, keyword_results = await asyncio.gather(
                self.vector_retriever.retrieve(query, fetch_k, warehouse=warehouse),
                self.keyword_retriever.retrieve(query, fetch_k, warehouse=warehouse),
                return_exceptions=True,
            )
        except Exception as e:
            logger.error(f"混合召回并行执行失败: {e}")
            vector_results, keyword_results = [], []

        # 异常降级
        if isinstance(vector_results, Exception):
            logger.warning(f"向量召回异常，降级跳过: {vector_results}")
            vector_results = []
        if isinstance(keyword_results, Exception):
            logger.warning(f"关键词召回异常，降级跳过: {keyword_results}")
            keyword_results = []

        # 如果两路都空，返回空
        if not vector_results and not keyword_results:
            return []

        # RRF 融合
        return self._rrf_fuse(vector_results, keyword_results, top_k)

    def _rrf_fuse(self, vector_results: List[Dict],
                  keyword_results: List[Dict], top_k: int) -> List[Dict]:
        """
        RRF (Reciprocal Rank Fusion) 融合算法

        对每个文档，score = sum(1 / (rrf_k + rank_i))
        按 RRF score 降序取 top_k
        """
        # 去重键：优先 doc_id + chunk_index，否则用 content 前缀
        def _dedup_key(doc: Dict) -> str:
            doc_id = doc.get("doc_id", "")
            chunk_index = doc.get("chunk_index", doc.get("chunk_id", ""))
            if doc_id and chunk_index != "":
                return f"{doc_id}:{chunk_index}"
            content = doc.get("content", "")
            return content[:100] if content else str(id(doc))

        fused: Dict[str, Dict] = {}

        # 向量结果按 score 已排序，取 rank
        for rank, doc in enumerate(vector_results):
            key = _dedup_key(doc)
            if key not in fused:
                fused[key] = doc.copy()
                fused[key]["rrf_score"] = 0.0
                fused[key]["sources"] = []
            fused[key]["rrf_score"] += 1.0 / (self.rrf_k + rank + 1)
            fused[key]["sources"].append("vector")

        # 关键词结果
        for rank, doc in enumerate(keyword_results):
            key = _dedup_key(doc)
            if key not in fused:
                fused[key] = doc.copy()
                fused[key]["rrf_score"] = 0.0
                fused[key]["sources"] = []
            fused[key]["rrf_score"] += 1.0 / (self.rrf_k + rank + 1)
            if "keyword" not in fused[key]["sources"]:
                fused[key]["sources"].append("keyword")

        # 按 RRF score 降序取 top_k
        ranked = sorted(fused.values(), key=lambda x: x["rrf_score"], reverse=True)
        results = ranked[:top_k]

        logger.debug(
            f"RRF融合: 向量{len(vector_results)} + 关键词{len(keyword_results)} "
            f"→ 去重后{len(fused)} → Top{len(results)}"
        )
        return results

    async def retrieve_multi(self, queries: List[str], top_k: int = 20,
                             warehouse: str = None) -> List[Dict]:
        """
        多查询并行召回：对每个查询独立执行向量+关键词召回，RRF 融合所有结果

        Args:
            queries:  查询文本列表（原文+改写+扩展变体）
            top_k:    最终返回数量
            warehouse: 可选，仓库编号过滤（穿透到子检索器）
        Returns:
            融合排序后的文档列表（含 rrf_score 和 sources 字段）
        """
        if not queries:
            return []

        fetch_k = top_k * 2

        # 对每个查询并行发起向量+关键词召回
        tasks = []
        for q in queries:
            tasks.append(self.vector_retriever.retrieve(q, fetch_k, warehouse=warehouse))
            tasks.append(self.keyword_retriever.retrieve(q, fetch_k, warehouse=warehouse))
        results = await asyncio.gather(*tasks, return_exceptions=True)

        # 去重键（与 _rrf_fuse 一致）
        def _dedup_key(doc: Dict) -> str:
            doc_id = doc.get("doc_id", "")
            chunk_index = doc.get("chunk_index", doc.get("chunk_id", ""))
            if doc_id and chunk_index != "":
                return f"{doc_id}:{chunk_index}"
            content = doc.get("content", "")
            return content[:100] if content else str(id(doc))

        # 分组处理：偶数索引=向量，奇数索引=关键词
        all_fused: Dict[str, Dict] = {}
        for i, result in enumerate(results):
            if isinstance(result, Exception):
                logger.warning(f"多路召回第{i}路异常，降级跳过: {result}")
                continue
            channel = "vector" if i % 2 == 0 else "keyword"
            for rank, doc in enumerate(result):
                key = _dedup_key(doc)
                if key not in all_fused:
                    all_fused[key] = doc.copy()
                    all_fused[key]["rrf_score"] = 0.0
                    all_fused[key]["sources"] = []
                all_fused[key]["rrf_score"] += 1.0 / (self.rrf_k + rank + 1)
                if channel not in all_fused[key]["sources"]:
                    all_fused[key]["sources"].append(channel)

        ranked = sorted(all_fused.values(), key=lambda x: x["rrf_score"], reverse=True)
        results_final = ranked[:top_k]

        logger.debug(
            f"多路RRF融合: {len(queries)}查询 × 2路 "
            f"→ 去重后{len(all_fused)} → Top{len(results_final)}"
        )
        return results_final
