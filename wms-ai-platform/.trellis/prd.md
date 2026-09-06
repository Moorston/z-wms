# PRD — B3/B4/B5 完成度审计与剩余缺口修复

## Goal

审计 PRD V2.0 批次 3-5（共 16 个任务）的实现完成度，确认已落地部分的质量，识别并修复剩余缺口。

## Background

PRD V2.0 执行计划（5 批次 22 任务）中，批次 1-2（基础设施+RAG）已于 2026-09-04/05 完成。
批次 3-5 涉及 OCR 升级、WMS 对接、预测/报表真实数据、基础设施增强。

### 审计结论（2026-09-05 全面扫描）

| 批次 | 任务 | 状态 | 证据 |
|------|------|------|------|
| B3-T1 | OCR MySQL 持久化 | ✅ DONE | `_save_to_mysql()` line 164, `list_results()` line 404 |
| B3-T2 | OCR PO 差异对接 | ✅ DONE | `_diff_with_po()` line 257, `data_service /v1/data/mysql` |
| B3-T3 | Kafka 消费者+确认回写 | ✅ DONE | `handle_ocr_batch_message()` line 515, scheduler 注册 |
| B3-T4 | ClickHouse Schema 对齐 | ✅ DONE | `llm_call_log` table in init_clickhouse.sql line 188 |
| B4-T1 | 预测真实数据+MinIO | ✅ DONE | `_get_history_data()` line 265, MinIO 上传/下载 |
| B4-T2 | 报表真实数据+动态Schema | ✅ DONE | `_load_schema()` line 63, `_query_metrics()` line 176 |
| B4-T3 | 语音拣货增强 | ✅ DONE | `get_task_from_wms()` line 30, `get_task_progress()` line 58 |
| B4-T4 | 无人机盘点修复 | ✅ DONE | `Form` 导入修复, `_compare_inventory()` line 160 |
| B4-T5 | 打包视频修复 | ✅ DONE | `Form` 导入修复, `get_events()` line 165 |
| B5-T1 | 网关分布式限流 | ✅ DONE | Redis+Lua 滑动窗口 line 73-87 |
| B5-T2 | 链路追踪+指标 | ⚠️ **PARTIAL** | 见下 |
| B5-T3 | docker-compose 重写 | ✅ DONE | 12 服务 + 6 中间件 |
| B5-T4 | AIOps 模块实现 | ✅ DONE | Pydantic 修复, 脚本库, Webhook, 告警聚合 |

## B5-T2 剩余缺口分析

### 已完成部分
- `common/tracing.py` — 完整的 OpenTelemetry 实现：
  - `init_tracing()` — OTLP exporter 初始化
  - `instrument_app()` — FastAPI 自动追踪注入
  - `get_trace_id()` — 3 级优先级 TraceId 获取
  - `LLMCallLogger` — 写入 ClickHouse `llm_call_log` 表
- `common/ai_client.py` — `llm_call_logger.log_call()` 已接入 3 个调用点：
  - 缓存命中路径（line 64）
  - LLM 调用失败路径（line 103）
  - LLM 调用成功路径（line 118）
- `deploy/scripts/init_clickhouse.sql` — `llm_call_log` 表已定义
- `tests/test_tracing_llm_log.py` — 11 个单元测试覆盖 tracing 核心逻辑
- `gateway/main.py` — 网关中间件已生成 TraceId 并透传 `X-Trace-Id` header

### 未完成部分
**`init_tracing()` 和 `instrument_app()` 未被任何服务调用。**

当前各服务 `main.py` 仅调用 `setup_metrics(app)`，未调用：
- `init_tracing("service-name")` — 初始化 OTLP exporter
- `instrument_app(app, "service-name")` — 注入 FastAPI 自动追踪中间件

受影响文件：
- `gateway/main.py`
- `model_service/main.py`
- `data_service/main.py`
- `scheduler/main.py`
- `modules/ocr/main.py`
- `modules/forecast/main.py`
- `modules/report/main.py`
- `modules/rag/main.py`
- `modules/aiops/main.py`
- `modules/voice/main.py`
- `modules/drone/main.py`
- `modules/video/main.py`

### 影响评估
- **`init_tracing()`**: 未初始化 OTLP exporter 时，OpenTelemetry span 不会导出到 collector。但 `get_trace_id()` 仍能工作（网关已手动生成 TraceId）。
- **`instrument_app()`**: 未注入时，FastAPI 自动生成的 span 不可用。但网关中间件已手动透传 TraceId，各模块可通过 `get_trace_id(request)` 获取。
- **实际影响**: 在生产环境中（OTEL_ENABLED=true），span 不会被导出到 Jaeger/Tempo 等后端，链路追踪数据不可见。开发环境无影响（降级为 no-op）。

## Requirements

### R1: 链路追踪初始化接线（P1 — 生产环境功能缺口）

在所有 12 个服务的 `main.py` 中，`setup_metrics(app)` 之后增加 `init_tracing()` + `instrument_app()` 调用。
对于当前缺失 `setup_metrics` 的 8 个服务，同时补上 `setup_metrics` 挂载。

**验收标准**:
1. 所有 12 个服务均调用 `init_tracing()` + `instrument_app()`
2. 未启用时（OTEL_ENABLED=false）降级为 no-op，不影响启动
3. 全量测试保持通过（pytest tests/ -v → 全绿）
4. `python -c "from gateway.main import app; print('ok')"` 等所有服务导入无错误

### R2: Metrics 挂载补全

对当前缺失 `setup_metrics(app)` 的 8 个服务补上：
- `model_service/main.py`、`data_service/main.py`、`scheduler/main.py`
- `modules/ocr/main.py`、`modules/forecast/main.py`、`modules/voice/main.py`
- `modules/drone/main.py`、`modules/video/main.py`

**验收标准**:
1. 所有 12 个服务均调用 `setup_metrics(app)`
2. `/metrics` 端点在各服务可访问

## Acceptance Criteria

1. **AC1**: 12 个服务均调用 `init_tracing()` + `instrument_app()`
2. **AC2**: 12 个服务均调用 `setup_metrics(app)`
3. **AC3**: 全量 pytest 测试通过（118/118 绿）
4. **AC4**: 所有服务 `python -c "from X.main import app"` 导入无错误
5. **AC5**: 开发环境（OTEL_ENABLED 未设置）启动无报错，降级为 no-op

## Out of Scope

- OpenTelemetry collector 部署（需要额外基础设施，docker-compose 中已有 prometheus 但无 Jaeger/Tempo）
- 各模块 `get_trace_id(request)` 显式调用（当前通过网关 header 透传，各模块可在需要时调用）
- Prometheus/Grafana 仪表盘配置（已有 Prometheus scrape config，仪表盘非本次范围）
- ClickHouse `llm_call_log` 表数据验证（需要完整环境集成测试）

## Open Questions

**Q1**: 是否需要将 `model_service/main.py` 中 LLM 实际调用的 token/cost 信息也写入 `llm_call_log`？
- 当前 `ai_client.py` 调用 model_service `/v1/llm/chat`，但 token/cost 信息由 `ai_client.py` 估算（4 字符 ≈ 1 token），不是 model_service 返回的真实值。
- 如果 model_service 的响应中包含 `usage` 字段（如 OpenAI API 格式），则可以在 `ai_client.py` 中读取并传递给 `llm_call_logger.log_call()`。

**我的推荐答案**: 暂不做。当前估算方案已可接受，且 model_service 当前返回格式未包含 usage。等实际使用 DeepSeek API 时（API 返回 usage），再增强 `ai_client.py` 读取 `resp.json()["usage"]` 传递给 logger。
