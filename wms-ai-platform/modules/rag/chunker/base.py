"""
分块基类 — Chunk 数据结构 + BaseChunker 抽象基类
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from abc import ABC, abstractmethod
from typing import List, Dict, Optional
from pydantic import BaseModel, Field
from loguru import logger


class Chunk(BaseModel):
    """文档分块数据结构"""
    content: str                           # 分块文本内容
    chunk_type: str = "text"              # text / heading / table
    position: int = 0                     # 在文档中的位置序号
    token_count: int = 0                  # 估算 token 数
    metadata: Dict = Field(default_factory=dict)  # 页码/标题/表头等

    class Config:
        arbitrary_types_allowed = True


class BaseChunker(ABC):
    """分块器抽象基类"""

    def __init__(self, chunk_size: int = 512, overlap: int = 64):
        self.chunk_size = chunk_size
        self.overlap = overlap

    @abstractmethod
    def chunk(self, text: str = None, sections: list = None,
              tables: list = None) -> List[Chunk]:
        """分块抽象方法"""
        ...

    def _count_tokens(self, text: str) -> int:
        """
        简单 token 估算：
        - 中文约 2 字符 = 1 token
        - 英文约 4 字符 = 1 token
        混合取 len(text) // 2 近似
        """
        if not text:
            return 0
        # 统计中文字符数
        import re
        cjk_count = len(re.findall(r"[一-鿿]", text))
        other_count = len(text) - cjk_count
        return cjk_count // 2 + other_count // 4

    def _make_chunk(self, content: str, chunk_type: str = "text",
                    position: int = 0, **metadata) -> Chunk:
        """工厂方法：创建 Chunk 并自动计算 token_count"""
        return Chunk(
            content=content,
            chunk_type=chunk_type,
            position=position,
            token_count=self._count_tokens(content),
            metadata=metadata,
        )
