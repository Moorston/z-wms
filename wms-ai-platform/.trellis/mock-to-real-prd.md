# PRD — AI 平台 Mock 转真实 + RAG/KG 增强 + WMS 跨平台集成

> 状态：规划中（brainstorm 阶段）
> 范围：wms-ai-platform 全模块 + x-wms-platform 集成接点
> 创建日期：2026-09-06

## Goal

（待 brainstorm 收敛）

初步方向：将 wms-ai-platform 中仍处于 Mock/骨架状态的模块转为真实实现，
深化 RAG/知识图谱能力，并打通 WMS 主平台（Java）与 AI 平台（Python）的双向集成。

## Background

### 项目现状（基于 PRD V2.0 + 09-05 审计报告）

wms-ai-platform 是 X WMS 的独立 AI 能力平台（Python 3.11 + FastAPI，12 服务），
通过 API/Kafka 与 Java 主平台解耦，提供 8 大 AI 能力：
OCR / 预测 / 报表 / RAG问答 / AIOps / 语音拣货 / 无人机盘点 / 打包视频。

#### 已落地（生产可用）
- **基础设施层**（B1+B2）：DeepSeek/GLM 商用 API、SiliconFlow Embedding/Rerank、MySQL、五级缓存
- **RAG 知识库核心**：Parser/Chunker/Retriever/Rerank/Generator/Chat（35 文件，真实实现）
- **RAG 检索质量优化**：jieba 分词、检索参数配置化、评估体系（eval/）、监控（monitor.py）
- **知识图谱集成**（kg/）：extraction/store/retriever，第三路检索通道
- **OCR 部分**：MySQL 持久化、Kafka 批量、5 种单据类型、LLM 抽取
- **基础设施增强**：Redis 分布式限流、连接池、docker-compose 20 服务

#### 待推进（本次规划目标）
1. **6 个模块 Mock 转真实**（审计报告最大缺口）：
   - OCR：PO 差异校验对接 WMS 真实数据（当前 Mock）、OCR 引擎升级 PP-StructureV3
   - forecast：历史数据对接 WMS（当前 Mock 模拟）、模型存 MinIO、结果回写 ClickHouse
   - report：动态 Schema + 真实指标计算、定时报表推送
   - voice：WebSocket + ASR(FunASR) + TTS + WMS 拣货任务对接
   - drone：航线规划 + CLIP 识别 + YOLO 数量估算 + 库存比对
   - video：RTSP 流 + YOLO 检测 + 违规检测 + 效率统计
2. **RAG/KG 进一步增强**（kg-prd Out of Scope 项）：
   - 实体消歧、跨文档合并、图推理、图可视化等高级能力
3. **WMS↔AI 跨平台集成**：
   - scheduler 调用 data_service 不存在端点（OCR 批处理 404）
   - Kafka topic 双向对接（ocr-confirmed / forecast-completed / wms-outbound-event 等）
   - WMS 主平台 wms-integration 服务是否对接 AI 平台

### PRD V2.0 迭代规划（6 Sprint × 2 周）
- Sprint 1-2：基础设施 + RAG（已完成）
- Sprint 3：OCR 升级 + WMS 对接（部分完成）
- Sprint 4：预测 + 报表落地（Mock 状态）
- Sprint 5：基础设施增强（部分完成，链路追踪接线未完成见 prd.md）
- Sprint 6：高级模块 + 运营（AIOps/语音/无人机/视频）

### 关键约束（PRD V2.0 §1.2 目标）
- 去掉本地 GPU 依赖，6C12G 可启动
- 知识库检索问答准确率 ≥80%
- 8 大模块全部可演示（至少端到端核心流程）
- 与 WMS 核心系统打通
- AI API 费用 ≤¥500/月，服务器 ≤¥2000/月

## Confirmed Facts

### 文档层
1. PRD V2.0 是基于真实架构的落地版，V1.0 为早期待评审版（以 V2.0 为准）
2. 8 模块优先级：OCR/forecast/report/RAG 为 P0-P1，AIOps/voice/drone/video 为 P2
3. 与 WMS 集成方式：REST API + Kafka 双向，AI 故障不影响 WMS 核心（可降级人工）
4. 6 个 Kafka topic 已规划（ocr-confirmed / ocr-batch / forecast-completed / report-push / wms-outbound-event / ai-alert）
5. 现有 .trellis/ 三份 PRD 均已落地（B3-B5审计、RAG检索优化、知识图谱）

### 代码层（2026-09-06 直接探查，审计报告之后代码已大幅推进）

**重大发现：09-05 审计报告标注的"Mock/骨架"状态已部分过时，多个模块已接真实数据。**

#### 6 个模块实际状态（对照 PRD V2.0 "待开发"清单）

| 模块 | PRD V2.0 标注 | 代码实际状态 | 证据 |
|------|--------------|-------------|------|
| **ocr** | 完成85%，PO差异Mock | PO差异已真实查询 WMS `po_detail` 表 | `main.py:262 _diff_with_po` 调 `data_service /v1/data/mysql` 查 po_detail |
| **forecast** | 完成80%，历史数据Mock | ClickHouse 真实查询+失败降级Mock，MinIO模型存储已实现 | `main.py:116-117 _get_history_data`，`:128-129 _upload_model_to_minio`，`:145 _train_prophet` 真Prophet |
| **report** | 完成50%，指标Mock | ClickHouse 真实查询+`is_mock` 分支降级 | `main.py:182 真实查询`，`:199/218/240 is_mock 分支` |
| **voice** | 完成15%骨架 | **已接 WMS** `pick_task` 表真实查询+进度查询+Redis缓存 | `main.py:35-61 get_task_from_wms`，`:63-96 get_task_progress`，tracing已初始化 |
| **drone** | 完成15%骨架 | **已接 WMS** `wms_location` 库位坐标+牛耕式航线算法+Form导入已修复 | `main.py:51-84 _plan_flight_path`，`:86-100 _get_location_coords`，`:9 Form导入` |
| **video** | 完成15%骨架 | FFmpeg/YOLO 降级模拟事件，未装时返回空（不再Mock） | `main.py:73 降级`，`:154 Mock事件`，`:202 返回空` |
| **aiops** | 完成15%骨架 | `_collect_metrics` 有 `raise NotImplementedError`，Prometheus查询降级Mock | `main.py:65 NotImplementedError`，`:198/212/240 降级` |

#### RAG/KG 实际状态
- `modules/rag/kg/` 已有 extraction.py / store.py / retriever.py + __init__.py（4 文件，kg-prd 已落地）
- `modules/rag/eval/` 已有 evaluator.py / generation_evaluator.py（评估体系已落地）
- `modules/rag/monitor.py` 已存在（监控已落地）
- `modules/rag/verifier/` 目录存在（kg-prd 未提及，待确认用途）
- jieba 已在 requirements.txt（分词优化已落地）

#### 跨平台集成实际状态
- **确认 bug**：scheduler 调用 `data_service /v1/data/minio/{name}` 端点不存在（agent 确认），OCR 批处理 Kafka 消费 404
- voice/drone 已通过 `data_client.query_mysql` 直查 WMS MySQL 表（pick_task / wms_location / po_detail）
- 待确认：WMS 主平台(Java) wms-integration 服务是否主动对接 AI 平台（agent 未及完成此探查）

## Requirements

（待 brainstorm 收敛 — 按三大方向逐项提问后填写）

## Acceptance Criteria

（待 brainstorm 收敛）

## Out of Scope

（待 brainstorm 收敛）

## Open Questions

（brainstorm 进行中，待逐项提问）
