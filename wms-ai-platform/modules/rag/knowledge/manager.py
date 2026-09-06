"""
知识库管理器 — 文档 CRUD + 分类树 + 处理状态跟踪
写 MySQL kb_document / kb_chunk 表
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

import json
from typing import List, Dict, Optional
from loguru import logger

from common.data_client import mysql_client, minio_client
from common.utils import gen_id, now_str


class KnowledgeManager:
    """知识库文档管理（MySQL 持久化，mock 降级安全）"""

    def __init__(self):
        self.minio_prefix = "rag/documents"

    # ========== 文档 CRUD ==========

    def create_document(self, title: str, category: str = "general",
                        warehouse: str = None, tags: list = None,
                        source_file: str = None, file_type: str = None,
                        file_size: int = 0, minio_object: str = None,
                        created_by: str = None) -> str:
        """创建文档记录（status=pending），返回 doc_id"""
        doc_id = gen_id()
        sql = """
            INSERT INTO kb_document
            (doc_id, title, category, warehouse, tags, source_file, file_type,
             file_size, minio_object, chunk_count, status, created_by)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """
        mysql_client.execute(sql, (
            doc_id, title, category, warehouse,
            json.dumps(tags or [], ensure_ascii=False),
            source_file, file_type, file_size, minio_object,
            0, "pending", created_by,
        ))
        logger.info(f"文档创建: doc_id={doc_id}, title={title}, category={category}")
        return doc_id

    def update_status(self, doc_id: str, status: str,
                      chunk_count: int = None, error_msg: str = None):
        """更新文档处理状态"""
        parts = ["status = %s"]
        params = [status]
        if chunk_count is not None:
            parts.append("chunk_count = %s")
            params.append(chunk_count)
        if error_msg:
            parts.append("error_msg = %s")
            params.append(error_msg)
        params.append(doc_id)
        sql = f"UPDATE kb_document SET {', '.join(parts)} WHERE doc_id = %s"
        mysql_client.execute(sql, tuple(params))
        logger.info(f"文档状态更新: doc_id={doc_id}, status={status}")

    def get_document(self, doc_id: str) -> Optional[Dict]:
        """获取单个文档"""
        rows = mysql_client.query(
            "SELECT * FROM kb_document WHERE doc_id = %s", (doc_id,)
        )
        return rows[0] if rows else None

    def list_documents(self, category: str = None, status: str = None,
                       warehouse: str = None, limit: int = 100,
                       offset: int = 0) -> List[Dict]:
        """分页列出文档"""
        conditions = []
        params = []
        if category:
            conditions.append("category = %s")
            params.append(category)
        if status:
            conditions.append("status = %s")
            params.append(status)
        if warehouse:
            conditions.append("warehouse = %s")
            params.append(warehouse)

        where = " WHERE " + " AND ".join(conditions) if conditions else ""
        sql = f"SELECT * FROM kb_document{where} ORDER BY created_at DESC LIMIT %s OFFSET %s"
        params.extend([limit, offset])
        return mysql_client.query(sql, tuple(params))

    def delete_document(self, doc_id: str):
        """删除文档（kb_chunk 外键级联删除）"""
        # 先删 Milvus 向量（如果有）
        try:
            doc = self.get_document(doc_id)
            if doc and doc.get("minio_object"):
                minio_client.remove_object(doc["minio_object"])
        except Exception as e:
            logger.warning(f"删除 MinIO 对象失败: {e}")

        mysql_client.execute("DELETE FROM kb_document WHERE doc_id = %s", (doc_id,))
        logger.info(f"文档删除: doc_id={doc_id}")

    # ========== 分块管理 ==========

    def save_chunks(self, doc_id: str, chunks: list,
                    embeddings: list = None) -> int:
        """
        保存分块到 kb_chunk 表

        Args:
            doc_id:      文档ID
            chunks:      List[Chunk] 或 List[Dict]（含 content/chunk_type/position/token_count/metadata）
            embeddings:  向量列表（Milvus insert 后返回的 embedding_id 列表）
        Returns:
            保存的分块数
        """
        if not chunks:
            return 0

        values = []
        for i, chunk in enumerate(chunks):
            # 兼容 Chunk 对象和 dict
            if hasattr(chunk, "content"):
                content = chunk.content
                chunk_type = chunk.chunk_type
                position = chunk.position
                token_count = chunk.token_count
                metadata = chunk.metadata
            else:
                content = chunk.get("content", "")
                chunk_type = chunk.get("chunk_type", "text")
                position = chunk.get("position", i)
                token_count = chunk.get("token_count", 0)
                metadata = chunk.get("metadata", {})

            embedding_id = None
            if embeddings and i < len(embeddings):
                embedding_id = str(embeddings[i]) if embeddings[i] else None

            values.append((
                doc_id, i, content, chunk_type, position,
                token_count, json.dumps(metadata, ensure_ascii=False),
                embedding_id,
            ))

        sql = """
            INSERT INTO kb_chunk
            (doc_id, chunk_index, content, chunk_type, position, token_count, metadata, embedding_id)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
        """
        for v in values:
            mysql_client.execute(sql, v)

        # 更新文档分块数和状态
        self.update_status(doc_id, "completed", chunk_count=len(chunks))
        logger.info(f"分块保存: doc_id={doc_id}, count={len(chunks)}")
        return len(chunks)

    def get_chunks(self, doc_id: str) -> List[Dict]:
        """获取文档的所有分块"""
        return mysql_client.query(
            "SELECT * FROM kb_chunk WHERE doc_id = %s ORDER BY chunk_index", (doc_id,)
        )

    # ========== 分类树 ==========

    def get_category_tree(self) -> List[Dict]:
        """获取分类树（按 category 分组统计）"""
        sql = """
            SELECT category, COUNT(*) as doc_count, SUM(chunk_count) as chunk_count
            FROM kb_document WHERE status = 'completed'
            GROUP BY category
        """
        return mysql_client.query(sql)
