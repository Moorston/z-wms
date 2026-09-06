"""
递归字符分块策略（兜底）— 按分隔符递归拆分，512 token + 64 重叠
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from typing import List, Dict
from loguru import logger

from modules.rag.chunker.base import BaseChunker, Chunk


class RecursiveChunker(BaseChunker):
    """递归字符分块器（兜底策略）"""

    # 分隔符优先级：段落 > 换行 > 句号 > 空格
    SEPARATORS = ["\n\n", "\n", "。", ".", "；", ";", "，", ",", " ", ""]

    def chunk(self, text: str = None, sections: list = None,
              tables: list = None) -> List[Chunk]:
        """递归字符分块"""
        if not text or not text.strip():
            return []

        # chunk_size 是 token 数，转换为字符数（约 2 倍）
        char_limit = self.chunk_size * 2
        overlap_chars = self.overlap * 2

        chunks_text = self._split_text_recursive(text, char_limit, self.SEPARATORS, overlap_chars)

        chunks: List[Chunk] = []
        for i, c in enumerate(chunks_text):
            c = c.strip()
            if not c:
                continue
            chunks.append(self._make_chunk(c, chunk_type="text", position=i))

        logger.debug(f"递归分块: {len(text)}字符 → {len(chunks)}块")
        return chunks

    def _split_text_recursive(self, text: str, limit: int,
                              separators: list, overlap: int) -> List[str]:
        """递归按分隔符拆分"""
        if len(text) <= limit:
            return [text]

        # 找到最合适的分隔符
        for i, sep in enumerate(separators):
            if sep == "":
                # 无分隔符可用，按字符硬切
                return self._hard_split(text, limit, overlap)

            if sep in text:
                parts = text.split(sep)
                chunks = []
                current = ""

                for part in parts:
                    candidate = current + sep + part if current else part
                    if len(candidate) <= limit:
                        current = candidate
                    else:
                        if current:
                            chunks.append(current)
                        # 如果单个 part 就超限，递归用更细的分隔符
                        if len(part) > limit:
                            sub = self._split_text_recursive(part, limit, separators[i + 1:], overlap)
                            chunks.extend(sub)
                            current = ""
                        else:
                            current = part

                if current:
                    chunks.append(current)

                # 添加重叠
                if overlap > 0 and len(chunks) > 1:
                    chunks = self._add_overlap(chunks, overlap)

                return chunks

        return self._hard_split(text, limit, overlap)

    def _hard_split(self, text: str, limit: int, overlap: int) -> List[str]:
        """硬切分（按字符长度）"""
        chunks = []
        start = 0
        while start < len(text):
            end = min(start + limit, len(text))
            chunks.append(text[start:end])
            if end >= len(text):
                break
            start = end - overlap if overlap < limit else end
        return chunks

    def _add_overlap(self, chunks: List[str], overlap: int) -> List[str]:
        """给分块添加重叠"""
        result = [chunks[0]]
        for i in range(1, len(chunks)):
            prev_tail = chunks[i - 1][-overlap:] if len(chunks[i - 1]) > overlap else chunks[i - 1]
            result.append(prev_tail + chunks[i])
        return result
