"""
RAG 生成质量评估器 — Faithfulness + Answer Relevance
通过 LLM 评估答案与参考文档的一致性和问题相关性

指标定义：
  Faithfulness: 答案中每个要点是否有参考文档支持（0.0-1.0）
  Answer Relevance: 答案是否回答了用户问题（0.0-1.0）

降级安全：LLM 评估失败时返回 0.5 中性分数
"""
import os
import sys
import asyncio
from dataclasses import dataclass, field
from typing import List, Dict

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from loguru import logger

from common.ai_client import ai_client as _ai_client_singleton


@dataclass
class GenerationMetrics:
    """生成质量评估指标"""
    faithfulness: float = 0.0
    answer_relevance: float = 0.0
    total_samples: int = 0
    per_sample: List[Dict] = field(default_factory=list)

    def to_dict(self) -> Dict:
        return {
            "faithfulness": round(self.faithfulness, 4),
            "answer_relevance": round(self.answer_relevance, 4),
            "total_samples": self.total_samples,
        }


class GenerationEvaluator:
    """
    RAG 生成质量评估器

    用法：
        evaluator = GenerationEvaluator()
        metrics = await evaluator.evaluate([
            {"id": "q1", "question": "...", "answer": "...", "context_docs": [...]},
        ])
        print(metrics.to_dict())
    """

    def __init__(self, ai_client=None):
        self._ai_client = ai_client or _ai_client_singleton

    def evaluate(self, samples: List[Dict]) -> GenerationMetrics:
        """同步评估入口（适合 CLI/测试）"""
        return asyncio.run(self.evaluate_async(samples))

    async def evaluate_async(self, samples: List[Dict]) -> GenerationMetrics:
        """
        异步执行生成质量评估

        Args:
            samples: [{"id", "question", "answer", "context_docs": [...]}, ...]

        Returns:
            GenerationMetrics 对象
        """
        if not samples:
            return GenerationMetrics()

        faithfulness_sum = 0.0
        relevance_sum = 0.0
        faithfulness_count = 0
        relevance_count = 0
        per_sample = []

        for sample in samples:
            sid = sample.get("id", "unknown")
            question = sample.get("question", "")
            answer = sample.get("answer", "")
            context_docs = sample.get("context_docs", [])

            # 1. Faithfulness 评估
            faithfulness_score = await self._evaluate_faithfulness(answer, context_docs)

            # 2. Answer Relevance 评估
            relevance_score = await self._evaluate_answer_relevance(question, answer, context_docs)

            faithfulness_sum += faithfulness_score
            relevance_sum += relevance_score
            faithfulness_count += 1
            relevance_count += 1

            per_sample.append({
                "id": sid,
                "question": question[:50],
                "faithfulness": faithfulness_score,
                "answer_relevance": relevance_score,
            })

        n = max(len(samples), 1)
        return GenerationMetrics(
            faithfulness=faithfulness_sum / n,
            answer_relevance=relevance_sum / n,
            total_samples=len(samples),
            per_sample=per_sample,
        )

    async def _evaluate_faithfulness(self, answer: str, context_docs: List[Dict]) -> float:
        """
        评估答案忠实度：答案要点是否有参考文档支持

        无 context_docs 时跳过（返回 0.5 中性分）
        """
        if not context_docs:
            return 0.5

        # 取前 3 个文档避免 token 爆炸
        ref_texts = "\n\n".join(
            f"[参考{i}] {d.get('title', '')}\n{d.get('content', '')}"
            for i, d in enumerate(context_docs[:3], 1)
        )
        prompt = f"""评估以下回答的忠实度：回答中的每个要点是否可以从参考资料中找到依据。

参考资料：
{ref_texts}

回答：
{answer}

请输出JSON格式：
{{"score": 0.0-1.0, "issues": ["无依据的要点1", ...]}}
score 定义：0.0=完全编造, 1.0=完全基于资料
只输出JSON，不要解释。"""

        try:
            result = await self._ai_client.llm_extract_json(prompt)
            score = float(result.get("score", 0.5))
            return max(0.0, min(1.0, score))
        except Exception as e:
            logger.warning(f"Faithfulness 评估失败，降级为 0.5: {e}")
            return 0.5

    async def _evaluate_answer_relevance(self, question: str, answer: str,
                                          context_docs: List[Dict]) -> float:
        """
        评估答案相关性：答案是否回答了用户问题

        即使无 context_docs 也评估（答案可能对问题无关）
        """
        prompt = f"""评估以下回答与问题的相关性：回答是否针对问题进行了有效回答。

问题：
{question}

回答：
{answer}

请输出JSON格式：
{{"score": 0.0-1.0, "issues": ["无关的内容", ...]}}
score 定义：0.0=完全无关, 1.0=精准回答
只输出JSON，不要解释。"""

        try:
            result = await self._ai_client.llm_extract_json(prompt)
            score = float(result.get("score", 0.5))
            return max(0.0, min(1.0, score))
        except Exception as e:
            logger.warning(f"Answer Relevance 评估失败，降级为 0.5: {e}")
            return 0.5
