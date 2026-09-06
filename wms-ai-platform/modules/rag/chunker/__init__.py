"""
WMS AI Platform - RAG 混合分块策略
结构化分块（主） + 递归字符分块（兜底） + 表格分块
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from modules.rag.chunker.base import BaseChunker, Chunk
from modules.rag.chunker.structural import StructuralChunker
from modules.rag.chunker.recursive import RecursiveChunker
from modules.rag.chunker.table import TableChunker


class DefaultChunker:
    """
    组合分块策略：
    - 有 sections → StructuralChunker（标题结构化分块）
    - 有 tables   → TableChunker（表格整体成块）
    - 纯文本      → RecursiveChunker（递归字符分块兜底）
    """

    def __init__(self, chunk_size: int = 512, overlap: int = 64):
        self.structural = StructuralChunker(chunk_size, overlap)
        self.recursive = RecursiveChunker(chunk_size, overlap)
        self.table = TableChunker(chunk_size, overlap)
        self.chunk_size = chunk_size
        self.overlap = overlap

    def chunk(self, text: str = None, sections: list = None,
              tables: list = None) -> list:
        """
        自动选策略分块

        Args:
            text:     纯文本（无 sections 时使用）
            sections: 结构化段落列表（ParsedDocument.sections）
            tables:   表格列表（ParsedDocument.tables）
        Returns:
            List[Chunk]
        """
        chunks = []
        position = 0

        # 1. 有 sections → 结构化分块
        if sections:
            struct_chunks = self.structural.chunk(sections=sections)
            for c in struct_chunks:
                c.position = position
                position += 1
                chunks.append(c)

        # 2. 有 tables → 表格分块
        if tables:
            table_chunks = self.table.chunk(tables=tables)
            for c in table_chunks:
                c.position = position
                position += 1
                chunks.append(c)

        # 3. 纯文本（无 sections 和 tables）→ 递归分块
        if not chunks and text:
            rec_chunks = self.recursive.chunk(text=text)
            for c in rec_chunks:
                c.position = position
                position += 1
                chunks.append(c)

        return chunks


__all__ = [
    "BaseChunker", "Chunk", "StructuralChunker",
    "RecursiveChunker", "TableChunker", "DefaultChunker",
]
