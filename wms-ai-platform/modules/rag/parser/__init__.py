"""
WMS AI Platform - RAG 文档解析引擎
插件化解析器，支持 PDF/Word/Excel/Markdown/图片 5 种格式
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from modules.rag.parser.base import ParserRegistry, ParsedDocument, DocumentParser

# 自动注册内置解析器（延迟注册，避免依赖缺失时 import 失败）
def _register_builtin():
    """注册所有内置解析器到 ParserRegistry 单例"""
    registry = ParserRegistry._instance
    if registry is None:
        registry = ParserRegistry()
        ParserRegistry._instance = registry

    # Markdown（无第三方依赖，优先注册）
    try:
        from modules.rag.parser.markdown_parser import MarkdownParser
        registry._register(MarkdownParser)
    except Exception:
        pass

    # PDF
    try:
        from modules.rag.parser.pdf_parser import PdfParser
        registry._register(PdfParser)
    except Exception:
        pass

    # Word
    try:
        from modules.rag.parser.word_parser import WordParser
        registry._register(WordParser)
    except Exception:
        pass

    # Excel
    try:
        from modules.rag.parser.excel_parser import ExcelParser
        registry._register(ExcelParser)
    except Exception:
        pass

    # Image
    try:
        from modules.rag.parser.image_parser import ImageParser
        registry._register(ImageParser)
    except Exception:
        pass


# 模块加载时自动注册
_register_builtin()

__all__ = ["ParserRegistry", "ParsedDocument", "DocumentParser"]
