"""
LLM 答案验证器 — RAG 生成答案的 Self-Check 验证

单次 LLM 验证 + 置信度融合（retrieval*0.6 + verification*0.4）
降级安全：LLM 失败时返回原始 retrieval_confidence
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from typing import List, Dict
from loguru import logger

from common.ai_client import ai_client as _ai_client_singleton


class LLMVerifier:
    """LLM 答案验证器（单次验证，降级安全）"""

    def __init__(self, ai_client=None):
        # 允许注入 ai_client（测试用），默认用全局单例
        self._ai_client = ai_client or _ai_client_singleton

    async def verify(self, answer: str, context_docs: List[Dict],
                     retrieval_confidence: float) -> Dict:
        """
        验证答案是否基于参考文档

        Args:
            answer:                LLM 生成的答案
            context_docs:          检索到的参考文档
            retrieval_confidence:  检索阶段置信度（基于相关性分数）

        Returns:
            {
                "valid": bool,
                "issues": List[str],          # 无法从参考文档找到依据的要点
                "confidence_adjusted": float,  # 融合后的置信度
            }
        """
        if not context_docs:
            return {
                "valid": True,
                "issues": [],
                "confidence_adjusted": retrieval_confidence,
            }

        # 构建验证 prompt（取前 3 个文档避免 token 爆炸）
        ref_texts = "\n\n".join(
            f"[参考{i}] {d.get('title', '')}\n{d.get('content', '')}"
            for i, d in enumerate(context_docs[:3], 1)
        )
        prompt = f"""验证以下回答是否可以从参考资料中找到依据。
逐条检查回答中的每个要点，判断是否有参考资料支持。

参考资料：
{ref_texts}

回答：
{answer}

请输出JSON格式：
{{"valid": true/false, "issues": ["无法找到依据的要点1", ...], "score": 0.0-1.0}}
只输出JSON，不要解释。"""

        try:
            result = await self._ai_client.llm_extract_json(prompt)
            valid = result.get("valid", True)
            issues = result.get("issues", [])
            verification_score = float(result.get("score", 0.5))
            # 融合置信度：retrieval 60% + verification 40%
            confidence_adjusted = round(
                retrieval_confidence * 0.6 + verification_score * 0.4, 4
            )
            return {
                "valid": valid,
                "issues": issues,
                "confidence_adjusted": confidence_adjusted,
            }
        except Exception as e:
            logger.warning(f"答案验证失败，降级跳过: {e}")
            return {
                "valid": True,
                "issues": [],
                "confidence_adjusted": retrieval_confidence,
            }
