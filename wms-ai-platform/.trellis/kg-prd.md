# PRD — 知识图谱（Knowledge Graph）集成

## Goal

将知识图谱作为第三路检索通道集成到 WMS AI 平台的 RAG 模块，通过实体-关系三元组索引和图路径检索增强混合检索能力，提升跨文档关联召回和实体关系推理。

## Background

当前 RAG 流水线：解析 → 分块 → 混合检索（向量+关键词 RRF）→ Rerank → LLM 生成。
知识库存储：Milvus（向量）+ MySQL（元数据+全文），无图数据库，无实体关系抽取。

WMS 领域天然存在丰富的实体关系（仓库-库位-商品-批次-订单-波次-拣货员），
但当前 RAG 无法感知这些关系，导致：
- 跨文档关联检索能力弱（"波次拣货涉及哪些库存操作"需跨多个文档理解）
- 实体关系推理能力缺失（无法回答"SKU123 从入库到出库经过哪些节点"）
- 知识库查询结果缺乏结构化关系上下文

## Design Decisions

| # | 决策 | 选项 | 理由 |
|---|------|------|------|
| Q1 | 用途定位 | A — 纯检索增强（Graph-based Retrieval Augmentation） | KG 作为第三路检索通道，不引入图数据库，实体关系三元组轻量存储 |
| Q2 | 抽取策略 | C — 混合模式（正则+词典 + LLM 补充） | 正则覆盖标准格式实体（SKU/WH/WAVE），LLM 补充非结构化语义关系 |
| Q3 | 三元组存储 | C — MySQL 持久化 + 内存邻接表 | 查询走内存（<1ms），存储走 MySQL（持久），启动时重建邻接表 |
| Q4 | 融合方式 | B — KG 独立检索 + 前置实体识别 | KG 不走 RRF 融合，图路径序列化后直接追加到 docs 列表，降级安全 |
| Q5a | 模块位置 | A — `modules/rag/kg/` | 与 Q1 定位一致，KG 不是独立模块，子目录含 extraction/store/retriever |
| Q5b | 抽取时机 | A — 文档上传时同步抽取 | 与当前上传架构一致，文档数量不大时可接受 |
| Q5c | 延迟预算 | A — ≤10ms | 内存邻接表 1-2 跳展开，纯内存操作 |
| Q6 | Prompt Schema | C — 增强 Schema（含 type + source_span + confidence） | 支持按类型过滤、引用溯源、低质量过滤 |
| Q7 | 查询时实体识别 | A — 纯正则+词典 | WMS 实体格式标准化，查询时 LLM 调用不符合 ≤10ms 预算 |
| Q8a | 测试范围 | C — 单元测试 + 集成测试 | 与现有测试策略一致 |
| Q8b | 验收标准 | B — 功能 + 质量验收 | 合理质量门槛，性能由 Q5c 天然满足 |

## Confirmed Facts

1. 代码库中无任何知识图谱相关代码（无 entity/relation/extraction 模块）
2. docker-compose.yml 中无图数据库服务（Neo4j/ArangoDB 均未部署）
3. requirements.txt 中无任何图数据库或 NLP 实体抽取依赖
4. 当前 RAG 混合检索架构：VectorRetriever(Milvus HNSW+COSINE) + KeywordRetriever(BM25 MySQL FULLTEXT) → HybridRetriever(RRF k=60)
5. MySQL `kb_document` + `kb_chunk` 表已持久化文档元数据，`kb_chunk.metadata` JSON 列可复用
6. `ai_client.py` 提供 `llm_extract_json()` 和 `embed()` 接口，可供实体抽取使用
7. 5 级缓存架构已就绪，可复用
8. `fusion.py` `_rrf_fuse()` 双路融合，KG 作为独立通道不改动 RRF 逻辑

## Requirements

### R1: 实体关系抽取模块（`modules/rag/kg/extraction.py`）

- 实现 `extract_triples(content, doc_id) -> List[Triple]`
- 混合模式：正则+词典覆盖标准格式实体（SKU/WH/WAVE/B 批次号）
- LLM 补充抽取非结构化语义关系（使用 `ai_client.llm_extract_json()`）
- 输出 Schema：`{subject, subject_type, predicate, object, object_type, confidence, source_span, doc_id}`
- `subject_type` / `object_type` 枚举：仓库/库位/商品/SKU/波次/订单/批次/拣货员
- 置信度 < 0.5 的三元组过滤丢弃

### R2: 图存储模块（`modules/rag/kg/store.py`）

- MySQL 表 `kg_triple`：`(id, subject, subject_type, predicate, object, object_type, confidence, doc_id, created_at)`
- 内存邻接表：`Dict[str, List[Tuple[str, str, float]]]`（节点 → [(关系, 目标, 置信度)]）
- 启动时从 MySQL 重建邻接表（`SELECT subject, predicate, object, confidence FROM kg_triple`）
- 文档上传时同步写入 MySQL + 更新内存表
- 文档删除时清理关联三元组 + 更新内存表

### R3: 图检索模块（`modules/rag/kg/retriever.py`）

- 实现 `GraphRetriever.retrieve(query, max_hops=2, top_k=10) -> List[GraphResult]`
- 查询时纯正则+词典实体识别（`extract_entities(query)`）
- 命中 KG 节点后 1-2 跳展开
- 结果序列化：`"SKU123 → 属于 → WH01 → 包含 → A01 → 上架"`
- 输出：`[{path, entities, hop_count, confidence}]`

### R4: KG 检索集成到 RAG 流水线（`modules/rag/main.py`）

- `chat()` 和 `chat_stream()` 中，混合检索后追加 KG 检索结果
- KG 结果序列化为文本附加到 docs 列表，标记 `source: "kg"`
- 降级安全：KG 不可用时（邻接表为空/异常），vector+keyword 不受影响
- 缓存版本 bump 包含 KG 数据变更

### R5: 配置（`common/config.py`）

- `kg_enabled: bool = True` — KG 总开关
- `kg_extract_llm: bool = True` — LLM 补充抽取开关
- `kg_max_hops: int = 2` — 最大跳数
- `kg_min_confidence: float = 0.5` — 最低置信度阈值
- `kg_top_k: int = 10` — 最大返回路径数

## Acceptance Criteria

- AC1: 5 种实体类型可抽取（SKU/仓库/库位/波次/批次）
- AC2: KG 检索延迟 ≤10ms（内存邻接表 1-2 跳）
- AC3: 图路径序列化正确（"实体 → 关系 → 实体" 链式格式）
- AC4: KG 降级安全（不可用时不影响 vector+keyword 检索）
- AC5: 三元组抽取准确率 ≥80%（10 个标注文档样本验证）
- AC6: 查询时实体识别覆盖率 ≥85%（标准格式实体）
- AC7: 文档上传/删除后 KG 数据同步更新
- AC8: 单元测试 + 集成测试覆盖全部 4 个 KG 模块

## Out of Scope

- 图数据库基础设施（Neo4j/ArangoDB）— 当前用 MySQL+内存邻接表
- 查询时 LLM 实体识别 — 纯正则+词典，查询延迟 ≤10ms
- 复杂图推理（多跳路径搜索、最短路径、社区发现）
- 图可视化（Cytoscape.js / D3.js）
- 知识图谱推理引擎（OWL/DL 推理）
- 跨文档实体消歧（同一实体在不同文档中的合并）

## Open Questions

（无 — 全部决策已收敛）
