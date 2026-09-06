-- ============================================================
-- X WMS 库存移库管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 移库单/移库明细/移库任务/移库流水
-- ============================================================

-- 1. 移库单表
CREATE TABLE wms_move_order (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    move_no         VARCHAR(64)  NOT NULL, -- 移库单号
    move_type       VARCHAR(32)  NOT NULL, -- NORMAL普通移库/BATCH批量移库/AUTO自动移库/REPLENISH补货移库/ADJUST调整移库
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    from_area       VARCHAR(64), -- 来源库区
    to_area         VARCHAR(64), -- 目标库区
    status          VARCHAR(32) DEFAULT 'DRAFT', -- DRAFT草稿/RELEASED已下发/EXECUTING执行中/PARTIAL部分完成/COMPLETED已完成/CANCELLED已取消
    total_sku       INT    DEFAULT 0,
    total_qty       INT(18,4)  DEFAULT 0,
    moved_qty       INT(18,4)  DEFAULT 0,
    priority        SMALLINT     DEFAULT 5,
    reason          VARCHAR(512),
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    executed_by     VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    started_time    TIMESTAMP,
    completed_time  TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_move_order PRIMARY KEY (id),
    CONSTRAINT uk_wms_move_order_no UNIQUE (move_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_move_warehouse ON wms_move_order(warehouse_code);
CREATE INDEX idx_wms_move_status ON wms_move_order(status);
CREATE INDEX idx_wms_move_type ON wms_move_order(move_type);

-- 2. 移库明细表
CREATE TABLE wms_move_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    move_no         VARCHAR(64)  NOT NULL,
    line_no         INT    NOT NULL,
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    batch_no        VARCHAR(128),
    from_location   VARCHAR(64)  NOT NULL,
    to_location     VARCHAR(64)  NOT NULL,
    from_container  VARCHAR(64),
    to_container    VARCHAR(64),
    plan_qty        INT(18,4)  NOT NULL, -- 计划数量
    moved_qty       INT(18,4)  DEFAULT 0, -- 已移数量
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING待移/MOVING移库中/COMPLETED已完成/CANCELLED已取消
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_move_detail PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_move_d_move ON wms_move_detail(move_no);
CREATE INDEX idx_wms_move_d_sku ON wms_move_detail(sku_code);
CREATE INDEX idx_wms_move_d_from ON wms_move_detail(from_location);
CREATE INDEX idx_wms_move_d_to ON wms_move_detail(to_location);

-- 3. 移库任务表（分配给拣货员/设备的任务）
CREATE TABLE wms_move_task (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    task_no         VARCHAR(64)  NOT NULL, -- 任务号
    move_no         VARCHAR(64)  NOT NULL,
    line_no         INT,
    task_type       VARCHAR(32) DEFAULT 'MANUAL', -- MANUAL人工/WCS设备
    assignee        VARCHAR(64), -- 执行人
    equipment_code  VARCHAR(64), -- 设备编码
    from_location   VARCHAR(64)  NOT NULL,
    to_location     VARCHAR(64)  NOT NULL,
    sku_code        VARCHAR(64),
    batch_no        VARCHAR(128),
    plan_qty        INT(18,4)  NOT NULL,
    moved_qty       INT(18,4)  DEFAULT 0,
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING待执行/ASSIGNED已分配/EXECUTING执行中/COMPLETED已完成/CANCELLED已取消
    assigned_time   TIMESTAMP,
    started_time    TIMESTAMP,
    completed_time  TIMESTAMP,
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_move_task PRIMARY KEY (id),
    CONSTRAINT uk_wms_move_task_no UNIQUE (task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_move_task_move ON wms_move_task(move_no);
CREATE INDEX idx_wms_move_task_assignee ON wms_move_task(assignee);
CREATE INDEX idx_wms_move_task_status ON wms_move_task(status);

-- 4. 移库流水表
CREATE TABLE wms_move_log (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    log_no          VARCHAR(64)  NOT NULL,
    move_no         VARCHAR(64),
    task_no         VARCHAR(64),
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64)  NOT NULL,
    batch_no        VARCHAR(128),
    from_location   VARCHAR(64),
    to_location     VARCHAR(64),
    from_container  VARCHAR(64),
    to_container    VARCHAR(64),
    move_qty        INT(18,4)  NOT NULL,
    operator        VARCHAR(64),
    action_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_move_log PRIMARY KEY (id),
    CONSTRAINT uk_wms_move_log_no UNIQUE (log_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_move_log_warehouse ON wms_move_log(warehouse_code);
CREATE INDEX idx_wms_move_log_sku ON wms_move_log(sku_code);
CREATE INDEX idx_wms_move_log_time ON wms_move_log(action_time);


-- 注释
ALTER TABLE wms_move_order COMMENT='移库单表';
ALTER TABLE wms_move_detail COMMENT='移库明细表';
ALTER TABLE wms_move_task COMMENT='移库任务表';
ALTER TABLE wms_move_log COMMENT='移库流水表';
