"""
表格分块策略 — 表格整体成块（转 Markdown 表格文本）
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from typing import List, Dict
from loguru import logger

from modules.rag.chunker.base import BaseChunker, Chunk


class TableChunker(BaseChunker):
    """表格分块器（表格整体成块）"""

    def chunk(self, text: str = None, sections: list = None,
              tables: list = None) -> List[Chunk]:
        """
        将表格转为 Markdown 格式文本块

        Args:
            tables: ParsedDocument.tables 列表，每个含 headers/rows/page
        """
        if not tables:
            return []

        chunks: List[Chunk] = []

        for i, tbl in enumerate(tables):
            headers = tbl.get("headers", [])
            rows = tbl.get("rows", [])
            page = tbl.get("page", 0)

            if not headers:
                continue

            # 转 Markdown 表格文本
            md_lines = []
            md_lines.append("| " + " | ".join(str(h) for h in headers) + " |")
            md_lines.append("| " + " | ".join("---" for _ in headers) + " |")
            for row in rows:
                md_lines.append("| " + " | ".join(str(c) for c in row) + " |")

            content = "\n".join(md_lines)
            chunk = self._make_chunk(
                content,
                chunk_type="table",
                position=i,
                headers=headers,
                row_count=len(rows),
                page=page,
            )
            chunks.append(chunk)

        logger.debug(f"表格分块: {len(tables)}表 → {len(chunks)}块")
        return chunks
