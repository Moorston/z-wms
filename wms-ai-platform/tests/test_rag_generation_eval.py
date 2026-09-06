"""
测试：R6 — 生成质量评估扩展
覆盖 GenerationEvaluator：Faithfulness + Answer Relevance
"""
import pytest
import os, sys
from unittest.mock import patch, AsyncMock, MagicMock

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


def _make_mock_ai(side_effect=None, return_value=None):
    """创建 mock ai_client，注入到 GenerationEvaluator 构造函数"""
    mock = MagicMock()
    if side_effect is not None:
        mock.llm_extract_json = AsyncMock(side_effect=side_effect)
    elif return_value is not None:
        mock.llm_extract_json = AsyncMock(return_value=return_value)
    else:
        mock.llm_extract_json = AsyncMock(return_value={"score": 0.5, "issues": []})
    return mock


class TestGenerationMetrics:
    """GenerationMetrics 数据类测试"""

    def test_to_dict(self):
        from modules.rag.eval.generation_evaluator import GenerationMetrics

        m = GenerationMetrics(
            faithfulness=0.85,
            answer_relevance=0.9,
            total_samples=10,
            per_sample=[],
        )
        d = m.to_dict()
        assert d["faithfulness"] == 0.85
        assert d["answer_relevance"] == 0.9
        assert d["total_samples"] == 10
        assert "faithfulness" in d
        assert "answer_relevance" in d

    def test_default_values(self):
        from modules.rag.eval.generation_evaluator import GenerationMetrics

        m = GenerationMetrics()
        assert m.faithfulness == 0.0
        assert m.answer_relevance == 0.0
        assert m.total_samples == 0


class TestGenerationEvaluatorEvaluate:
    """GenerationEvaluator 评估逻辑测试"""

    @pytest.mark.asyncio
    async def test_evaluate_faithfulness_good(self):
        """高 faithfulness 答案评估：LLM 返回 score=0.9"""
        from modules.rag.eval.generation_evaluator import GenerationEvaluator

        mock_ai = _make_mock_ai(side_effect=[
            {"score": 0.9, "issues": []},
            {"score": 0.85, "issues": []},
        ])
        evaluator = GenerationEvaluator(ai_client=mock_ai)
        answer = "波次拣货是将多个订单合并为批次进行拣货的策略。"
        context_docs = [{"title": "波次拣货", "content": "波次拣货是将多个订单合并处理"}]

        metrics = await evaluator.evaluate_async([
            {"id": "q1", "question": "什么是波次拣货？",
             "answer": answer, "context_docs": context_docs},
        ])

        assert metrics.total_samples == 1
        assert metrics.faithfulness == 0.9
        assert metrics.answer_relevance == 0.85

    @pytest.mark.asyncio
    async def test_evaluate_faithfulness_bad(self):
        """低 faithfulness 答案：LLM 返回 score=0.2"""
        from modules.rag.eval.generation_evaluator import GenerationEvaluator

        mock_ai = _make_mock_ai(side_effect=[
            {"score": 0.2, "issues": ["机器学习算法无依据"]},
            {"score": 0.3, "issues": []},
        ])
        evaluator = GenerationEvaluator(ai_client=mock_ai)
        answer = "波次拣货是一种先进的机器学习算法。"  # 编造内容
        context_docs = [{"title": "波次拣货", "content": "波次拣货是将多个订单合并处理"}]

        metrics = await evaluator.evaluate_async([
            {"id": "q1", "question": "什么是波次拣货？",
             "answer": answer, "context_docs": context_docs},
        ])

        assert metrics.faithfulness == 0.2

    @pytest.mark.asyncio
    async def test_evaluate_degraded(self):
        """LLM 评估失败降级：返回 0.5 中性分数"""
        from modules.rag.eval.generation_evaluator import GenerationEvaluator

        mock_ai = _make_mock_ai(side_effect=Exception("LLM 超时"))
        evaluator = GenerationEvaluator(ai_client=mock_ai)
        answer = "回答内容"
        context_docs = [{"title": "手册", "content": "内容"}]

        metrics = await evaluator.evaluate_async([
            {"id": "q1", "question": "问题？",
             "answer": answer, "context_docs": context_docs},
        ])

        # 降级时 faithfulness 和 answer_relevance 都是 0.5
        assert metrics.faithfulness == 0.5
        assert metrics.answer_relevance == 0.5

    @pytest.mark.asyncio
    async def test_evaluate_no_context_docs(self):
        """无 context_docs 时 faithfulness 跳过（设为 0.5），answer_relevance 正常评估"""
        from modules.rag.eval.generation_evaluator import GenerationEvaluator

        mock_ai = _make_mock_ai(return_value={"score": 0.8, "issues": []})
        evaluator = GenerationEvaluator(ai_client=mock_ai)
        answer = "暂无相关信息"
        context_docs = []

        metrics = await evaluator.evaluate_async([
            {"id": "q1", "question": "不相关问题",
             "answer": answer, "context_docs": context_docs},
        ])

        # 无文档时 faithfulness 跳过（0.5），answer_relevance 正常评估
        assert metrics.total_samples == 1
        assert metrics.answer_relevance == 0.8

    @pytest.mark.asyncio
    async def test_evaluate_multiple_samples(self):
        """多样本评估：指标为各样本均分"""
        from modules.rag.eval.generation_evaluator import GenerationEvaluator

        mock_ai = _make_mock_ai(side_effect=[
            {"score": 0.8, "issues": []},  # sample1 faithfulness
            {"score": 0.7, "issues": []},  # sample1 relevance
            {"score": 0.6, "issues": []},  # sample2 faithfulness
            {"score": 0.9, "issues": []},  # sample2 relevance
        ])
        evaluator = GenerationEvaluator(ai_client=mock_ai)
        answer1 = "回答1"
        answer2 = "回答2"
        context = [{"title": "手册", "content": "内容"}]

        metrics = await evaluator.evaluate_async([
            {"id": "q1", "question": "问题1", "answer": answer1, "context_docs": context},
            {"id": "q2", "question": "问题2", "answer": answer2, "context_docs": context},
        ])

        assert metrics.total_samples == 2
        # (0.8 + 0.6) / 2 = 0.7
        assert metrics.faithfulness == 0.7
        # (0.7 + 0.9) / 2 = 0.8
        assert metrics.answer_relevance == 0.8

    def test_evaluate_sync_wrapper(self):
        """evaluate() 同步包装器调用 evaluate_async()"""
        from modules.rag.eval.generation_evaluator import GenerationEvaluator

        evaluator = GenerationEvaluator()
        with patch.object(evaluator, "evaluate_async") as mock_async:
            mock_async.return_value = "mocked_metrics"
            result = evaluator.evaluate([])
        assert result == "mocked_metrics"
        mock_async.assert_called_once()


class TestGenerationEvaluatorIntegration:
    """GenerationEvaluator 集成测试"""

    @pytest.mark.asyncio
    async def test_evaluate_with_per_sample(self):
        """评估结果包含逐样本明细"""
        from modules.rag.eval.generation_evaluator import GenerationEvaluator

        mock_ai = _make_mock_ai(return_value={"score": 0.8, "issues": []})
        evaluator = GenerationEvaluator(ai_client=mock_ai)
        context = [{"title": "手册", "content": "内容"}]

        metrics = await evaluator.evaluate_async([
            {"id": "q1", "question": "问题？", "answer": "回答",
             "context_docs": context},
        ])

        assert len(metrics.per_sample) == 1
        ps = metrics.per_sample[0]
        assert ps["id"] == "q1"
        assert "faithfulness" in ps
        assert "answer_relevance" in ps
