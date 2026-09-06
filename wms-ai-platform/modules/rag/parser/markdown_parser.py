"""
Markdown 解析器 — 按标题结构化解析（纯文本 .txt 也走此解析器）
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

import re
from typing import List, Dict
from loguru import logger

from modules.rag.parser.base import DocumentParser, ParsedDocument


class MarkdownParser(DocumentParser):
    """Markdown/纯文本解析器（按标题结构化）"""

    supported_extensions = [".md", ".markdown", ".txt"]

    def parse(self, file_path: str = None, content: bytes = None,
              filename: str = None) -> ParsedDocument:
        doc = ParsedDocument()
        doc.metadata["filename"] = filename or (os.path.basename(file_path) if file_path else "unknown")

        try:
            raw = self._read_content(file_path, content)
            text = raw.decode("utf-8", errors="ignore")
        except Exception as e:
            logger.error(f"Markdown 解析读取失败: {e}")
            doc.metadata["error"] = str(e)
            return doc

        doc.title = doc.metadata["filename"]

        # 按标题 (# ## ###) 分块
        lines = text.split("\n")
        current_heading = ""
        current_lines: List[str] = []
        page = 1

        def flush():
            nonlocal current_heading, current_lines
            if current_lines:
                section_text = "\n".join(current_lines).strip()
                if section_text:
                    doc.add_section(section_text, heading=current_heading, page=page)
                    page += 1
            current_lines = []

        heading_pattern = re.compile(r"^(#{1,6})\s+(.+)$")

        for line in lines:
            m = heading_pattern.match(line)
            if m:
                flush()
                current_heading = m.group(2).strip()
                # 标题本身也作为一个 section
                doc.add_section(line, heading=current_heading, page=page)
                page += 1
                current_lines = []
            else:
                current_lines.append(line)

        flush()

        # 如果没有任何标题结构，整篇作为一个 section
        if not doc.sections and text.strip():
            doc.add_section(text.strip(), heading="", page=1)

        logger.info(f"Markdown 解析完成: {filename}, {len(doc.sections)}段, {doc.total_chars}字符")
        return doc
