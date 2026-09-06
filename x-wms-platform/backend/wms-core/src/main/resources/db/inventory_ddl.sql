-- ============================================================
-- X WMS 库存管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 库存台账/库存流水/库存调整单/库存冻结记录
-- ============================================================

-- 1. 库存台账表
CREATE TABLE wms_inventory (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    inventory_no    VARCHAR(64)  NOT NULL,
    warehouse_code  VARCHAR(64)  NOT NULL,
    location_code   VARCHAR(64)  NOT NULL,
    sku_code        VARCHAR(64)  NOT NULL,
    batch_no        VARCHAR(128),
    owner_code_col  VARCHAR(64),
    quantity        INT(18,4) DEFAULT 0, -- 总数量
    available_qty   INT(18,4) DEFAULT 0, -- 可用数量
    allocated_qty   INT(18,4) DEFAULT 0, -- 预占数量
    picking_qty     INT(18,4) DEFAULT 0, -- 拣货中数量
    frozen_qty      INT(18,4) DEFAULT 0, -- 冻结数量
    unit            VARCHAR(32),
    status          VARCHAR(32) DEFAULT 'NORMAL', -- NORMAL/FROZEN/EMPTY
    version         INT    DEFAULT 0, -- 乐观锁版本号
    last_in_time    TIMESTAMP, -- 最后入库时间
    last_out_time   TIMESTAMP, -- 最后出库时间
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_inventory PRIMARY KEY (id),
    CONSTRAINT uk_wms_inventory UNIQUE (warehouse_code, location_code, sku_code, batch_no, owner_code_col)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inv_sku ON wms_inventory(sku_code);
CREATE INDEX idx_wms_inv_batch ON wms_inventory(batch_no);
CREATE INDEX idx_wms_inv_location ON wms_inventory(location_code);
CREATE INDEX idx_wms_inv_owner ON wms_inventory(owner_code_col);

-- 2. 库存流水表
CREATE TABLE wms_inventory_transaction (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    trans_no        VARCHAR(64)  NOT NULL,
    inventory_no    VARCHAR(64),
    warehouse_code  VARCHAR(64),
    location_code   VARCHAR(64),
    sku_code        VARCHAR(64),
    batch_no        VARCHAR(128),
    owner_code_col  VARCHAR(64),
    trans_type      VARCHAR(32)  NOT NULL, -- INBOUND入库/OUTBOUND出库/TRANSFER调拨/ADJUST调整/FREEZE冻结/UNFREEZE解冻/SPLIT拆分/MERGE合并
    trans_direction VARCHAR(8)   NOT NULL, -- IN/OUT
    ref_type        VARCHAR(32), -- 关联单据类型
    ref_no          VARCHAR(64), -- 关联单号
    before_qty      INT(18,4), -- 变更前数量
    trans_qty       INT(18,4) NOT NULL, -- 变更数量
    after_qty       INT(18,4), -- 变更后数量
    before_available INT(18,4),
    trans_available INT(18,4),
    after_available INT(18,4),
    operator        VARCHAR(64),
    trans_time      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(512),
    trace_id        VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_inv_trans PRIMARY KEY (id),
    CONSTRAINT uk_wms_inv_trans_no UNIQUE (trans_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inv_trans_inv ON wms_inventory_transaction(inventory_no);
CREATE INDEX idx_wms_inv_trans_sku ON wms_inventory_transaction(sku_code);
CREATE INDEX idx_wms_inv_trans_batch ON wms_inventory_transaction(batch_no);
CREATE INDEX idx_wms_inv_trans_ref ON wms_inventory_transaction(ref_type, ref_no);
CREATE INDEX idx_wms_inv_trans_time ON wms_inventory_transaction(trans_time);

-- 3. 库存调整单表
CREATE TABLE wms_inventory_adjust (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    adjust_no       VARCHAR(64)  NOT NULL,
    adjust_type     VARCHAR(32)  NOT NULL, -- PROFIT盘盈/LOSS盘亏/DAMAGE损坏/EXPIRE过期/TRANSFER转仓
    warehouse_code  VARCHAR(64),
    location_code   VARCHAR(64),
    sku_code        VARCHAR(64),
    batch_no        VARCHAR(128),
    owner_code_col  VARCHAR(64),
    before_qty      INT(18,4),
    adjust_qty      INT(18,4) NOT NULL,
    after_qty       INT(18,4),
    reason          VARCHAR(512),
    status          VARCHAR(32) DEFAULT 'CREATED', -- CREATED/APPROVED/DONE/CANCELLED
    approver        VARCHAR(64),
    approve_time    TIMESTAMP,
    operator        VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_inv_adjust PRIMARY KEY (id),
    CONSTRAINT uk_wms_inv_adjust_no UNIQUE (adjust_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inv_adjust_sku ON wms_inventory_adjust(sku_code);
CREATE INDEX idx_wms_inv_adjust_status ON wms_inventory_adjust(status);

-- 4. 库存冻结记录表
CREATE TABLE wms_inventory_freeze (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    freeze_no       VARCHAR(64)  NOT NULL,
    inventory_no    VARCHAR(64),
    warehouse_code  VARCHAR(64),
    location_code   VARCHAR(64),
    sku_code        VARCHAR(64),
    batch_no        VARCHAR(128),
    owner_code_col  VARCHAR(64),
    freeze_qty      INT(18,4) NOT NULL,
    unfreeze_qty    INT(18,4) DEFAULT 0,
    freeze_type     VARCHAR(32), -- QC质检/STOCKTAKE盘点/DAMAGE损坏/ORDER订单
    ref_no          VARCHAR(64), -- 关联单号
    status          VARCHAR(32) DEFAULT 'FROZEN', -- FROZEN/PARTIAL_UNFROZEN/UNFROZEN
    operator        VARCHAR(64),
    freeze_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    unfreeze_time   TIMESTAMP,
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_inv_freeze PRIMARY KEY (id),
    CONSTRAINT uk_wms_inv_freeze_no UNIQUE (freeze_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inv_freeze_inv ON wms_inventory_freeze(inventory_no);
CREATE INDEX idx_wms_inv_freeze_sku ON wms_inventory_freeze(sku_code);
CREATE INDEX idx_wms_inv_freeze_status ON wms_inventory_freeze(status);


-- 注释
ALTER TABLE wms_inventory COMMENT='库存台账表';
ALTER TABLE wms_inventory_transaction COMMENT='库存流水表';
ALTER TABLE wms_inventory_adjust COMMENT='库存调整单表';
ALTER TABLE wms_inventory_freeze COMMENT='库存冻结记录表';
