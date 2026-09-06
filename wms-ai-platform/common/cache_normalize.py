"""
WMS AI Platform - Prompt 标准化
对不同 task_type 的 prompt 做归一化处理，提升精确缓存命中率
- 去首尾空白 + 压缩多余空格
- 全角标点→半角
- RAG 场景去语气词（前缀/后缀）
- NL2SQL 场景英文小写
"""
import re

# 全角数字→半角数字
_FULLWIDTH_DIGITS = str.maketrans("０１２３４５６７８９", "0123456789")

# RAG 问答场景语气词前缀模式
_PREFIX_WORDS = re.compile(r"^(请问|麻烦您?|我想知道|能不能|怎么样|如何)")

# RAG 问答场景语气词后缀模式
_SUFFIX_WORDS = re.compile(r"(呢|啊|吧|呀|吗|哈|哦|哎|哟)?$")


def normalize_prompt(prompt: str, task_type: str = "rag") -> str:
    """
    Prompt 标准化：去空白/统一标点/去语气词，提升精确缓存命中率

    Args:
        prompt:     原始 prompt
        task_type:  任务类型（rag / nl2sql / llm / ocr / ...），不同类型有不同归一化策略

    Returns:
        标准化后的 prompt
    """
    if not prompt:
        return ""

    # 1. 去首尾空白 + 压缩多余空格（含全角空格）
    prompt = prompt.replace("　", " ")
    prompt = re.sub(r"\s+", " ", prompt.strip())

    # 2. 全角标点→半角（常见中文标点）
    prompt = (
        prompt.replace("？", "?")
        .replace("，", ",")
        .replace("。", ".")
        .replace("：", ":")
        .replace("；", ";")
        .replace("（", "(")
        .replace("）", ")")
        .replace("「", '"')
        .replace("」", '"')
        .replace("『", '"')
        .replace("』", '"')
        .replace("“", '"')
        .replace("”", '"')
        .replace("‘", "'")
        .replace("’", "'")
    )
    # 全角数字→半角
    prompt = prompt.translate(_FULLWIDTH_DIGITS)

    # 3. RAG 场景去语气词（前缀 + 后缀，循环去除前缀以应对"请问麻烦问一下"叠加）
    if task_type.startswith("rag"):
        # 循环去前缀（最多3轮，避免死循环）
        for _ in range(3):
            new_prompt = _PREFIX_WORDS.sub("", prompt)
            if new_prompt == prompt:
                break
            prompt = new_prompt
        # 去后缀
        prompt = _SUFFIX_WORDS.sub("", prompt)

    # 4. NL2SQL 场景英文小写（SQL 关键字大小写无关）
    if task_type == "nl2sql":
        prompt = prompt.lower()

    return prompt.strip()
