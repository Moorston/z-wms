"""
结构化分块策略（主策略）— 按 Markdown 标题块分块，超长块用递归分块再拆
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from typing import List, Dict
from loguru import logger

from modules.rag.chunker.base import BaseChunker, Chunk
from modules.rag.chunker.recursive import RecursiveChunker


class StructuralChunker(BaseChunker):
    """结构化分块（按标题块分块）"""

    def __init__(self, chunk_size: int = 512, overlap: int = 64):
        super().__init__(chunk_size, overlap)
        self._recursive = RecursiveChunker(chunk_size, overlap)

    def chunk(self, text: str = None, sections: list = None,
              tables: list = None) -> List[Chunk]:
        """
        按 sections（标题块）分块

        Args:
            sections: ParsedDocument.sections 列表，每个含 text/heading/page
        """
        if not sections:
            return []

        chunks: List[Chunk] = []
        position = 0

        for sec in sections:
            sec_text = sec.get("text", "").strip()
            if not sec_text:
                continue

            heading = sec.get("heading", "")
            page = sec.get("page", 0)

            # 标题行本身
            if heading and sec_text == heading:
                chunks.append(self._make_chunk(
                    sec_text, chunk_type="heading", position=position,
                    heading=heading, page=page,
                ))
                position += 1
                continue

            # 内容块：如果超长，用递归分块再拆
            token_count = self._count_tokens(sec_text)
            if token_count > self.chunk_size:
                sub_chunks = self._recursive.chunk(text=sec_text)
                for sc in sub_chunks:
                    sc.position = position
                    sc.metadata["heading"] = heading
                    sc.metadata["page"] = page
                    position += 1
                    chunks.append(sc)
            else:
                chunks.append(self._make_chunk(
                    sec_text, chunk_type="text", position=position,
                    heading=heading, page=page,
                ))
                position += 1

        logger.debug(f"结构化分块: {len(sections)}段 → {len(chunks)}块")
        return chunks
