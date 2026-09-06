# WMS AI Platform — PRD V2.0 落地审计报告

**日期**: 2026-09-05
**范围**: 全部 8 业务模块 + 4 基础设施服务 + 部署配置
**目标**: 评估业务需求（PRD V2.0）与技术架构的实现匹配度

---

## 总体评估

| 维度 | 状态 | 说明 |
|------|------|------|
| 基础设施层 | ✅ 完成 | B1+B2 批次 100% 落地 |
| RAG 模块 | ✅ 完成 | 3170 行 / 58 文件，全 Real |
| OCR 模块 | 🟡 部分 | MySQL 持久化 ✅，PO 差异 Mock |
| 其余 6 模块 | 🔴 Mock | 全部骨架状态，无真实数据 |
| 部署配置 | ✅ 完成 | docker-compose 20 服务 + K3s |

**综合评分**: 🟡 部分完成 — 基础设施和 RAG 已生产可用，6 个业务模块等待真实数据接入

---

## 批次完成度

| 批次 | 内容 | 完成率 | 关键项 |
|------|------|:------:|--------|
| B1 | 基础设施（模型+数据层） | 100% | DeepSeek/GLM、SiliconFlow、MySQL、MySQL Schema |
| B2 | RAG 知识库核心 | 100% | Parser/Chunker/Retriever/Rerank/Generator/Chat |
| B3 | OCR 升级 + WMS 对接 | 70% | MySQL持久化✅, Kafka✅, PO差异❌Mock |
| B4 | 预测+报表+P2模块 | 20% | 全部Mock（Forecast/Report/Voice/Drone/Video） |
| B5 | 基础设施增强 | 40% | Redis限流✅, 连接池✅, 网关无CORS/BodyLimit |

---

## 发现 Bug 清单（共 20 个）

### 🔴 运行时崩溃（P0 — 必须立即修复）

| # | 文件 | 问题 | 影响 |
|---|------|------|------|
| 1 | `modules/drone/main.py:158` | `Form(...)` 未导入 | 端点调用即 NameError |
| 2 | `modules/video/main.py:166` | `Form(...)` 未导入 | 端点调用即 NameError |
| 3 | `modules/aiops/main.py` | Pydantic 模型属性赋值丢失 | 任务状态无法更新 |
| 4 | `modules/forecast/main.py` | `np.random.seed(hash(sku))` | 重启后预测结果不可重现 |
| 5 | `modules/rag/parser/pdf_parser.py:70` | `asyncio.new_event_loop()` + `run_until_complete()` | FastAPI 异步上下文调用即 RuntimeError |
| 6 | `modules/rag/parser/image_parser.py` | 同 #5 | 同上 |
| 7 | `scheduler/main.py:handle_ocr_batch` | 调用 `data_service` 不存在的 `/v1/data/minio/{name}` 端点 | OCR 批处理 Kafka 消费永远 404 |

### 🟡 数据/逻辑错误（P1 — 尽快修复）

| # | 文件 | 问题 | 影响 |
|---|------|------|------|
| 8 | `data_service/main.py` | SQL 校验为 denylist，不防 `UNION`/`INTO OUTFILE` | SQL 注入风险 |
| 9 | `data_service/main.py` | Oracle/ClickHouse 端点丢弃 LIMIT 校验结果 | 慢查询无上限 |
| 10 | `common/data_client.py` | 每次 HTTP 调用新建 `AsyncClient` | 无连接池，性能差 |
| 11 | `gateway/main.py` | 无请求体大小限制 | 大文件上传可 OOM |
| 12 | `gateway/main.py` | 不转发 SSE/流式响应 | 后端 SSE 通过网关失效 |
| 13 | `modules/rag/knowledge/manager.py:delete_document()` | 删除文档不从 Milvus 删除向量 | 孤儿向量累积 |
| 14 | `modules/rag/knowledge/manager.py:save_chunks()` | 逐行 INSERT | 大文档性能瓶颈 |
| 15 | `modules/rag/chat/session.py:update_feedback()` | 任何 dislike 使全部 RAG L3 缓存失效 | 过度缓存清理 |
| 16 | `modules/rag/rerank/bge_rerank.py` | `rerank()` 和 `rerank_texts()` 读不同字段 | API 格式不一致 |
| 17 | `model_service/main.py` | LLM 用同步 `OpenAI` 客户端在 `async def` 内 | 阻塞事件循环 |
| 18 | `common/ai_client.py` | `llm_chat_messages` 无 tracing 日志 | 可观测性盲区 |
| 19 | `common/data_client.py` | MySQL 单连接无连接池 | 并发下序列化/损坏 |
| 20 | `scheduler/main.py` | 版本仍为 `"1.0.0"` | 未同步到 V2.0 |

---

## 逐模块详情

### ✅ RAG 模块（modules/rag/ — 3170 行, 58 文件）

| 子模块 | 文件 | 行数 | 状态 |
|--------|------|------|------|
| Parser | base/pdf/word/excel/markdown/image | 549 | ✅ Real |
| Chunker | base/structural/recursive/table | 294 | ✅ Real |
| Retriever | vector/keyword/fusion + word_dict.txt(101词) | 438 | ✅ Real |
| Rerank | bge_rerank.py | 108 | ✅ Real |
| Generator | llm_generator.py | 172 | ✅ Real |
| Verifier | llm_verifier.py | 86 | ✅ Real |
| Knowledge | manager.py | 175 | ✅ Real (有 #13/#14 bug) |
| Chat | session.py | 129 | ✅ Real (有 #15 bug) |
| Eval | evaluator.py + generation_evaluator.py | 400 | ✅ Real |
| Monitor | monitor.py | 175 | ✅ Real |
| Main | main.py | 646 | ✅ Real |

**RAG 优化 V2 完成状态**（2026-09-05）：
- R1 多路召回 ✅ | R2 元数据穿透 ✅ | R3 Prompt 升级 ✅
- R4 LLM Self-Check ✅ | R5 SSE 流式 ✅ | R6 生成质量评估 ✅
- 167/167 测试全绿

### 🟡 OCR 模块（modules/ocr/main.py — 353 行）

- ✅ MySQL `ocr_result` 持久化
- ✅ Kafka `ocr-confirmed` 事件
- ✅ 6 个端点完整
- ❌ `_diff_with_po` 用 Mock 硬编码 PO 明细

### 🔴 全部 Mock 的模块

| 模块 | 文件 | 行数 | 端点数 | Mock 方法 |
|------|------|------|--------|-----------|
| Forecast | forecast/main.py | 364 | 7 | `_get_history_data`, 训练, 预测 |
| Report | report/main.py | 204 | 4 | `_text_to_sql`, `_query_metrics` |
| Voice | voice/main.py | 180 | 3 | 全部端点 Mock |
| Drone | drone/main.py | 184 | 4 | 全部 Mock + Form Bug |
| Video | video/main.py | 192 | 4 | 全部 Mock + Form Bug |
| AIOps | aiops/main.py | 156 | 4 | 全部 Mock + Pydantic Bug |

---

## 基础设施层详情

### common/config.py（187 行）✅
- Pydantic BaseSettings，70+ 配置字段
- `_check_production_secrets` 安全门禁（11 个密钥检查）
- ⚠️ `model_route` 是 JSON 字符串，非 typed dict，解析错误静默降级

### common/ai_client.py（370 行）✅
- 五级缓存编排（L1→L2→L3），tenacity 重试
- ⚠️ `llm_chat` 和 `llm_chat_messages` 缓存逻辑重复 ~60 行
- ⚠️ `llm_chat_messages` 无 tracing 日志和热 prompt 记录

### common/data_client.py（331 行）✅
- Redis/MySQL/Kafka/MinIO 全客户端，懒加载 + mock 降级
- ⚠️ MySQL 单连接无池化，并发不安全
- ⚠️ `query_wms`/`query_olap`/`query_mysql` 每次新建 HTTP 客户端

### gateway/main.py（252 行）✅
- Redis+Lua 滑动窗口限流，JWT+API Key 双鉴权
- httpx 连接池复用（100 max）
- ⚠️ 无 CORS、无 body limit、不转发流式响应

### model_service/main.py（663 行）✅
- DeepSeek 主力 + GLM 降级，SiliconFlow embedding/rerank/OCR
- ⚠️ 同步 OpenAI 客户端阻塞事件循环
- ⚠️ YOLO/CLIP 懒加载无线程安全

### data_service/main.py（189 行）✅
- MySQL/Oracle/ClickHouse 三后端，SELECT-only
- ⚠️ SQL 校验为 denylist 而非 parser（#8）
- ⚠️ Oracle/ClickHouse 丢弃 LIMIT（#9）

### scheduler/main.py（215 行）🟡
- APScheduler cron + Kafka 消费
- ⚠️ 版本 1.0.0 未更新
- ⚠️ `handle_ocr_batch` 调用不存在的 data_service 端点（#7）
- ⚠️ Kafka 同步轮询阻塞事件循环

---

## 部署配置

| 文件 | 内容 | 状态 |
|------|------|------|
| docker-compose.yml | 20 服务（8 中间件 + 12 FastAPI），全部 healthcheck | ✅ |
| Dockerfile | python:3.11-slim，gcc/g++/libgl1/ffmpeg | ✅ |
| init_mysql.sql | 5 表（kb_document/kb_chunk/chat_session/chat_message/ocr_result） | ✅ |
| init_clickhouse.sql | 14 表（含 llm_call_log/rag_retrieval_log） | ✅ |
| deploy/k3s/deployment.yaml | 12 Deployment + Ingress + HPA | ✅ |

---

## 修复优先级建议

### P0 — 立即修复（4 个崩溃级 Bug）
1. drone/video `Form` 导入 → 2 行修复
2. aiops Pydantic → 改用 `model_copy(update=...)`
3. forecast `hash(sku)` → 改用 `hashlib.md5`

### P1 — 尽快修复（数据/安全）
4. data_service SQL 注入 → 用 `sqlparse` AST 校验
5. data_service Oracle/CH LIMIT → 使用校验后的 SQL
6. RAG parser event loop → 改用 `asyncio.to_thread()` 或 `nest_asyncio`
7. scheduler data_service 端点 → 新增 MinIO 代理端点

### P2 — 计划修复（Mock → Real）
8. Forecast 真实数据（ClickHouse 查询）
9. Report 真实数据（动态 Schema + 查询）
10. AIOps Prometheus 接入
11. OCR PO 差异对接

### P3 — 增强
12. Gateway CORS + body limit + WebSocket
13. model_service 异步 OpenAI 客户端
14. MySQL 连接池化
15. RAG 批量插入 + 向量清理
