-- ============================================================
-- X WMS 补货管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 补货规则/补货任务/补货在途
-- ============================================================

-- 1. 补货规则表
CREATE TABLE wms_replenish_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL,
    rule_name       VARCHAR(128) NOT NULL,
    sku             VARCHAR(64),
    owner_code      VARCHAR(64),
    warehouse_code  VARCHAR(64),
    pick_area_code  VARCHAR(64),            -- 拣货区编码
    storage_area_code VARCHAR(64),          -- 存储区编码
    safety_stock    DECIMAL(14,4)  DEFAULT 0, -- 安全库存
    reorder_point   DECIMAL(14,4)  DEFAULT 0, -- 补货点(触发阈值)
    replenish_qty   DECIMAL(14,4),            -- 补货量(固定批量)
    max_stock       DECIMAL(14,4),            -- 补货上限
    replenish_type  VARCHAR(16)  DEFAULT 'TO_LEVEL', -- FIXED固定批量/TO_LEVEL按需补到上限/EOQ经济订货量
    priority        SMALLINT     DEFAULT 5, -- 优先级 1-10, 1最高
    abc_class       VARCHAR(8),             -- A/B/C类差异化配置
    lead_time_hours SMALLINT     DEFAULT 2, -- 补货提前期(小时)
    status          VARCHAR(16)  DEFAULT 'ENABLED',
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    deleted         TINYINT(1)     DEFAULT 0,
    CONSTRAINT pk_wms_replenish_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_replenish_rule_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_repl_rule_sku ON wms_replenish_rule(sku);
CREATE INDEX idx_wms_repl_rule_area ON wms_replenish_rule(pick_area_code);

-- 2. 补货任务表
CREATE TABLE wms_replenish_task (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    task_no         VARCHAR(64)  NOT NULL,
    rule_id         BIGINT,
    sku             VARCHAR(64)  NOT NULL,
    barcode         VARCHAR(64),
    product_name    VARCHAR(256),
    batch_no        VARCHAR(64),
    owner_code      VARCHAR(64),
    warehouse_code  VARCHAR(64),
    from_location   VARCHAR(64),            -- 源库位(存储区)
    to_location     VARCHAR(64),            -- 目标库位(拣货区)
    plan_qty        DECIMAL(14,4) NOT NULL,   -- 计划补货量
    actual_qty      DECIMAL(14,4) DEFAULT 0,  -- 实际补货量
    replenish_type  VARCHAR(16),            -- NORMAL普通/URGENT紧急/CROSSDOCK越库/PRESALE大促预补/PERIODIC周期
    priority        SMALLINT     DEFAULT 5,
    status          VARCHAR(32)  NOT NULL,  -- PENDING待处理/ASSIGNED已分配/PICKING拣货中/PICKED已拣货/PUTAWAYING上架中/COMPLETED完成/EXCEPTION异常/CANCELLED取消
    trigger_source  VARCHAR(32),            -- AUTO自动/MANUAL手动/SHORTAGE缺货触发/SCHEDULE定时/PRESALE大促
    assignee        VARCHAR(64),
    assign_time     TIMESTAMP,
    start_time      TIMESTAMP,
    finish_time     TIMESTAMP,
    exception_reason VARCHAR(512),
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    deleted         TINYINT(1)     DEFAULT 0,
    CONSTRAINT pk_wms_replenish_task PRIMARY KEY (id),
    CONSTRAINT uk_wms_replenish_task_no UNIQUE (task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_repl_task_status ON wms_replenish_task(status);
CREATE INDEX idx_wms_repl_task_sku ON wms_replenish_task(sku);
CREATE INDEX idx_wms_repl_task_priority ON wms_replenish_task(priority);
CREATE INDEX idx_wms_repl_task_owner ON wms_replenish_task(owner_code_col);

-- 3. 补货在途库存表
CREATE TABLE wms_replenish_in_transit (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    task_id         BIGINT    NOT NULL,
    sku             VARCHAR(64)  NOT NULL,
    batch_no        VARCHAR(64),
    from_location   VARCHAR(64)  NOT NULL,
    to_location     VARCHAR(64)  NOT NULL,
    qty             DECIMAL(14,4) NOT NULL,
    status          VARCHAR(16)  DEFAULT 'IN_TRANSIT', -- IN_TRANSIT在途/ARRIVED已到/CANCELLED取消
    owner_code      VARCHAR(64),
    warehouse_code  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    arrived_time    TIMESTAMP,
    CONSTRAINT pk_wms_replenish_transit PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_repl_transit_task ON wms_replenish_in_transit(task_id);
CREATE INDEX idx_wms_repl_transit_sku ON wms_replenish_in_transit(sku);


-- 注释
ALTER TABLE wms_replenish_rule COMMENT='补货规则表';
ALTER TABLE wms_replenish_task COMMENT='补货任务表';
ALTER TABLE wms_replenish_in_transit COMMENT='补货在途库存表';
