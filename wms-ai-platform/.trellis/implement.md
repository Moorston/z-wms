# RAG 检索质量优化 — 实施计划

## 实施顺序

### 步骤 1：依赖 + 词表 + 配置基础（无依赖，最先做）

- [ ] `requirements.txt` 新增 `jieba`
- [ ] `modules/rag/retriever/word_dict.txt` 创建 WMS 领域词表
- [ ] `common/config.py` 新增 7 个 RAG 检索配置项
- [ ] `.env.example` 新增对应环境变量示例

### 步骤 2：中文分词替换（R1）

- [ ] `modules/rag/retriever/keyword.py` — jieba.cut() 替换正则分词
- [ ] `deploy/scripts/init_mysql.sql` — kb_chunk FULLTEXT ngram parser

### 步骤 3：检索参数配置化（R2）

- [ ] `modules/rag/retriever/vector.py` — ef/M/efConstruction 从配置读取
- [ ] `modules/rag/retriever/fusion.py` — rrf_k 从配置读取
- [ ] `modules/rag/main.py` — top_k 和 rerank_top_n 从配置读取
- [ ] `modules/rag/retriever/keyword.py` — keyword_mode 从配置读取

### 步骤 4：检索质量评估体系（R3）

- [ ] `modules/rag/eval/__init__.py` — 包初始化
- [ ] `tests/data/rag_eval_dataset.json` — 20-30 条评估数据集
- [ ] `modules/rag/eval/evaluator.py` — 评估脚本 + CLI
- [ ] RAG main.py 新增 `GET /rag/evaluate` 端点

### 步骤 5：检索质量监控（R4）

- [ ] `modules/rag/monitor.py` — 监控记录器（asyncio.Queue + 批量写入）
- [ ] `deploy/scripts/init_clickhouse.sql` — 新增 rag_retrieval_log 表
- [ ] `modules/rag/main.py` — chat() 集成监控 + `GET /rag/monitor/stats` 端点

### 步骤 6：测试

- [ ] `tests/test_rag_tokenizer.py`
- [ ] `tests/test_rag_config.py`
- [ ] `tests/test_rag_eval.py`
- [ ] `tests/test_rag_monitor.py`

## 验证命令

```bash
# 1. 语法验证
cd wms-ai-platform
python -c "import jieba; print(list(jieba.cut('库存预占')))"
python -c "from common.config import settings; print(settings.rag_retrieval_vector_ef)"
python -c "from modules.rag.eval.evaluator import RetrievalEvaluator; print('eval ok')"
python -c "from modules.rag.monitor import RetrievalMonitor; print('monitor ok')"

# 2. 旧分词已移除
grep -rn "re.findall.*一-鿿" modules/rag/  # 应为空
grep -rn "ef=64" modules/rag/  # 应为空
grep -rn "rrf_k=60" modules/rag/  # 应为空

# 3. 新测试
python -m pytest tests/test_rag_tokenizer.py tests/test_rag_config.py tests/test_rag_eval.py tests/test_rag_monitor.py -v

# 4. 全量回归
python -m pytest tests/ -v
# 预期：118 个原有测试保持全绿
```

## 风险与回滚

| 风险 | 回滚方式 |
|------|----------|
| jieba 加载词表慢（首次加载约 100ms） | 不影响性能，仅首次调用加载 |
| MySQL ngram 索引需要重建 | `DROP INDEX ft_content` 后重建，或 `ALTER TABLE ... ADD FULLTEXT ... WITH PARSER ngram` |
| ClickHouse 不可用导致监控不可用 | 不影响主流程（设计已包含降级） |
| 配置项默认值与旧值不一致 | `rag_retrieval_vector_ef` 默认 128（旧值 64），其余默认值与旧值一致 |
