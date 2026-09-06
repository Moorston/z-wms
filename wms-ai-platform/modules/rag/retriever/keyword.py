"""
关键词召回器 — BM25 / MySQL FULLTEXT
支持内存 BM25 召回和 MySQL FULLTEXT 索引召回两种模式
中文分词使用 jieba + WMS 领域词表
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from typing import List, Dict, Optional
from loguru import logger

from common.data_client import mysql_client
from common.config import settings

# WMS 领域词表（jieba userdict 格式）
_WORD_DICT_PATH = os.path.join(os.path.dirname(__file__), "word_dict.txt")
jieba_loaded = False

def _ensure_jieba_loaded():
    """懒加载 jieba + 领域词表，避免启动时加载延迟"""
    global jieba_loaded
    if jieba_loaded:
        return
    import jieba
    try:
        jieba.load_userdict(_WORD_DICT_PATH)
        logger.debug(f"jieba 领域词表已加载: {os.path.basename(_WORD_DICT_PATH)}")
    except Exception as e:
        logger.warning(f"领域词表加载失败（使用默认词典）: {e}")
    jieba_loaded = True


def tokenize(text: str) -> List[str]:
    """中文分词：jieba 精确模式 + WMS 领域词表，过滤纯标点和空串"""
    _ensure_jieba_loaded()
    import jieba
    tokens = jieba.cut(text, cut_all=False)
    return [t.lower() for t in tokens if t.strip()]


class KeywordRetriever:
    """关键词召回器（BM25 / MySQL FULLTEXT，懒加载，降级安全）"""

    def __init__(self):
        self._bm25 = None  # 内存 BM25 延迟构建
        # 关键词召回模式：从配置读取
        self._mode = settings.rag_retrieval_keyword_mode

    async def retrieve(self, query: str, top_k: int = 20,
                       documents: List[Dict] = None,
                       warehouse: str = None) -> List[Dict]:
        """
        关键词召回

        Args:
            query:     查询文本
            top_k:     召回数量
            documents: 可选，传入文档列表则走内存 BM25；否则走 MySQL FULLTEXT
            warehouse: 可选，仓库编号过滤（MySQL WHERE 条件）
        Returns:
            [{"content":..., "doc_id":..., "score":..., "chunk_id":...}, ...]
        """
        # 配置强制指定 mysql_fulltext 模式时跳过 BM25
        if self._mode == "mysql_fulltext":
            return self._mysql_fulltext_retrieve(query, top_k, warehouse)

        # 默认 bm25 模式
        if documents:
            return self._bm25_retrieve(query, documents, top_k)

        return self._mysql_fulltext_retrieve(query, top_k, warehouse)

    def _mysql_fulltext_retrieve(self, query: str, top_k: int,
                                warehouse: str = None) -> List[Dict]:
        """MySQL FULLTEXT 召回（使用 ngram parser 支持中文）"""
        try:
            sql = (
                "SELECT chunk_id, doc_id, content, "
                "MATCH(content) AGAINST(%s IN NATURAL LANGUAGE MODE) AS score "
                "FROM kb_chunk "
                "WHERE MATCH(content) AGAINST(%s IN NATURAL LANGUAGE MODE)"
            )
            params = [query, query]
            if warehouse:
                sql += " AND warehouse = %s"
                params.append(warehouse)
            sql += " ORDER BY score DESC LIMIT %s"
            params.append(top_k)
            rows = mysql_client.query(sql, tuple(params))
            results = []
            for row in rows:
                results.append({
                    "content": row.get("content", ""),
                    "doc_id": row.get("doc_id", ""),
                    "chunk_id": row.get("chunk_id"),
                    "score": float(row.get("score", 0.0)),
                })
            logger.debug(f"MySQL FULLTEXT 召回 {len(results)} 条结果")
            return results
        except Exception as e:
            logger.warning(f"MySQL FULLTEXT 召回失败（降级返回空）: {e}")
            return []

    def _bm25_retrieve(self, query: str, documents: List[Dict], top_k: int) -> List[Dict]:
        """内存 BM25 召回（rank-bm25），中文使用 jieba 分词"""
        try:
            from rank_bm25 import BM25Okapi
        except ImportError:
            logger.warning("rank-bm25 未安装，关键词召回降级返回原序")
            return documents[:top_k]

        try:
            corpus = [tokenize(d.get("content", d.get("title", ""))) for d in documents]
            bm25 = BM25Okapi(corpus)
            query_tokens = tokenize(query)
            scores = bm25.get_scores(query_tokens)

            # 按分数排序取 top_k
            ranked = sorted(enumerate(scores), key=lambda x: x[1], reverse=True)[:top_k]
            results = []
            for idx, score in ranked:
                doc = documents[idx].copy()
                doc["score"] = float(score)
                results.append(doc)
            logger.debug(f"BM25 内存召回 {len(results)} 条结果")
            return results
        except Exception as e:
            logger.error(f"BM25 召回失败: {e}")
            return documents[:top_k]
