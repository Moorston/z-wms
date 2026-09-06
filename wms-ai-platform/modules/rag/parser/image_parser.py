"""
图片解析器 — DeepSeek-OCR-2 API 文字识别

PRD V2.0 修复：
  - 使用线程 + 独立事件循环替代 asyncio.new_event_loop() + run_until_complete()
    （后者在 FastAPI 异步上下文调用即 RuntimeError）
"""
import os, sys, asyncio
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from typing import List, Dict
from loguru import logger

from modules.rag.parser.base import DocumentParser, ParsedDocument


class ImageParser(DocumentParser):
    """图片解析器（DeepSeek-OCR-2 API，通过 ai_client.ocr_recognize 调用）"""

    supported_extensions = [".png", ".jpg", ".jpeg", ".bmp", ".tiff"]

    def parse(self, file_path: str = None, content: bytes = None,
              filename: str = None) -> ParsedDocument:
        doc = ParsedDocument()
        doc.metadata["filename"] = filename or (os.path.basename(file_path) if file_path else "unknown")

        # 读取图片字节
        if file_path:
            try:
                with open(file_path, "rb") as f:
                    content = f.read()
            except Exception as e:
                logger.error(f"图片读取失败: {e}")
                doc.metadata["error"] = str(e)
                return doc
        elif content is None:
            doc.metadata["error"] = "无图片数据"
            return doc

        try:
            from common.ai_client import ai_client

            # 在线程中运行独立事件循环（线程安全，不污染 FastAPI 事件循环）
            import threading
            result_holder = {}

            def _run_ocr():
                loop = asyncio.new_event_loop()
                asyncio.set_event_loop(loop)
                try:
                    result_holder["result"] = loop.run_until_complete(ai_client.ocr_recognize(content))
                finally:
                    loop.close()

            thread = threading.Thread(target=_run_ocr, daemon=True)
            thread.start()
            thread.join(timeout=60)

            result = result_holder.get("result", {})
            full_text = result.get("text", "") or ""
            if full_text.strip():
                doc.add_section(full_text, heading="OCR识别结果", page=1)

            doc.title = doc.metadata["filename"]
            logger.info(f"图片解析完成: {filename}, {doc.total_chars}字符")
        except Exception as e:
            logger.error(f"图片解析失败: {e}")
            doc.metadata["error"] = str(e)

        return doc
