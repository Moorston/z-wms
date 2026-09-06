"""
RAG 检索质量评估器 — Recall@K + MRR
基于手工标注 QA 数据集评估检索召回质量

指标定义：
  Recall@K: Top-K 检索结果中包含相关文档的比例
  MRR (Mean Reciprocal Rank): 1/首个相关文档排名 的平均值（最高 1.0，最低 0.0）
"""
import asyncio
import json
import os
from dataclasses import dataclass, field
from typing import List, Dict, Optional, Callable

import sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from loguru import logger


@dataclass
class QAItem:
    """评估数据集中的单条 QA 样本"""
    id: str          # 唯一标识
    question: str    # 问题
    gold_doc_ids: List[str]   # 相关文档 ID 列表（至少 1 个）
    gold_chunk_ids: List[int] = field(default_factory=list)  # 可选，相关 chunk 索引


@dataclass
class EvalMetrics:
    """评估结果指标"""
    recall_at_1: float = 0.0
    recall_at_3: float = 0.0
    recall_at_5: float = 0.0
    mrr: float = 0.0
    total_samples: int = 0
    per_sample: List[Dict] = field(default_factory=list)

    def to_dict(self) -> Dict:
        return {
            "recall_at_1": round(self.recall_at_1, 4),
            "recall_at_3": round(self.recall_at_3, 4),
            "recall_at_5": round(self.recall_at_5, 4),
            "mrr": round(self.mrr, 4),
            "total_samples": self.total_samples,
        }


class RetrievalEvaluator:
    """
    检索质量评估器

    用法：
        evaluator = RetrievalEvaluator(retriever=retriever_instance)
        metrics = evaluator.evaluate()
        print(metrics.to_dict())
    """

    def __init__(self, retriever=None, dataset_path: str = None):
        self._retriever = retriever
        self._dataset_path = dataset_path or self._default_dataset_path()

    @staticmethod
    def _default_dataset_path() -> str:
        base = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
        return os.path.join(base, "tests", "data", "rag_eval_dataset.json")

    def load_dataset(self, path: str = None) -> List[QAItem]:
        """加载 JSON 格式评估数据集"""
        path = path or self._dataset_path
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
        items = []
        for entry in data.get("qa_pairs", []):
            items.append(QAItem(
                id=entry["id"],
                question=entry["question"],
                gold_doc_ids=entry.get("gold_doc_ids", []),
                gold_chunk_ids=entry.get("gold_chunk_ids", []),
            ))
        logger.info(f"加载评估数据集: {len(items)} 条样本 from {path}")
        return items

    def evaluate(self, dataset: List[QAItem] = None, top_k: int = 5) -> EvalMetrics:
        """
        执行评估（同步版本，适合 CLI / 测试调用）

        Args:
            dataset:  评估数据集，None 则自动加载
            top_k:    每轮检索返回数量
        Returns:
            EvalMetrics 对象
        """
        return asyncio.run(self.evaluate_async(dataset, top_k))

    async def evaluate_async(self, dataset: List[QAItem] = None, top_k: int = 5) -> EvalMetrics:
        """
        异步执行评估

        Args:
            dataset:  评估数据集
            top_k:    每轮检索返回数量
        Returns:
            EvalMetrics 对象
        """
        if dataset is None:
            dataset = self.load_dataset()

        if self._retriever is None:
            raise ValueError("RetrievalEvaluator 需要传入 retriever 实例")

        recall_at_1 = recall_at_3 = recall_at_5 = 0.0
        mrr_sum = 0.0
        per_sample = []

        for item in dataset:
            try:
                results = await self._retriever.retrieve(item.question, top_k=top_k)
            except Exception as e:
                logger.warning(f"样本 {item.id} 检索失败: {e}")
                results = []

            # 提取返回文档的 doc_id 集合
            retrieved_doc_ids = [r.get("doc_id", "") for r in results]

            # Recall@K 计算
            r1, r3, r5, rr = self._compute_metrics(item, retrieved_doc_ids)
            recall_at_1 += r1
            recall_at_3 += r3
            recall_at_5 += r5
            mrr_sum += rr

            per_sample.append({
                "id": item.id,
                "question": item.question[:50],
                "recall@1": r1, "recall@3": r3, "recall@5": r5,
                "rr": rr,
                "retrieved_doc_ids": retrieved_doc_ids,
            })

        n = max(len(dataset), 1)
        return EvalMetrics(
            recall_at_1=recall_at_1 / n,
            recall_at_3=recall_at_3 / n,
            recall_at_5=recall_at_5 / n,
            mrr=mrr_sum / n,
            total_samples=len(dataset),
            per_sample=per_sample,
        )

    @staticmethod
    def _compute_metrics(item: QAItem, retrieved_doc_ids: List[str]) -> tuple:
        """
        计算单样本指标

        Returns:
            (recall_at_1, recall_at_3, recall_at_5, reciprocal_rank)
        """
        gold_set = set(item.gold_doc_ids)
        first_hit_rank = None

        for k, doc_id in enumerate(retrieved_doc_ids, start=1):
            if doc_id in gold_set:
                first_hit_rank = k
                break

        if first_hit_rank is None:
            return 0.0, 0.0, 0.0, 0.0

        rr = 1.0 / first_hit_rank
        r1 = 1.0 if first_hit_rank <= 1 else 0.0
        r3 = 1.0 if first_hit_rank <= 3 else 0.0
        r5 = 1.0 if first_hit_rank <= 5 else 0.0
        return r1, r3, r5, rr


# ========== CLI 入口 ==========

def run_evaluation(dataset_path: str = None, top_k: int = 5, verbose: bool = True):
    """
    CLI 评估入口

    用法:
        python -m modules.rag.eval.evaluator [--dataset path] [--top-k 5]

    Returns:
        EvalMetrics 对象（或 None 如果无法初始化 retriever）
    """
    try:
        from modules.rag.retriever import HybridRetriever
        retriever = HybridRetriever()
    except Exception as e:
        logger.error(f"无法初始化 HybridRetriever: {e}")
        return None

    evaluator = RetrievalEvaluator(retriever=retriever, dataset_path=dataset_path)
    metrics = evaluator.evaluate(top_k=top_k)

    if verbose:
        print("\n" + "=" * 60)
        print("RAG 检索质量评估结果")
        print("=" * 60)
        print(f"  样本总数:   {metrics.total_samples}")
        print(f"  Recall@1:   {metrics.recall_at_1:.4f}")
        print(f"  Recall@3:   {metrics.recall_at_3:.4f}")
        print(f"  Recall@5:   {metrics.recall_at_5:.4f}")
        print(f"  MRR:        {metrics.mrr:.4f}")
        print("=" * 60)

        if metrics.per_sample:
            print("\n逐样本明细:")
            print(f"  {'ID':<8} {'Recall@1':>9} {'Recall@3':>9} {'Recall@5':>9} {'RR':>7}  问题")
            print("  " + "-" * 70)
            for s in metrics.per_sample:
                print(f"  {s['id']:<8} {s['recall@1']:>9.4f} {s['recall@3']:>9.4f} "
                      f"{s['recall@5']:>9.4f} {s['rr']:>7.4f}  {s['question']}")
    return metrics


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="RAG 检索质量评估")
    parser.add_argument("--dataset", type=str, default=None, help="评估数据集路径")
    parser.add_argument("--top-k", type=int, default=5, help="每轮检索返回数量")
    args = parser.parse_args()
    run_evaluation(dataset_path=args.dataset, top_k=args.top_k)
