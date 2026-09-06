-- WMS AI Platform - ClickHouse 初始化脚本
-- 创建数据库和核心表

CREATE DATABASE IF NOT EXISTS wms_ai;

USE wms_ai;

-- ========== OCR识别记录表 ==========
CREATE TABLE IF NOT EXISTS ocr_record (
    doc_id String,
    doc_type String,
    raw_text String,
    extracted_data String,  -- JSON
    confidence Float32,
    diff_result String,     -- JSON
    status String,          -- pending_confirm/confirmed/rejected
    po_no String,
    order_no String,
    created_at DateTime,
    confirmed_at Nullable(DateTime)
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(created_at)
ORDER BY (doc_type, created_at)
TTL created_at + INTERVAL 90 DAY;

-- ========== 预测结果表 ==========
CREATE TABLE IF NOT EXISTS forecast_result (
    sku String,
    warehouse String,
    forecast_date Date,
    periods Int32,
    forecast Array(Float32),
    lower Array(Float32),
    upper Array(Float32),
    mape Float32,
    model String,
    created_at DateTime
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(forecast_date)
ORDER BY (warehouse, sku, forecast_date)
TTL forecast_date + INTERVAL 365 DAY;

-- ========== 预测模型元数据表 ==========
CREATE TABLE IF NOT EXISTS forecast_model (
    sku String,
    warehouse String,
    model String,
    mape Float32,
    data_points Int32,
    trained_at DateTime
) ENGINE = ReplacingMergeTree(trained_at)
ORDER BY (warehouse, sku, model);

-- ========== 补货建议表 ==========
CREATE TABLE IF NOT EXISTS replenishment_suggestion (
    id String,
    sku String,
    warehouse String,
    current_stock Float32,
    lead_demand Float32,
    safety_stock Float32,
    reorder_point Float32,
    suggest_qty Float32,
    need_replenish UInt8,
    service_level Float32,
    created_at DateTime
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(created_at)
ORDER BY (warehouse, sku, created_at);

-- ========== 报表记录表 ==========
CREATE TABLE IF NOT EXISTS report_record (
    report_id String,
    report_type String,
    report_date Date,
    warehouse String,
    metrics String,      -- JSON
    analysis String,
    push_channels Array(String),
    created_at DateTime
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(report_date)
ORDER BY (report_type, report_date)
TTL report_date + INTERVAL 365 DAY;

-- ========== RAG问答记录表 ==========
CREATE TABLE IF NOT EXISTS rag_chat_log (
    session_id String,
    question String,
    answer String,
    sources String,     -- JSON
    feedback Nullable(String),  -- useful/useless
    created_at DateTime
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(created_at)
ORDER BY (session_id, created_at)
TTL created_at + INTERVAL 180 DAY;

-- ========== AIOps告警记录表 ==========
CREATE TABLE IF NOT EXISTS aiops_alert (
    alert_id String,
    alert_name String,
    severity String,
    service String,
    metric String,
    value Float32,
    threshold Float32,
    root_cause String,   -- JSON
    fix_result String,   -- JSON
    created_at DateTime
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(created_at)
ORDER BY (service, severity, created_at)
TTL created_at + INTERVAL 90 DAY;

-- ========== 视频分析事件表 ==========
CREATE TABLE IF NOT EXISTS video_event (
    event_id String,
    station_id String,
    event_type String,
    severity String,
    description String,
    order_no Nullable(String),
    snapshot_url String,
    created_at DateTime
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(created_at)
ORDER BY (station_id, event_type, created_at)
TTL created_at + INTERVAL 90 DAY;

-- ========== 视频效率统计表 ==========
CREATE TABLE IF NOT EXISTS video_efficiency (
    station_id String,
    stat_date Date,
    pack_count Int32,
    avg_duration Float32,
    created_at DateTime DEFAULT now()
) ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(stat_date)
ORDER BY (station_id, stat_date);

-- ========== 无人机盘点任务表 ==========
CREATE TABLE IF NOT EXISTS drone_inventory_task (
    task_id String,
    warehouse String,
    area String,
    status String,
    flight_plan String,   -- JSON
    summary String,       -- JSON
    start_time Nullable(DateTime),
    end_time Nullable(DateTime),
    created_at DateTime
) ENGINE = ReplacingMergeTree(created_at)
PARTITION BY toYYYYMM(created_at)
ORDER BY (warehouse, task_id);

-- ========== 无人机盘点结果表 ==========
CREATE TABLE IF NOT EXISTS drone_inventory_result (
    task_id String,
    location String,
    recognized String,    -- JSON
    diff String,          -- JSON
    image_url String,
    created_at DateTime
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(created_at)
ORDER BY (task_id, location)
TTL created_at + INTERVAL 180 DAY;

-- ========== 语音拣货记录表 ==========
CREATE TABLE IF NOT EXISTS voice_pick_log (
    picker_id String,
    task_id String,
    location String,
    sku String,
    qty Int32,
    status String,
    exception Nullable(String),
    duration Float32,
    created_at DateTime
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(created_at)
ORDER BY (picker_id, created_at)
TTL created_at + INTERVAL 180 DAY;

-- ========== LLM 调用日志表（PRD 6.2.5）==========
-- 记录所有 LLM 调用的成本、延迟、缓存命中、模型路由等信息
CREATE TABLE IF NOT EXISTS llm_call_log (
    call_id String,
    task_type String,           -- rag/nl2sql/ocr_extract/question_rewrite 等
    model String,               -- 实际使用的模型（deepseek-v4-pro / glm-5.2）
    fallback_used UInt8,        -- 是否触发了备选模型降级（0=否, 1=是）
    prompt_tokens UInt32,
    completion_tokens UInt32,
    total_tokens UInt32,
    cost Decimal(10, 6),        -- 单次调用成本（美元）
    latency_ms UInt32,          -- 端到端延迟
    cache_hit String,           -- L1/L2/L3/L4/L5/none，命中哪级缓存
    cache_key String,           -- 缓存键（用于命中率分析）
    status String,              -- success/error/timeout/rate_limited
    error_msg Nullable(String),
    user_id Nullable(String),
    module String,              -- 调用方模块（rag/ocr/forecast/report/aiops）
    trace_id Nullable(String),  -- 链路追踪 ID
    created_at DateTime
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(created_at)
ORDER BY (module, task_type, created_at)
TTL created_at + INTERVAL 90 DAY;

-- ========== OCR 结果分析表（ClickHouse 侧，补充 MySQL ocr_result 的分析副本）==========
-- MySQL 的 ocr_result 表为业务操作主表（含 extracted/diff_result JSON），
-- ClickHouse 侧为统计分析副本：扁平化 MySQL JSON 字段为数值列，便于聚合查询。
-- 字段映射关系：
--   MySQL result_id       → ClickHouse doc_id
--   MySQL diff_result     → ClickHouse has_diff/diff_count（JSON 展开为数值）
--   MySQL confirmed_by    → ClickHouse confirmed_by
--   MySQL diff_result     → ClickHouse extracted_item_count（JSON item 计数）
--   MySQL raw_text        → ClickHouse raw_text_length（字符长度）
--   MySQL —                → ClickHouse processing_ms（识别耗时，仅分析用途）
CREATE TABLE IF NOT EXISTS ocr_result (
    doc_id String,
    doc_type String,
    confidence Float32,
    status String,             -- pending_confirm/confirmed/rejected
    has_diff UInt8,
    diff_count UInt32,
    po_no Nullable(String),
    order_no Nullable(String),
    confirmed_by Nullable(String),
    extracted_item_count UInt32,
    raw_text_length UInt32,
    processing_ms UInt32,
    created_at DateTime,
    confirmed_at Nullable(DateTime)
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(created_at)
ORDER BY (doc_type, status, created_at)
TTL created_at + INTERVAL 90 DAY;

-- ========== RAG 检索日志表（监控） ==========
CREATE TABLE IF NOT EXISTS rag_retrieval_log (
    ts DateTime,
    query String,
    refined_query String,
    total_docs UInt32,
    vector_docs UInt32,
    keyword_docs UInt32,
    latency_ms UInt32,
    top_score Float64,
    hit UInt8,
    chunk_ids Array(Int32),
    session_id String,
    user_id String,
    feedback String
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(ts)
ORDER BY (session_id, ts)
TTL ts + INTERVAL 180 DAY;
