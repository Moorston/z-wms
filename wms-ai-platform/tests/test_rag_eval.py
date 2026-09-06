"""
测试：RAG 检索质量评估体系
验证 RetrievalEvaluator 指标计算正确性
"""
import pytest
import json
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from modules.rag.eval.evaluator import (
    RetrievalEvaluator,
    QAItem,
    EvalMetrics,
)


class TestEvalDataset:
    """评估数据集格式测试"""

    def test_dataset_exists(self):
        """评估数据集文件存在"""
        path = os.path.join(
            os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
            "tests", "data", "rag_eval_dataset.json",
        )
        assert os.path.exists(path), f"评估数据集不存在: {path}"

    def test_dataset_format(self):
        """评估数据集格式正确"""
        path = os.path.join(
            os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
            "tests", "data", "rag_eval_dataset.json",
        )
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
        assert "qa_pairs" in data
        assert len(data["qa_pairs"]) >= 20, f"样本数不足 20: {len(data['qa_pairs'])}"

        for pair in data["qa_pairs"]:
            assert "id" in pair
            assert "question" in pair
            assert "gold_doc_ids" in pair
            assert len(pair["gold_doc_ids"]) >= 1


class TestMetricsComputation:
    """指标计算正确性测试"""

    def _make_evaluator(self):
        """创建无真实 retriever 的 evaluator（仅测试指标计算）"""
        return RetrievalEvaluator(retriever=None)

    def test_recall_at_1_hit(self):
        """Recall@1：Top-1 命中返回 1.0"""
        item = QAItem(id="t1", question="test", gold_doc_ids=["doc_a"])
        retrieved = ["doc_a", "doc_b", "doc_c"]
        r1, r3, r5, rr = RetrievalEvaluator._compute_metrics(item, retrieved)
        assert r1 == 1.0
        assert rr == 1.0

    def test_recall_at_3_hit(self):
        """Recall@3：Top-3 内命中返回 1.0"""
        item = QAItem(id="t2", question="test", gold_doc_ids=["doc_b"])
        retrieved = ["doc_a", "doc_b", "doc_c"]
        r1, r3, r5, rr = RetrievalEvaluator._compute_metrics(item, retrieved)
        assert r1 == 0.0
        assert r3 == 1.0
        assert rr == 0.5  # 1/2

    def test_recall_at_5_hit(self):
        """Recall@5：Top-5 内命中返回 1.0"""
        item = QAItem(id="t3", question="test", gold_doc_ids=["doc_c"])
        retrieved = ["doc_a", "doc_b", "doc_c"]
        r1, r3, r5, rr = RetrievalEvaluator._compute_metrics(item, retrieved)
        assert r1 == 0.0
        assert r3 == 1.0  # doc_c 在 rank 3，属于 Top-3
        assert r5 == 1.0
        assert rr == 1.0 / 3.0

    def test_no_hit(self):
        """未命中返回全 0"""
        item = QAItem(id="t4", question="test", gold_doc_ids=["doc_z"])
        retrieved = ["doc_a", "doc_b", "doc_c"]
        r1, r3, r5, rr = RetrievalEvaluator._compute_metrics(item, retrieved)
        assert r1 == 0.0 and r3 == 0.0 and r5 == 0.0 and rr == 0.0

    def test_empty_results(self):
        """空结果返回全 0"""
        item = QAItem(id="t5", question="test", gold_doc_ids=["doc_a"])
        r1, r3, r5, rr = RetrievalEvaluator._compute_metrics(item, [])
        assert r1 == 0.0 and r3 == 0.0 and r5 == 0.0 and rr == 0.0

    def test_first_match_wins(self):
        """多个命中时取最早排名"""
        item = QAItem(id="t6", question="test", gold_doc_ids=["doc_a", "doc_b"])
        retrieved = ["doc_a", "doc_b", "doc_c"]
        r1, r3, r5, rr = RetrievalEvaluator._compute_metrics(item, retrieved)
        assert r1 == 1.0  # doc_a 在 Top-1
        assert rr == 1.0  # 第一个命中是 rank 1

    def test_eval_metrics_serialization(self):
        """EvalMetrics 可序列化为字典"""
        m = EvalMetrics(
            recall_at_1=0.8, recall_at_3=0.95, recall_at_5=1.0,
            mrr=0.75, total_samples=20,
        )
        d = m.to_dict()
        assert d["recall_at_1"] == 0.8
        assert d["recall_at_3"] == 0.95
        assert d["recall_at_5"] == 1.0
        assert d["mrr"] == 0.75
        assert d["total_samples"] == 20
