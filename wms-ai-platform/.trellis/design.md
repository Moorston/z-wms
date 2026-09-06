# RAG 检索质量优化 — 技术设计

## 架构总览

```
                      ┌─────────────────────────────────────────────┐
                      │              RAG chat() 主流程               │
                      │                                             │
  用户问题 ──→ 问题改写 ──→ HybridRetriever ──→ BgeReranker ──→ LLMGenerator
                    │                │              │                  │
                    │          ┌─────┴─────┐        │                  │
                    │          ▼           ▼        │                  │
                    │   VectorRetriever  KeywordRetriever             │
                    │   (Milvus HNSW)   (jieba BM25)                 │
                    │                       │                         │
                    │                  ┌────┴────┐                    │
                    │                  ▼         ▼                    │
                    │          BM25 内存   MySQL FULLTEXT             │
                    │          (jieba)    (ngram parser)              │
                    │                                              │
                    ▼                                              ▼
              检索质量监控 ──→ ClickHouse rag_retrieval_log
              (async, 失败不影响主流程)
                      │
                      ▼
              评估体系（离线）
              tests/data/rag_eval_dataset.json
              → evaluator.py → Recall@5 / MRR
```

## R1：中文分词修复

### jieba 分词替换

**Before（keyword.py:70-73）：**
```python
def tokenize(text: str) -> List[str]:
    tokens = re.findall(r"[一-鿿]|[a-zA-Z0-9]+", text)
    return [t.lower() for t in tokens]
```

**After：**
```python
import jieba

_WORD_DICT_PATH = os.path.join(os.path.dirname(__file__), "word_dict.txt")
jieba.load_userdict(_WORD_DICT_PATH)

def tokenize(text: str) -> List[str]:
    tokens = jieba.cut(text, cut_all=False)
    # 过滤纯标点和空串
    return [t.lower() for t in tokens if t.strip()]
```

### WMS 领域词表

`word_dict.txt` 格式（jieba userdict 标准格式，每行一个词）：
```
波次 5 n
库位 5 n
拣货 5 n
预占 5 n
盘点 5 n
库区 5 n
上架 5 v
出库 5 v
入库 5 v
波次拣货 10 n
库存预警 10 n
安全库存 10 n
补货策略 10 n
库位编码 10 n
库位分配 10 n
拣货波次 10 n
库存冻结 10 n
库存解冻 10 n
库存扣减 10 n
库存预占 10 n
批次追溯 10 n
条码扫描 10 n
PDA 5 n
SKU 5 n
WMS 5 n
AGV 5 n
上架作业 10 n
发运作业 10 n
收货作业 10 n
```

### MySQL FULLTEXT ngram

`deploy/scripts/init_mysql.sql` 中 `kb_chunk` 表修改：
```sql
-- 旧：
-- FULLTEXT INDEX ft_content (content)
-- 新：
FULLTEXT INDEX ft_content (content) WITH PARSER ngram
```
需在 MySQL 配置 `my.cnf` 中设置 `ngram_token_size=2`。

## R2：检索参数配置化

### 配置项映射

| 配置项 | 旧位置 | 旧值 | 新值（默认） |
|--------|--------|------|-------------|
| `rag_retrieval_vector_ef` | vector.py:96 硬编码 | 64 | 128 |
| `rag_retrieval_vector_m` | vector.py:55 硬编码 | 16 | 16 |
| `rag_retrieval_vector_ef_construction` | vector.py:56 硬编码 | 200 | 200 |
| `rag_retrieval_rrf_k` | fusion.py:24 硬编码 | 60 | 60 |
| `rag_retrieval_top_k` | main.py:216 硬编码 | 20 | 20 |
| `rag_retrieval_rerank_top_n` | main.py:72 硬编码 | 5 | 5 |
| `rag_retrieval_keyword_mode` | keyword.py:33 逻辑判断 | 隐式 | "bm25" |

### ef 参数说明

HNSW `ef` 参数影响召回率与速度的平衡：
- `ef=64`：召回率约 90-95%，速度快
- `ef=128`：召回率约 95-99%，速度略慢（推荐）
- `ef=256`：召回率约 99%+，速度明显变慢

默认 128 是 WMS 知识库（通常 <10 万 chunk）的合理起点。

## R3：评估体系

### 评估指标定义

**Recall@K（召回率）：**
```
Recall@K = |期望文档 ∩ Top-K 结果| / |期望文档|
```
本任务用 `expected_keywords`（期望关键词集合）替代期望文档 ID，判断 Top-K 结果中是否包含这些关键词。

**MRR（平均倒数排名）：**
```
MRR = (1/N) * Σ (1/rank_i)
```
其中 `rank_i` 是第 i 条查询中第一个期望关键词首次出现的排名位置。

### 评估数据集格式

```json
[
  {
    "id": "eval_001",
    "query": "波次拣货怎么操作",
    "expected_keywords": ["波次", "拣货", "操作"],
    "category": "拣货流程",
    "priority": "high"
  },
  {
    "id": "eval_002",
    "query": "安全库存预警阈值怎么设置",
    "expected_keywords": ["安全库存", "预警", "阈值"],
    "category": "库存管理",
    "priority": "high"
  }
]
```

### 评估脚本架构

```
evaluator.py
├── RetrievalEvaluator 类
│   ├── __init__(dataset_path, retriever=None)
│   ├── evaluate_single(query, retrieved_docs) → {"recall_at_5", "mrr"}
│   ├── evaluate_dataset() → {"avg_recall_at_5", "avg_mrr", "details"}
│   └── run() → 命令行入口
└── CLI: python -m modules.rag.eval.evaluator --dataset <path>
```

当 `retriever=None` 时使用内存模拟数据评估计算逻辑正确性；当 `retriever` 存在时走真实检索。

## R4：检索质量监控

### 数据流

```
chat() ──→ monitor.record_retrieval_log(entry)
                │
                ▼
          asyncio.Queue (maxsize=1000)
                │
                ▼ (后台 worker 每 5s 批量 flush)
          ClickHouse rag_retrieval_log
                │
                ▼
          GET /rag/monitor/stats → 聚合查询
```

### 降级策略

1. ClickHouse 不可用 → Queue 内存缓冲，不阻塞主流程
2. Queue 满 → 丢弃最老记录 + 记 warning
3. 写入异常 → 记 error 日志，继续运行

### 监控条目数据结构

```python
@dataclass
class RetrievalLogEntry:
    trace_id: str
    query: str
    refined_query: str
    vector_results: int
    keyword_results: int
    fused_results: int
    reranked_results: int
    top_scores: str          # JSON string
    sources: str             # JSON string
    confidence: float
    latency_ms: int
    embedding_latency_ms: int
    rerank_latency_ms: int
    model: str
    status: str              # success/degraded/failed
```

## 兼容性

- jieba 是纯 Python 包，无 C 扩展，所有平台可安装
- MySQL ngram parser 是 MySQL 8.0 内置功能，无需插件
- ClickHouse 表使用 MergeTree + 按月分区，与现有 `llm_call_log` 一致
- 所有配置项有默认值，环境变量可覆盖，向后兼容

## 回滚方案

1. 回滚 jieba 分词：`requirements.txt` 移除 jieba，`keyword.py` 恢复旧分词
2. 回滚 MySQL ngram：`init_mysql.sql` 恢复 `FULLTEXT INDEX ft_content (content)`
3. 回滚配置化：配置项保留（默认值与旧值一致），不影响旧代码
4. 回滚监控：ClickHouse 表保留（不删数据），`monitor.py` 调用注释掉
