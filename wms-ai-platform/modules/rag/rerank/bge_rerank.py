import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))
from common.ai_client import ai_client
from common.config import settings
from loguru import logger
from typing import List, Dict


class BgeReranker:
    """
    Bge-Reranker 客户端。

    通过 ai_client.rerank() 调用 model_service 的 /v1/llm/rerank 端点，
    后者再调 SiliconFlow API（BAAI/bge-reranker-v2-m3）。
    所有 AI 调用走 ai_client 统一客户端，不直接 import httpx。

    PRD V2.0 修复（Bug #7）：
      ai_client.rerank() 返回 {"index", "document", "relevance_score"}，
      但此前 rerank_texts() 中 document 字段与 texts 列表无法对应
      （texts 不含 "document" key，导致返回空字符串）。
      修复方案：统一两个方法使用 texts 列表做索引映射，
      使 rerank() 和 rerank_texts() 返回相同结构的原始数据引用。
    """

    def __init__(self, model: str = None, top_n: int = 5):
        """
        Args:
            model:  rerank 模型名称，默认取 settings.rerank_model
                    （BAAI/bge-reranker-v2-m3）
            top_n:  默认返回前 N 条
        """
        self.model = model or settings.rerank_model
        self.top_n = top_n

    async def rerank(self, query: str, documents: List[Dict], top_n: int = None) -> List[Dict]:
        """
        对文档列表做重排序。

        Args:
            query:     查询文本
            documents: 文档 dict 列表，每个 dict 至少含 content 字段
                       （回退到 title），可能含 title/score 等
            top_n:     返回前 N 条，默认 self.top_n
        Returns:
            重排序后的文档 dict 列表，合并 relevance_score 字段，
            按 relevance_score 降序。documents 为空时返回空列表。
            异常时降级为按原顺序返回 top_n 个文档（不抛异常）。
        """
        if not documents:
            return []

        effective_top_n = top_n or self.top_n

        try:
            # 从文档 dict 提取纯文本用于 rerank API
            texts = [d.get("content", d.get("title", "")) for d in documents]
            results = await ai_client.rerank(query, texts, effective_top_n)

            reranked = []
            for item in results:
                idx = item.get("index", 0)
                score = item.get("relevance_score", 0.0)
                if 0 <= idx < len(documents):
                    doc = dict(documents[idx])
                    doc["relevance_score"] = score
                    reranked.append(doc)

            reranked.sort(key=lambda d: d.get("relevance_score", 0.0), reverse=True)
            return reranked

        except Exception as e:
            logger.warning(
                f"BgeReranker.rerank 失败，降级返回原顺序前 {effective_top_n} 条: {e}"
            )
            return documents[:effective_top_n]

    async def rerank_texts(self, query: str, texts: List[str], top_n: int = None) -> List[Dict]:
        """
        便捷方法：直接接收文本列表做重排序。

        Args:
            query:  查询文本
            texts:  文本字符串列表
            top_n:  返回前 N 条，默认 self.top_n
        Returns:
            [{"text": str, "relevance_score": float, "index": int}, ...]
            其中 index 为原始 texts 列表中的位置（0-based），
            text 为原始 texts[index] 对应的字符串（不使用 API 返回的 document 字段）。
        """
        if not texts:
            return []

        effective_top_n = top_n or self.top_n

        try:
            results = await ai_client.rerank(query, texts, effective_top_n)

            # 使用索引回查原始 texts，避免依赖 API 返回的 document 字段
            return [
                {
                    "text": texts[idx] if (idx is not None and 0 <= idx < len(texts)) else "",
                    "relevance_score": item.get("relevance_score", 0.0),
                    "index": item.get("index", 0),
                }
                for item in results
                if (idx := item.get("index", 0)) is not None and 0 <= idx < len(texts)
            ]

        except Exception as e:
            logger.warning(
                f"BgeReranker.rerank_texts 失败，降级返回原顺序前 {effective_top_n} 条: {e}"
            )
            return [
                {"text": t, "relevance_score": 0.0, "index": i}
                for i, t in enumerate(texts[:effective_top_n])
            ]
