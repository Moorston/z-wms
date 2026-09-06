"""
WMS AI Platform - 知识图谱检索模块
查询时纯正则实体识别 + 1-2 跳展开 + 路径序列化
"""
import os
import sys
import time
from typing import List, Dict, Optional

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from loguru import logger
from common.config import settings
from modules.rag.kg.extraction import extract_entities
from modules.rag.kg.store import KGStore


class KnowledgeGraphRetriever:
    """
    知识图谱检索器
    独立通道，不走 RRF 融合，结果序列化后追加到 docs 列表
    """

    def __init__(self, store: Optional[KGStore] = None):
        self._store = store or KGStore()

    @property
    def store(self) -> KGStore:
        return self._store

    async def retrieve(self, query: str, top_k: int = None,
                       warehouse: str = None) -> List[Dict]:
        """
        KG 检索：实体识别 → 多跳展开 → 路径序列化
        返回格式与 vector/keyword 一致，可直接追加到 docs 列表

        返回：[{"title": "知识图谱", "content": "路径序列化文本", "doc_id": "KG",
                "chunk_index": 0, "relevance_score": 1.0, "sources": ["kg"]}]
        """
        if top_k is None:
            top_k = settings.rag_kg_top_k
        max_hops = settings.rag_kg_max_hops

        start = time.time()

        # 1. 实体识别
        entities = self._match_entities(query)
        if not entities:
            return []

        # 2. 检查哪些实体存在于图谱中
        matched_nodes = [e for e in entities if self._store.has_node(e)]
        if not matched_nodes:
            return []

        # 3. 多跳展开
        paths = []
        for node in matched_nodes:
            neighbors = self._store.get_neighbors(node, max_hops=max_hops)
            paths.extend(neighbors)

        if not paths:
            return []

        # 4. 按置信度排序 + 限制数量
        paths.sort(key=lambda p: p["confidence"], reverse=True)
        paths = paths[:top_k]

        # 5. 序列化
        results = []
        for p in paths:
            path_text = self._serialize_path(p["path"])
            results.append({
                "title": "知识图谱",
                "content": f"实体关系路径: {path_text}",
                "doc_id": "KG",
                "chunk_index": 0,
                "relevance_score": p["confidence"],
                "sources": ["kg"],
                "path": path_text,
                "entities": matched_nodes,
                "hop_count": p["hop_count"],
            })

        latency_ms = int((time.time() - start) * 1000)
        logger.info(
            f"KG 检索完成: query={query[:30]}, "
            f"entities={len(entities)}, matched={len(matched_nodes)}, "
            f"paths={len(paths)}, latency={latency_ms}ms"
        )
        return results

    def _match_entities(self, query: str) -> List[str]:
        """
        从查询文本中匹配实体
        纯正则+词典，不使用 LLM
        """
        entity_list = extract_entities(query)
        # 去重并保持顺序
        seen = set()
        unique = []
        for name, _ in entity_list:
            if name not in seen:
                seen.add(name)
                unique.append(name)
        return unique

    def _serialize_path(self, path: List[tuple]) -> str:
        """
        序列化为 'A → 关系 → B → 关系 → C'
        path 格式：[(subject, predicate, object), ...]
        """
        if not path:
            return ""
        parts = [path[0][0]]  # 起始实体
        for s, p, o in path:
            parts.append(f" → {p} → {o}")
        return "".join(parts)
