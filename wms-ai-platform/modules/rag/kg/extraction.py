"""
WMS AI Platform - 知识图谱实体关系抽取模块
混合模式：正则+词典（标准格式）+ LLM 补充（非结构化语义关系）
"""
import re
import os
import sys
from dataclasses import dataclass, field
from typing import List, Tuple, Dict

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from loguru import logger


@dataclass
class Triple:
    """知识图谱三元组"""
    subject: str
    subject_type: str
    predicate: str
    object: str
    object_type: str
    confidence: float = 1.0
    source_span: str = ""
    doc_id: str = ""


# ========== WMS 实体正则模式 ==========
# (类型标签, 正则表达式, 标准中文类型名)
_ENTITY_PATTERNS = [
    ("SKU",    r'\bSKU[A-Z0-9]+',        "商品"),
    ("仓库",    r'\bWH\d{2,4}',            "仓库"),
    ("波次",    r'\bWAVE\d{4,8}',          "波次"),
    ("批次",    r'\bB\d{8}',               "批次"),
    ("订单",    r'\bORD\d{8,12}',          "订单"),
    ("库位",    r'\b[A-Z]\d{2,4}\b',       "库位"),
]

# ========== 关系模板 ==========
# (中文关系, 英文/缩写)
_RELATION_MAP = {
    "属于": "belongs_to",
    "包含": "contains",
    "位于": "located_in",
    "经过": "passes_through",
    "关联": "related_to",
    "上架": "putaway",
    "拣货": "picking",
    "盘点": "counting",
    "入库": "inbound",
    "出库": "outbound",
}


def extract_entities(text: str) -> List[Tuple[str, str]]:
    """
    查询时实体识别：纯正则+词典匹配
    返回 [(entity_name, entity_type), ...]
    """
    entities = []
    for label, pattern, cn_type in _ENTITY_PATTERNS:
        for m in re.finditer(pattern, text):
            entities.append((m.group(), cn_type))
    return entities


def _extract_by_regex(content: str) -> List[Triple]:
    """
    正则+词典抽取：从标准格式文本中提取实体关系三元组
    策略：识别同一句中出现的多个实体，按位置推断关系
    """
    triples = []

    # 按行分割，逐行分析（行级关系最可靠）
    for line in content.split('\n'):
        if len(line.strip()) < 4:
            continue

        # 提取该行中所有实体
        line_entities = []
        for label, pattern, cn_type in _ENTITY_PATTERNS:
            for m in re.finditer(pattern, line):
                line_entities.append((m.group(), cn_type, m.start()))

        if len(line_entities) < 2:
            continue

        # 按位置排序
        line_entities.sort(key=lambda x: x[2])

        # 推断相邻实体之间的关系
        for i in range(len(line_entities) - 1):
            subj_name, subj_type = line_entities[i][0], line_entities[i][1]
            obj_name, obj_type = line_entities[i + 1][0], line_entities[i + 1][1]

            # 根据类型组合推断关系
            predicate = _infer_predicate(subj_type, obj_type, line)

            triples.append(Triple(
                subject=subj_name,
                subject_type=subj_type,
                predicate=predicate,
                object=obj_name,
                object_type=obj_type,
                confidence=0.9,
                source_span=line.strip()[:200],
            ))

    return triples


def _infer_predicate(subject_type: str, object_type: str, context: str) -> str:
    """
    根据实体类型组合和上下文推断关系
    """
    # 类型组合规则
    type_rules = {
        ("商品", "仓库"): "位于",
        ("商品", "库位"): "位于",
        ("商品", "批次"): "属于",
        ("批次", "商品"): "包含",
        ("波次", "商品"): "包含",
        ("波次", "仓库"): "属于",
        ("波次", "库位"): "包含",
        ("订单", "商品"): "包含",
        ("订单", "仓库"): "属于",
        ("订单", "波次"): "属于",
        ("商品", "波次"): "经过",
        ("库位", "仓库"): "属于",
    }

    key = (subject_type, object_type)
    if key in type_rules:
        return type_rules[key]

    # 反向规则
    rev_key = (object_type, subject_type)
    if rev_key in type_rules:
        return f"反向_{type_rules[rev_key]}"

    # 从上下文推断
    if "上架" in context:
        return "上架"
    if "拣货" in context:
        return "拣货"
    if "入库" in context:
        return "入库"
    if "出库" in context:
        return "出库"
    if "盘点" in context:
        return "盘点"

    return "关联"


def _extract_by_llm(content: str, doc_id: str = "") -> List[Triple]:
    """
    LLM 补充抽取：提取非结构化文本中的语义关系
    失败时返回空列表（降级安全）
    """
    from common.config import settings
    if not settings.rag_kg_extract_llm:
        return []

    # 限制发送给 LLM 的文本长度
    truncated = content[:2000]

    prompt = f"""你是 WMS（仓库管理系统）领域的知识图谱抽取助手。
请从以下文本中提取所有实体关系三元组。

文本：
{truncated}

要求：
1. 识别的实体类型限定为：商品(SKU)、仓库、库位、波次、订单、批次、拣货员
2. 关系使用中文动词：属于/包含/位于/经过/上架/拣货/盘点/入库/出库/关联
3. 每条三元组包含置信度（0-1）和原文片段（source_span）
4. 只返回 JSON，不要任何解释

输出格式（严格 JSON）：
{{"triples": [{{"subject": "实体名", "subject_type": "类型", "predicate": "关系", "object": "实体名", "object_type": "类型", "confidence": 0.85, "source_span": "原文片段"}}]}}"""

    schema = {"triples": "List[Dict]"}
    try:
        from common.ai_client import ai_client
        result = ai_client.llm_extract_json(prompt, schema)

        # 检查解析失败
        if isinstance(result, dict) and result.get("parse_error"):
            logger.warning(f"KG LLM 抽取解析失败: {result.get('parse_error')}")
            return []

        # 提取三元组列表
        raw_triples = []
        if isinstance(result, dict):
            raw_triples = result.get("triples", [])
            if not raw_triples:
                # 尝试从 raw 字段解析
                raw_data = result.get("raw")
                if raw_data:
                    import json
                    try:
                        parsed = json.loads(raw_data) if isinstance(raw_data, str) else raw_data
                        raw_triples = parsed.get("triples", [])
                    except Exception:
                        pass

        triples = []
        for t in raw_triples:
            if not isinstance(t, dict):
                continue
            subject = t.get("subject", "")
            obj = t.get("object", "")
            predicate = t.get("predicate", "")
            if not subject or not obj or not predicate:
                continue
            triples.append(Triple(
                subject=str(subject),
                subject_type=str(t.get("subject_type", "")),
                predicate=str(predicate),
                object=str(obj),
                object_type=str(t.get("object_type", "")),
                confidence=float(t.get("confidence", 0.7)),
                source_span=str(t.get("source_span", "")),
                doc_id=doc_id,
            ))

        logger.info(f"KG LLM 抽取完成: doc_id={doc_id}, triples={len(triples)}")
        return triples

    except Exception as e:
        logger.warning(f"KG LLM 抽取异常（降级跳过）: {e}")
        return []


def extract_triples(content: str, doc_id: str = "") -> List[Triple]:
    """
    实体关系抽取主入口
    混合模式：正则+词典 + LLM 补充 → 合并去重 → 置信度过滤
    """
    # 第一步：正则抽取
    regex_triples = _extract_by_regex(content)

    # 第二步：LLM 补充抽取
    llm_triples = _extract_by_llm(content, doc_id)

    # 第三步：合并 + 去重
    all_triples = regex_triples + llm_triples
    seen = set()
    deduped = []
    for t in all_triples:
        t.doc_id = doc_id
        key = (t.subject, t.predicate, t.object)
        if key not in seen:
            seen.add(key)
            deduped.append(t)

    # 第四步：置信度过滤
    from common.config import settings
    min_conf = settings.rag_kg_min_confidence
    filtered = [t for t in deduped if t.confidence >= min_conf]

    logger.info(
        f"KG 抽取完成: doc_id={doc_id}, "
        f"regex={len(regex_triples)}, llm={len(llm_triples)}, "
        f"deduped={len(deduped)}, filtered={len(filtered)}"
    )
    return filtered
