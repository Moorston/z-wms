-- ============================================================
-- X WMS 库存调整管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 库存调整单/库存调整明细/库存冻结单/库存调整流水
-- ============================================================

-- 1. 库存调整单表
CREATE TABLE wms_inventory_adjust (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    adjust_no       VARCHAR(64)  NOT NULL, -- 调整单号
    adjust_type     VARCHAR(32)  NOT NULL, -- PROFIT盘盈/LOSS盘亏/DAMAGE损坏/EXPIRE过期/TRANSFER调拨差异/MANUAL手工调整
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    ref_type        VARCHAR(32), -- 关联类型: STOCKTAKE盘点/RETURN退货/MANUAL手工
    ref_no          VARCHAR(64), -- 关联单号
    status          VARCHAR(32) DEFAULT 'DRAFT', -- DRAFT草稿/SUBMITTED已提交/APPROVED已审批/EXECUTED已执行/CANCELLED已取消
    total_sku       INT    DEFAULT 0, -- SKU种类数
    total_qty       INT(18,4)  DEFAULT 0, -- 总调整数量
    total_amount    INT(18,4)  DEFAULT 0, -- 总调整金额
    reason          VARCHAR(512), -- 调整原因
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    approved_by     VARCHAR(64),
    executed_by     VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    approved_time   TIMESTAMP,
    executed_time   TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_inventory_adjust PRIMARY KEY (id),
    CONSTRAINT uk_wms_inventory_adjust_no UNIQUE (adjust_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_adjust_warehouse ON wms_inventory_adjust(warehouse_code);
CREATE INDEX idx_wms_adjust_status ON wms_inventory_adjust(status);
CREATE INDEX idx_wms_adjust_type ON wms_inventory_adjust(adjust_type);

-- 2. 库存调整明细表
CREATE TABLE wms_inventory_adjust_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    adjust_no       VARCHAR(64)  NOT NULL,
    line_no         INT    NOT NULL,
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    batch_no        VARCHAR(128),
    location_code   VARCHAR(64),
    container_no    VARCHAR(64),
    before_qty      INT(18,4), -- 调整前数量
    adjust_qty      INT(18,4)  NOT NULL, -- 调整数量(正数增加/负数减少)
    after_qty       INT(18,4), -- 调整后数量
    unit_price      INT(18,4), -- 单价
    adjust_amount   INT(18,4), -- 调整金额
    reason          VARCHAR(512),
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_inventory_adjust_d PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_adjust_d_adjust ON wms_inventory_adjust_detail(adjust_no);
CREATE INDEX idx_wms_adjust_d_sku ON wms_inventory_adjust_detail(sku_code);

-- 3. 库存冻结单表
CREATE TABLE wms_inventory_freeze (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    freeze_no       VARCHAR(64)  NOT NULL, -- 冻结单号
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    freeze_type     VARCHAR(32)  NOT NULL, -- QC质检冻结/RETURN退货冻结/DAMAGE损坏冻结/MANUAL手工冻结
    ref_type        VARCHAR(32), -- 关联类型
    ref_no          VARCHAR(64), -- 关联单号
    status          VARCHAR(32) DEFAULT 'FROZEN', -- FROZEN已冻结/RELEASED已解冻
    sku_code        VARCHAR(64),
    batch_no        VARCHAR(128),
    location_code   VARCHAR(64),
    freeze_qty      INT(18,4)  NOT NULL, -- 冻结数量
    release_qty     INT(18,4)  DEFAULT 0, -- 已解冻数量
    reason          VARCHAR(512),
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    released_by     VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    released_time   TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_inventory_freeze PRIMARY KEY (id),
    CONSTRAINT uk_wms_inventory_freeze_no UNIQUE (freeze_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_freeze_warehouse ON wms_inventory_freeze(warehouse_code);
CREATE INDEX idx_wms_freeze_status ON wms_inventory_freeze(status);
CREATE INDEX idx_wms_freeze_sku ON wms_inventory_freeze(sku_code);

-- 4. 库存调整流水表
CREATE TABLE wms_inventory_adjust_log (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    log_no          VARCHAR(64)  NOT NULL, -- 流水号
    adjust_no       VARCHAR(64), -- 关联调整单号
    freeze_no       VARCHAR(64), -- 关联冻结单号
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64)  NOT NULL,
    batch_no        VARCHAR(128),
    location_code   VARCHAR(64),
    action_type     VARCHAR(32)  NOT NULL, -- ADJUST调整/FREEZE冻结/RELEASE解冻
    before_qty      INT(18,4), -- 操作前数量
    change_qty      INT(18,4)  NOT NULL, -- 变化数量
    after_qty       INT(18,4), -- 操作后数量
    before_frozen   INT(18,4), -- 操作前冻结数量
    after_frozen    INT(18,4), -- 操作后冻结数量
    operator        VARCHAR(64),
    action_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_inventory_adjust_log PRIMARY KEY (id),
    CONSTRAINT uk_wms_inventory_adjust_log UNIQUE (log_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_adjust_log_warehouse ON wms_inventory_adjust_log(warehouse_code);
CREATE INDEX idx_wms_adjust_log_sku ON wms_inventory_adjust_log(sku_code);
CREATE INDEX idx_wms_adjust_log_time ON wms_inventory_adjust_log(action_time);


-- 注释
ALTER TABLE wms_inventory_adjust COMMENT='库存调整单表';
ALTER TABLE wms_inventory_adjust_detail COMMENT='库存调整明细表';
ALTER TABLE wms_inventory_freeze COMMENT='库存冻结单表';
ALTER TABLE wms_inventory_adjust_log COMMENT='库存调整流水表';
