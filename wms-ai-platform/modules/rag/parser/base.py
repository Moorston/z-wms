"""
文档解析基类 — ParsedDocument 数据结构 + DocumentParser 抽象基类 + ParserRegistry 插件注册
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import List, Dict, Optional
from loguru import logger


@dataclass
class ParsedDocument:
    """解析后的文档统一数据结构"""
    title: str = ""
    sections: List[Dict] = field(default_factory=list)  # 每个含 text/heading/page
    tables: List[Dict] = field(default_factory=list)     # 每个含 headers/rows
    images: List[str] = field(default_factory=list)     # 图片描述或路径
    metadata: Dict = field(default_factory=dict)        # 文件名/大小/页数等
    total_chars: int = 0

    def add_section(self, text: str, heading: str = "", page: int = 0):
        """添加一个文本段"""
        self.sections.append({"text": text, "heading": heading, "page": page})
        self.total_chars += len(text)

    def add_table(self, headers: List[str], rows: List[List], page: int = 0):
        """添加一个表格"""
        self.tables.append({"headers": headers, "rows": rows, "page": page})

    def get_full_text(self) -> str:
        """获取拼接全文（供 fallback 分块用）"""
        parts = [s["text"] for s in self.sections]
        for t in self.tables:
            parts.append(" | ".join(t["headers"]))
            for row in t["rows"]:
                parts.append(" | ".join(str(c) for c in row))
        return "\n\n".join(parts)


class DocumentParser(ABC):
    """文档解析器抽象基类"""

    supported_extensions: List[str] = []

    @abstractmethod
    def parse(self, file_path: str = None, content: bytes = None,
              filename: str = None) -> ParsedDocument:
        """
        解析文档

        Args:
            file_path: 文件路径（二选一）
            content:   文件二进制内容
            filename:  文件名（用于判断类型，content 模式必需）
        Returns:
            ParsedDocument
        """
        ...

    def _read_content(self, file_path: str = None, content: bytes = None) -> bytes:
        """统一的文件读取工具"""
        if content is not None:
            return content
        if file_path:
            with open(file_path, "rb") as f:
                return f.read()
        return b""

    def _get_filename(self, file_path: str = None, filename: str = None) -> str:
        """获取文件名"""
        return filename or (os.path.basename(file_path) if file_path else "unknown")


class ParserRegistry:
    """
    解析器注册表（插件化）
    按扩展名注册，支持 magic number 判断
    """
    _instance = None

    def __init__(self):
        self._parsers: Dict[str, DocumentParser] = {}

    @classmethod
    def get_instance(cls) -> "ParserRegistry":
        """获取全局单例（自动触发内置解析器注册）"""
        if cls._instance is None:
            cls._instance = cls()
            # 触发 __init__ 模块中的自动注册
            try:
                import modules.rag.parser  # noqa: F401
            except Exception:
                pass
        return cls._instance

    def _register(self, parser_cls):
        """注册解析器类（内部方法）"""
        try:
            instance = parser_cls()
            for ext in parser_cls.supported_extensions:
                ext_lower = ext.lower()
                self._parsers[ext_lower] = instance
                logger.debug(f"注册解析器 {parser_cls.__name__} → {ext_lower}")
        except Exception as e:
            logger.warning(f"注册解析器 {parser_cls.__name__} 失败（依赖缺失?）: {e}")

    def register(self, parser_cls):
        """装饰器风格注册（供外部插件使用）"""
        self._register(parser_cls)
        return parser_cls

    def get_parser(self, ext: str) -> Optional[DocumentParser]:
        """按扩展名获取解析器"""
        ext_lower = ext.lower()
        if not ext_lower.startswith("."):
            ext_lower = "." + ext_lower
        return self._parsers.get(ext_lower)

    def _detect_by_magic(self, content: bytes) -> Optional[str]:
        """通过 magic number 判断文件类型"""
        if not content or len(content) < 4:
            return None
        if content[:4] == b"%PDF":
            return ".pdf"
        if content[:2] == b"PK":  # Office xlsx/docx 都是 ZIP 格式
            return ".office"
        if content[:8] == b"\x89PNG\r\n\x1a\n":
            return ".png"
        if content[:2] == b"\xff\xd8":  # JPEG
            return ".jpg"
        if content[:2] == b"BM":  # BMP
            return ".bmp"
        return None

    def parse(self, filename: str = None, content: bytes = None,
              file_path: str = None) -> ParsedDocument:
        """
        自动判断文件类型并解析

        Args:
            filename:  文件名（用于扩展名判断）
            content:   文件二进制内容
            file_path: 文件路径
        Returns:
            ParsedDocument
        """
        # 确定内容和文件名
        if content is None and file_path:
            content = self._read_content(file_path=file_path)
        if not filename:
            filename = os.path.basename(file_path) if file_path else "unknown"

        # 1. 优先按扩展名匹配
        ext = os.path.splitext(filename)[1].lower()
        parser = self.get_parser(ext)

        # 2. 扩展名匹配失败 → magic number
        if parser is None and content:
            magic_ext = self._detect_by_magic(content)
            if magic_ext == ".office":
                # 进一步区分 xlsx vs docx（检查 ZIP 内的 [Content_Types].xml）
                # 简单处理：按扩展名再判断
                if ext in (".xlsx", ".xls"):
                    parser = self.get_parser(".xlsx")
                else:
                    parser = self.get_parser(".docx")
            elif magic_ext:
                parser = self.get_parser(magic_ext)

        if parser is None:
            raise ValueError(f"不支持的文件类型: {filename} (ext={ext})")

        logger.info(f"解析文档: {filename} → {parser.__class__.__name__}")
        return parser.parse(file_path=file_path, content=content, filename=filename)
