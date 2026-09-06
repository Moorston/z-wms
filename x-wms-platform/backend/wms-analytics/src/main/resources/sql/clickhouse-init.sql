-- ============================================================
-- X WMS ClickHouse OLAP 建表脚本
-- 数据库：wms_olap
-- 表引擎：ReplacingMergeTree（自动去重，支持增量同步）
-- ============================================================

CREATE DATABASE IF NOT EXISTS wms_olap;

USE wms_olap;

-- 1. 入库单OLAP表
CREATE TABLE IF NOT EXISTS wms_inbound_order_olap (
    id UInt64,
    order_no String,
    inbound_type String,
    warehouse String,
    owner_code String,
    status String,
    expected_qty Decimal(18,4),
    received_qty Decimal(18,4),
    putaway_qty Decimal(18,4),
    created_at DateTime,
    updated_at DateTime
) ENGINE = ReplacingMergeTree(updated_at)
PARTITION BY toYYYYMM(created_at)
ORDER BY (warehouse, order_no)
TTL created_at + INTERVAL 2 YEAR;

-- 2. 出库单OLAP表
CREATE TABLE IF NOT EXISTS wms_outbound_order_olap (
    id UInt64,
    order_no String,
    outbound_type String,
    warehouse String,
    owner_code String,
    customer_code String,
    status String,
    wave_no String,
    sku String,
    expected_qty Decimal(18,4),
    allocated_qty Decimal(18,4),
    picked_qty Decimal(18,4),
    shipped_qty Decimal(18,4),
    express_code String,
    tracking_no String,
    created_at DateTime,
    updated_at DateTime
) ENGINE = ReplacingMergeTree(updated_at)
PARTITION BY toYYYYMM(created_at)
ORDER BY (warehouse, order_no)
TTL created_at + INTERVAL 2 YEAR;

-- 3. 作业任务OLAP表
CREATE TABLE IF NOT EXISTS wms_work_task_olap (
    id UInt64,
    task_no String,
    task_type String,
    warehouse String,
    status String,
    priority Int32,
    order_no String,
    sku String,
    location_code String,
    expected_qty Decimal(18,4),
    actual_qty Decimal(18,4),
    operator String,
    actual_start_time DateTime,
    completed_time DateTime,
    created_at DateTime,
    updated_at DateTime
) ENGINE = ReplacingMergeTree(updated_at)
PARTITION BY toYYYYMM(created_at)
ORDER BY (warehouse, task_no)
TTL created_at + INTERVAL 1 YEAR;

-- 4. 库存流水OLAP表
CREATE TABLE IF NOT EXISTS wms_inventory_transaction_olap (
    id UInt64,
    sku String,
    warehouse String,
    location_code String,
    batch_no String,
    transaction_type String,
    qty_before Decimal(18,4),
    qty_change Decimal(18,4),
    qty_after Decimal(18,4),
    order_no String,
    operator String,
    created_at DateTime
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(created_at)
ORDER BY (warehouse, sku, created_at)
TTL created_at + INTERVAL 2 YEAR;

-- 5. 批次追踪事件OLAP表
CREATE TABLE IF NOT EXISTS wms_batch_trace_olap (
    id UInt64,
    batch_no String,
    sku String,
    warehouse String,
    event_type String,
    qty_before Decimal(18,4),
    qty_change Decimal(18,4),
    qty_after Decimal(18,4),
    order_no String,
    location_code String,
    operator String,
    event_time DateTime
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(event_time)
ORDER BY (batch_no, event_time)
TTL event_time + INTERVAL 3 YEAR;

-- 6. 同步水位线表
CREATE TABLE IF NOT EXISTS wms_sync_watermark (
    table_name String,
    last_sync_time DateTime,
    updated_at DateTime DEFAULT now()
) ENGINE = ReplacingMergeTree(updated_at)
ORDER BY table_name;

-- ============================================================
-- 物化视图（预聚合，提升查询性能）
-- ============================================================

-- 出库按天汇总
CREATE MATERIALIZED VIEW IF NOT EXISTS wms_outbound_daily_mv
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(date)
ORDER BY (warehouse, date)
AS SELECT
    warehouse,
    toDate(created_at) as date,
    count() as order_count,
    sum(shipped_qty) as shipped_qty
FROM wms_outbound_order_olap
GROUP BY warehouse, date;

-- 作业按天+类型汇总
CREATE MATERIALIZED VIEW IF NOT EXISTS wms_worktask_daily_mv
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(date)
ORDER BY (warehouse, date, task_type)
AS SELECT
    warehouse,
    toDate(completed_time) as date,
    task_type,
    count() as task_count,
    sum(actual_qty) as total_qty
FROM wms_work_task_olap
WHERE status = 'COMPLETED'
GROUP BY warehouse, date, task_type;
