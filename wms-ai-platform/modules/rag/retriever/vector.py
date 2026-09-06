"""
向量召回器 — Milvus Top-K 语义检索
复用 ai_client.embed() 向量化，Milvus HNSW+COSINE 索引
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from typing import List, Dict, Optional
from loguru import logger

from common.ai_client import ai_client
from common.config import settings


class VectorRetriever:
    """向量召回器（Milvus，懒加载，连接失败降级 mock）"""

    def __init__(self, collection_name: str = "wms_knowledge"):
        self.collection_name = collection_name
        self._collection = None

    def _get_collection(self):
        """懒加载 Milvus Collection，失败降级 None（mock）"""
        if self._collection is not None:
            return self._collection
        try:
            from pymilvus import connections, Collection, FieldSchema, CollectionSchema, DataType, utility

            alias = "default"
            try:
                connections.connect(
                    alias=alias,
                    host=settings.milvus_host,
                    port=str(settings.milvus_port),
                )
            except Exception:
                # 可能已连接
                pass

            if not utility.has_collection(self.collection_name, using=alias):
                # 创建 Collection
                fields = [
                    FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=True),
                    FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=settings.embedding_dim),
                    FieldSchema(name="title", dtype=DataType.VARCHAR, max_length=500),
                    FieldSchema(name="content", dtype=DataType.VARCHAR, max_length=8000),
                    FieldSchema(name="doc_id", dtype=DataType.VARCHAR, max_length=64),
                    FieldSchema(name="chunk_index", dtype=DataType.INT32),
                    FieldSchema(name="warehouse", dtype=DataType.VARCHAR, max_length=128),
                ]
                schema = CollectionSchema(fields, description="WMS知识库向量索引")
                self._collection = Collection(self.collection_name, schema, using=alias)
                # 创建 HNSW + COSINE 索引
                index_params = {
                    "index_type": "HNSW",
                    "metric_type": "COSINE",
                    "params": {
                        "M": settings.rag_retrieval_vector_m,
                        "efConstruction": settings.rag_retrieval_vector_ef_construction,
                    },
                }
                self._collection.create_index(field_name="embedding", index_params=index_params)
                logger.info(f"Milvus Collection '{self.collection_name}' 创建成功（HNSW+COSINE）")
            else:
                self._collection = Collection(self.collection_name, using=alias)
                self._collection.load()

            logger.info(f"Milvus Collection 加载成功: {self.collection_name}")
        except Exception as e:
            logger.warning(f"Milvus连接失败，向量检索降级 mock: {e}")
            self._collection = None
        return self._collection

    async def retrieve(self, query: str, top_k: int = 20,
                       warehouse: str = None) -> List[Dict]:
        """
        向量召回 Top-K

        Args:
            query:     查询文本
            top_k:     召回数量
            warehouse: 可选，仓库编号过滤（Milvus filter expression）
        Returns:
            [{"title":..., "content":..., "doc_id":..., "chunk_index":..., "warehouse":..., "score": float}, ...]
        """
        collection = self._get_collection()
        if collection is None:
            logger.debug("向量检索 mock 模式，返回空列表")
            return []

        try:
            # 向量化 query
            embeddings = await ai_client.embed([query])
            if not embeddings or not embeddings[0]:
                return []
            query_vector = embeddings[0]

            # warehouse 过滤表达式（向后兼容：已有数据 warehouse 为空字符串）
            filter_expr = None
            if warehouse:
                filter_expr = f'warehouse == "{warehouse}"'

            # Milvus 搜索
            search_kwargs = dict(
                data=[query_vector],
                anns_field="embedding",
                param={"metric_type": "COSINE", "params": {"ef": settings.rag_retrieval_vector_ef}},
                limit=top_k,
                output_fields=["title", "content", "doc_id", "chunk_index", "warehouse"],
            )
            if filter_expr is not None:
                search_kwargs["filter"] = filter_expr
            results = collection.search(**search_kwargs)

            hits = results[0] if results else []
            docs = []
            for hit in hits:
                entity = hit.entity if hasattr(hit, "entity") else {}
                docs.append({
                    "title": entity.get("title", ""),
                    "content": entity.get("content", ""),
                    "doc_id": entity.get("doc_id", ""),
                    "chunk_index": entity.get("chunk_index", 0),
                    "warehouse": entity.get("warehouse", ""),
                    "score": float(hit.distance) if hasattr(hit, "distance") else 0.0,
                })
            logger.debug(f"向量召回 {len(docs)} 条结果")
            return docs
        except Exception as e:
            logger.error(f"向量检索失败: {e}")
            return []
