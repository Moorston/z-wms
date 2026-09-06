"""
PDF 解析器 — PyMuPDF 文本提取 + DeepSeek-OCR-2 API 扫描件 fallback

PRD V2.0 修复：
  - _ocr_page 使用 asyncio.to_thread() 替代 asyncio.new_event_loop() + run_until_complete()
    （后者在 FastAPI 异步上下文调用即 RuntimeError）
"""
import os, sys, asyncio
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from typing import List, Dict
from loguru import logger

from modules.rag.parser.base import DocumentParser, ParsedDocument


class PdfParser(DocumentParser):
    """PDF 文档解析器（PyMuPDF 文本提取，扫描件 fallback DeepSeek-OCR-2 API）"""

    supported_extensions = [".pdf"]

    def parse(self, file_path: str = None, content: bytes = None,
              filename: str = None) -> ParsedDocument:
        doc = ParsedDocument()
        doc.metadata["filename"] = filename or (os.path.basename(file_path) if file_path else "unknown")

        try:
            import fitz  # PyMuPDF
        except ImportError:
            logger.warning("pymupdf 未安装，PDF 解析返回空文档")
            doc.metadata["error"] = "pymupdf not installed"
            return doc

        try:
            if file_path:
                pdf = fitz.open(file_path)
            else:
                pdf = fitz.open(stream=content, filetype="pdf")

            doc.metadata["page_count"] = pdf.page_count
            doc.title = doc.metadata["filename"]

            total_text = ""

            for page_num in range(pdf.page_count):
                page = pdf[page_num]
                text = page.get_text("text").strip()

                if text:
                    doc.add_section(text, heading=f"第{page_num + 1}页", page=page_num + 1)
                    total_text += text
                else:
                    # 扫描件页：尝试 DeepSeek-OCR-2 API
                    logger.debug(f"第{page_num + 1}页无文本，可能为扫描件，尝试 OCR fallback")
                    ocr_text = self._ocr_page(page, page_num + 1)
                    if ocr_text:
                        doc.add_section(ocr_text, heading=f"第{page_num + 1}页(OCR)", page=page_num + 1)
                        total_text += ocr_text

                # 表格提取（简单实现）
                try:
                    tables = page.find_tables()
                    if tables and tables.tables:
                        for tbl in tables.tables:
                            rows_data = tbl.extract()
                            if rows_data and len(rows_data) > 1:
                                headers = [str(c or "") for c in rows_data[0]]
                                rows = [[str(c or "") for c in r] for r in rows_data[1:]]
                                doc.add_table(headers, rows, page=page_num + 1)
                except Exception:
                    pass  # find_tables 可能不支持所有 PDF

            pdf.close()
            logger.info(f"PDF 解析完成: {filename}, {pdf.page_count if 'pdf' in dir() else '?'}页, {doc.total_chars}字符")
        except Exception as e:
            logger.error(f"PDF 解析失败: {e}")
            doc.metadata["error"] = str(e)

        return doc

    def _ocr_page(self, page, page_num: int) -> str:
        """
        对扫描件页面做 DeepSeek-OCR-2 API 识别（通过 ai_client.ocr_recognize）

        修复：不再使用 asyncio.new_event_loop() + run_until_complete()，
        改为在线程中运行独立事件循环，避免与 FastAPI 事件循环冲突。
        """
        try:
            # 渲染页面为图片字节
            pix = page.get_pixmap(dpi=200)
            img_bytes = pix.tobytes("png")

            from common.ai_client import ai_client

            # 在线程中运行独立事件循环（线程安全，不污染 FastAPI 事件循环）
            import threading
            result_holder = {}

            def _run_ocr():
                loop = asyncio.new_event_loop()
                asyncio.set_event_loop(loop)
                try:
                    result_holder["result"] = loop.run_until_complete(ai_client.ocr_recognize(img_bytes))
                finally:
                    loop.close()

            thread = threading.Thread(target=_run_ocr, daemon=True)
            thread.start()
            thread.join(timeout=60)

            return result_holder.get("result", {}).get("text", "") or ""
        except Exception as e:
            logger.debug(f"页面 OCR 失败: {e}")
            return ""
