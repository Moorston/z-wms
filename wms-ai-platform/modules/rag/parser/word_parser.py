"""
Word 解析器 — python-docx 段落 + 表格提取
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from typing import List, Dict
from loguru import logger

from modules.rag.parser.base import DocumentParser, ParsedDocument


class WordParser(DocumentParser):
    """Word 文档解析器（python-docx）"""

    supported_extensions = [".docx"]

    def parse(self, file_path: str = None, content: bytes = None,
              filename: str = None) -> ParsedDocument:
        doc = ParsedDocument()
        doc.metadata["filename"] = filename or (os.path.basename(file_path) if file_path else "unknown")

        try:
            import docx
        except ImportError:
            logger.warning("python-docx 未安装，Word 解析返回空文档")
            doc.metadata["error"] = "python-docx not installed"
            return doc

        try:
            if file_path:
                d = docx.Document(file_path)
            else:
                import io
                d = docx.Document(io.BytesIO(content))

            doc.title = doc.metadata["filename"]

            # 段落提取（检测标题样式）
            current_heading = ""
            current_text = []
            page = 1  # docx 无页码概念，用段落数近似

            for para in d.paragraphs:
                text = para.text.strip()
                if not text:
                    continue

                # 判断是否标题样式
                if para.style and para.style.name and "Heading" in para.style.name:
                    # 先保存前一段内容
                    if current_text:
                        doc.add_section("\n".join(current_text), heading=current_heading, page=page)
                        current_text = []
                    current_heading = text
                    doc.add_section(text, heading=current_heading, page=page)
                    page += 1
                else:
                    current_text.append(text)

            # 保存最后一段
            if current_text:
                doc.add_section("\n".join(current_text), heading=current_heading, page=page)

            # 表格提取
            for table in d.tables:
                if not table.rows:
                    continue
                headers = [cell.text.strip() for cell in table.rows[0].cells]
                rows = []
                for row in table.rows[1:]:
                    rows.append([cell.text.strip() for cell in row.cells])
                doc.add_table(headers, rows, page=page)

            doc.metadata["paragraph_count"] = len(d.paragraphs)
            doc.metadata["table_count"] = len(d.tables)
            logger.info(f"Word 解析完成: {filename}, {len(doc.sections)}段, {len(doc.tables)}表, {doc.total_chars}字符")
        except Exception as e:
            logger.error(f"Word 解析失败: {e}")
            doc.metadata["error"] = str(e)

        return doc
