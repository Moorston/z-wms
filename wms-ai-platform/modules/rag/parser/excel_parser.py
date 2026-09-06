"""
Excel 解析器 — openpyxl sheet + 表格提取
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from typing import List, Dict
from loguru import logger

from modules.rag.parser.base import DocumentParser, ParsedDocument


class ExcelParser(DocumentParser):
    """Excel 文档解析器（openpyxl）"""

    supported_extensions = [".xlsx", ".xls"]

    def parse(self, file_path: str = None, content: bytes = None,
              filename: str = None) -> ParsedDocument:
        doc = ParsedDocument()
        doc.metadata["filename"] = filename or (os.path.basename(file_path) if file_path else "unknown")

        try:
            import openpyxl
        except ImportError:
            logger.warning("openpyxl 未安装，Excel 解析返回空文档")
            doc.metadata["error"] = "openpyxl not installed"
            return doc

        try:
            if file_path:
                wb = openpyxl.load_workbook(file_path, data_only=True)
            else:
                import io
                wb = openpyxl.load_workbook(io.BytesIO(content), data_only=True)

            doc.title = doc.metadata["filename"]

            for sheet_name in wb.sheetnames:
                ws = wb[sheet_name]
                rows = list(ws.iter_rows(values_only=True))

                if not rows:
                    continue

                # 第一行作为 header
                headers = [str(c) if c is not None else "" for c in rows[0]]
                data_rows = []
                for row in rows[1:]:
                    if any(c is not None for c in row):
                        data_rows.append([str(c) if c is not None else "" for c in row])

                # 表格存入 tables
                doc.add_table(headers, data_rows, page=0)

                # 每个 sheet 也存为一个 section（汇总描述）
                summary = f"Sheet: {sheet_name}，共 {len(data_rows)} 行数据，列: {', '.join(headers[:10])}"
                doc.add_section(summary, heading=f"Sheet: {sheet_name}", page=0)

            wb.close()
            doc.metadata["sheet_count"] = len(wb.sheetnames) if hasattr(wb, "sheetnames") else 0
            logger.info(f"Excel 解析完成: {filename}, {len(doc.tables)}表, {doc.total_chars}字符")
        except Exception as e:
            logger.error(f"Excel 解析失败: {e}")
            doc.metadata["error"] = str(e)

        return doc
