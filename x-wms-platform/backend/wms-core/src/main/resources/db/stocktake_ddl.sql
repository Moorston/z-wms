-- ============================================================
-- X WMS 盘点管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 盘点任务/盘点明细/盘点差异/盘点调整
-- ============================================================

-- 1. 盘点任务表
CREATE TABLE wms_stocktake_task (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    task_no         VARCHAR(64)  NOT NULL,
    task_name       VARCHAR(128),
    stocktake_type  VARCHAR(16)  NOT NULL, -- FULL全盘/AREA库区/SKU指定/CYCLE循环/RANDOM抽盘
    warehouse_code  VARCHAR(64)  NOT NULL,
    area_code       VARCHAR(64),            -- 库区(库区盘点时)
    location_from   VARCHAR(64),            -- 库位范围起
    location_to     VARCHAR(64),            -- 库位范围止
    sku_list        TEXT,                    -- 指定SKU列表(JSON)
    owner_code      VARCHAR(64),
    batch_no        VARCHAR(64),            -- 指定批次
    abc_class       VARCHAR(8),             -- ABC分类(循环盘点)
    cycle_count_id  BIGINT,              -- 循环盘点计划ID
    plan_start_time TIMESTAMP,
    plan_end_time   TIMESTAMP,
    actual_start_time TIMESTAMP,
    actual_end_time TIMESTAMP,
    status          VARCHAR(32)  NOT NULL,  -- DRAFT草稿/PENDING待执行/COUNTING盘点中/RECOUNTING复盘中/ADJUSTING调整中/COMPLETED完成/CANCELLED取消
    freeze_flag     TINYINT(1)     DEFAULT 0, -- 是否冻结库存(盘点期间禁止出入库)
    blind_count     TINYINT(1)     DEFAULT 0, -- 是否盲盘(不显示系统库存)
    recount_threshold DECIMAL(14,4),          -- 差异超过此值触发复盘
    priority        SMALLINT     DEFAULT 5,
    assignee        VARCHAR(64),
    checker         VARCHAR(64),            -- 复核人
    total_sku_count INT    DEFAULT 0,
    total_location_count INT DEFAULT 0,
    counted_sku_count INT  DEFAULT 0,
    diff_sku_count  INT    DEFAULT 0,
    diff_qty        DECIMAL(14,4)  DEFAULT 0,
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    deleted         TINYINT(1)     DEFAULT 0,
    CONSTRAINT pk_wms_stocktake_task PRIMARY KEY (id),
    CONSTRAINT uk_wms_stocktake_task_no UNIQUE (task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_st_task_status ON wms_stocktake_task(status);
CREATE INDEX idx_wms_st_task_warehouse ON wms_stocktake_task(warehouse_code);
CREATE INDEX idx_wms_st_task_type ON wms_stocktake_task(stocktake_type);

-- 2. 盘点明细表
CREATE TABLE wms_stocktake_item (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    task_id         BIGINT    NOT NULL,
    location_code   VARCHAR(64)  NOT NULL,
    sku             VARCHAR(64)  NOT NULL,
    barcode         VARCHAR(64),
    product_name    VARCHAR(256),
    batch_no        VARCHAR(64),
    owner_code      VARCHAR(64),
    system_qty      DECIMAL(14,4) DEFAULT 0,  -- 系统库存
    first_count_qty DECIMAL(14,4),            -- 初盘数量
    second_count_qty DECIMAL(14,4),           -- 复盘数量
    final_count_qty DECIMAL(14,4),            -- 最终确认数量
    diff_qty        DECIMAL(14,4) DEFAULT 0,  -- 差异数量(最终-系统)
    diff_amount     DECIMAL(14,4) DEFAULT 0,  -- 差异金额
    count_status    VARCHAR(16),            -- PENDING待盘/COUNTED已盘/RECOUNT待复盘/CONFIRMED已确认/ADJUSTED已调整
    first_counter   VARCHAR(64),
    second_counter  VARCHAR(64),
    first_count_time TIMESTAMP,
    second_count_time TIMESTAMP,
    confirm_time    TIMESTAMP,
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_stocktake_item PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_st_item_task ON wms_stocktake_item(task_id);
CREATE INDEX idx_wms_st_item_sku ON wms_stocktake_item(sku);
CREATE INDEX idx_wms_st_item_location ON wms_stocktake_item(location_code);


-- 注释
ALTER TABLE wms_stocktake_task COMMENT='盘点任务表';
ALTER TABLE wms_stocktake_item COMMENT='盘点明细表';
