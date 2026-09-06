-- ============================================================
-- X WMS 库存流水管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 库存流水主表/库存流水汇总表/库存对账表/库存对账差异表
-- ============================================================

-- 1. 库存流水主表
CREATE TABLE wms_inventory_transaction (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    txn_no          VARCHAR(64)  NOT NULL, -- 流水号
    txn_type        VARCHAR(32)  NOT NULL, -- 流水类型: INBOUND入库/OUTBOUND出库/TRANSFER调拨/ADJUST调整/MOVE移库/RESERVE预占/RELEASE释放/FROZEN冻结/UNFROZEN解冻/RETURN退货/CROSSDOCK越库/VAS增值
    txn_direction   VARCHAR(8)   NOT NULL, -- 方向: IN增加/OUT减少/NONE无变化
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    location_code   VARCHAR(64),
    batch_no        VARCHAR(64),
    serial_no       VARCHAR(64),
    container_no    VARCHAR(64),
    quantity        INT(18,4)  NOT NULL, -- 变动数量(正数)
    before_qty      INT(18,4), -- 变动前数量
    after_qty       INT(18,4), -- 变动后数量
    unit_cost       INT(18,4), -- 单位成本
    total_cost      INT(18,4), -- 总成本
    business_type   VARCHAR(64), -- 业务类型
    business_no     VARCHAR(64), -- 业务单号
    business_line   INT, -- 业务行号
    ref_txn_no      VARCHAR(64), -- 关联流水号
    operator        VARCHAR(64),
    operate_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(512),
    trace_id        VARCHAR(64), -- 链路追踪ID
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_inv_txn PRIMARY KEY (id),
    CONSTRAINT uk_wms_inv_txn_no UNIQUE (txn_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inv_txn_warehouse ON wms_inventory_transaction(warehouse_code);
CREATE INDEX idx_wms_inv_txn_sku ON wms_inventory_transaction(sku_code);
CREATE INDEX idx_wms_inv_txn_type ON wms_inventory_transaction(txn_type);
CREATE INDEX idx_wms_inv_txn_biz ON wms_inventory_transaction(business_no);
CREATE INDEX idx_wms_inv_txn_time ON wms_inventory_transaction(operate_time);
CREATE INDEX idx_wms_inv_txn_batch ON wms_inventory_transaction(batch_no);

-- 2. 库存流水汇总表（按日/SKU/库位汇总）
CREATE TABLE wms_inventory_txn_summary (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    summary_date    DATE          NOT NULL, -- 汇总日期
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64)  NOT NULL,
    location_code   VARCHAR(64),
    batch_no        VARCHAR(64),
    begin_qty       INT(18,4), -- 期初数量
    inbound_qty     INT(18,4), -- 入库数量
    outbound_qty    INT(18,4), -- 出库数量
    adjust_in_qty   INT(18,4), -- 调整增加
    adjust_out_qty  INT(18,4), -- 调整减少
    transfer_in_qty INT(18,4), -- 调入数量
    transfer_out_qty INT(18,4), -- 调出数量
    end_qty         INT(18,4), -- 期末数量
    begin_cost      INT(18,4), -- 期初成本
    inbound_cost    INT(18,4), -- 入库成本
    outbound_cost   INT(18,4), -- 出库成本
    end_cost        INT(18,4), -- 期末成本
    txn_count       INT, -- 流水笔数
    summary_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_inv_txn_sum PRIMARY KEY (id),
    CONSTRAINT uk_wms_inv_txn_sum UNIQUE (summary_date, warehouse_code, sku_code, location_code, batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inv_txn_sum_date ON wms_inventory_txn_summary(summary_date);
CREATE INDEX idx_wms_inv_txn_sum_sku ON wms_inventory_txn_summary(sku_code);

-- 3. 库存对账表
CREATE TABLE wms_inventory_reconcile (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    reconcile_no    VARCHAR(64)  NOT NULL, -- 对账号
    reconcile_type  VARCHAR(32)  NOT NULL, -- 对账类型: DAILY日结/MONTHLY月结/MANUAL手动
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    reconcile_date  DATE          NOT NULL, -- 对账日期
    begin_qty       INT(18,4), -- 系统期初
    end_qty         INT(18,4), -- 系统期末
    actual_begin_qty INT(18,4), -- 实际期初
    actual_end_qty  INT(18,4), -- 实际期末
    diff_qty        INT(18,4), -- 差异数量
    diff_count      INT, -- 差异SKU数
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING待处理/PROCESSING处理中/RESOLVED已解决/IGNORED已忽略
    operator        VARCHAR(64),
    operate_time    TIMESTAMP,
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_inv_reconcile PRIMARY KEY (id),
    CONSTRAINT uk_wms_inv_reconcile_no UNIQUE (reconcile_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inv_reconcile_date ON wms_inventory_reconcile(reconcile_date);
CREATE INDEX idx_wms_inv_reconcile_status ON wms_inventory_reconcile(status);

-- 4. 库存对账差异表
CREATE TABLE wms_inventory_reconcile_diff (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    diff_id         VARCHAR(64)  NOT NULL, -- 差异ID
    reconcile_no    VARCHAR(64)  NOT NULL, -- 对账号
    warehouse_code  VARCHAR(64)  NOT NULL,
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    location_code   VARCHAR(64),
    batch_no        VARCHAR(64),
    system_qty      INT(18,4), -- 系统数量
    actual_qty      INT(18,4), -- 实际数量
    diff_qty        INT(18,4), -- 差异数量
    diff_type       VARCHAR(32), -- 差异类型: SHORTAGE盘亏/OVERAGE盘盈/COST_DIFF成本差异
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING/PROCESSING/RESOLVED/IGNORED
    resolve_action  VARCHAR(512), -- 处理措施
    resolved_by     VARCHAR(64),
    resolved_time   TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_inv_reconcile_diff PRIMARY KEY (id),
    CONSTRAINT uk_wms_inv_reconcile_diff_id UNIQUE (diff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inv_reconcile_diff_no ON wms_inventory_reconcile_diff(reconcile_no);
CREATE INDEX idx_wms_inv_reconcile_diff_sku ON wms_inventory_reconcile_diff(sku_code);


-- 注释
ALTER TABLE wms_inventory_transaction COMMENT='库存流水主表';
ALTER TABLE wms_inventory_txn_summary COMMENT='库存流水汇总表';
ALTER TABLE wms_inventory_reconcile COMMENT='库存对账表';
ALTER TABLE wms_inventory_reconcile_diff COMMENT='库存对账差异表';
