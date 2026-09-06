-- ============================================================
-- X WMS 库存冻结管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 库存冻结单/库存冻结明细/冻结原因/解冻记录
-- ============================================================

-- 1. 库存冻结单表
CREATE TABLE wms_inventory_freeze (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    freeze_no       VARCHAR(64)  NOT NULL, -- 冻结单号
    freeze_type     VARCHAR(32)  NOT NULL, -- 冻结类型: QC质检/STOCKTAKE盘点/EXCEPTION异常/RECALL召回/EXPIRE过期/CUSTOMER客户/OTHER其他
    freeze_reason   VARCHAR(512), -- 冻结原因
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    status          VARCHAR(32) DEFAULT 'FROZEN', -- FROZEN已冻结/PARTIAL部分解冻/UNFROZEN已解冻/CANCELLED已取消
    total_sku_count INT, -- 冻结SKU数
    total_qty       INT(18,4), -- 冻结总数量
    freeze_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    unfreeze_time   TIMESTAMP,
    operator        VARCHAR(64),
    approver        VARCHAR(64),
    approve_time    TIMESTAMP,
    remark          VARCHAR(512),
    ref_no          VARCHAR(64), -- 关联单号(质检单/盘点单等)
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_inv_freeze PRIMARY KEY (id),
    CONSTRAINT uk_wms_inv_freeze_no UNIQUE (freeze_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inv_freeze_warehouse ON wms_inventory_freeze(warehouse_code);
CREATE INDEX idx_wms_inv_freeze_type ON wms_inventory_freeze(freeze_type);
CREATE INDEX idx_wms_inv_freeze_status ON wms_inventory_freeze(status);

-- 2. 库存冻结明细表
CREATE TABLE wms_inventory_freeze_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    freeze_no       VARCHAR(64)  NOT NULL,
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    location_code   VARCHAR(64),
    batch_no        VARCHAR(64),
    serial_no       VARCHAR(64),
    container_no    VARCHAR(64),
    freeze_qty      INT(18,4)  NOT NULL, -- 冻结数量
    unfreeze_qty    INT(18,4) DEFAULT 0, -- 已解冻数量
    remain_qty      INT(18,4), -- 剩余冻结数量
    before_status   VARCHAR(32), -- 冻结前库存状态
    after_status    VARCHAR(32), -- 冻结后库存状态
    status          VARCHAR(32) DEFAULT 'FROZEN', -- FROZEN/PARTIAL/UNFROZEN
    freeze_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    unfreeze_time   TIMESTAMP,
    operator        VARCHAR(64),
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_inv_freeze_dtl PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inv_freeze_dtl_no ON wms_inventory_freeze_detail(freeze_no);
CREATE INDEX idx_wms_inv_freeze_dtl_sku ON wms_inventory_freeze_detail(sku_code);
CREATE INDEX idx_wms_inv_freeze_dtl_loc ON wms_inventory_freeze_detail(location_code);
CREATE INDEX idx_wms_inv_freeze_dtl_batch ON wms_inventory_freeze_detail(batch_no);
CREATE INDEX idx_wms_inv_freeze_dtl_status ON wms_inventory_freeze_detail(status);

-- 3. 冻结原因配置表
CREATE TABLE wms_freeze_reason (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    reason_code     VARCHAR(64)  NOT NULL, -- 原因编码
    reason_name     VARCHAR(128) NOT NULL, -- 原因名称
    freeze_type     VARCHAR(32)  NOT NULL, -- 冻结类型
    need_approve    VARCHAR(8)   DEFAULT 'N', -- 是否需要审批
    auto_unfreeze   VARCHAR(8)   DEFAULT 'N', -- 是否自动解冻
    auto_unfreeze_hours INT, -- 自动解冻小时数
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_freeze_reason PRIMARY KEY (id),
    CONSTRAINT uk_wms_freeze_reason_code UNIQUE (reason_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 解冻记录表
CREATE TABLE wms_inventory_unfreeze_log (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    unfreeze_no     VARCHAR(64)  NOT NULL, -- 解冻单号
    freeze_no       VARCHAR(64)  NOT NULL, -- 原冻结单号
    warehouse_code  VARCHAR(64)  NOT NULL,
    sku_code        VARCHAR(64)  NOT NULL,
    location_code   VARCHAR(64),
    batch_no        VARCHAR(64),
    unfreeze_qty    INT(18,4)  NOT NULL, -- 解冻数量
    unfreeze_type   VARCHAR(32), -- 解冻类型: FULL全部/PART部分/AUTO自动
    unfreeze_reason VARCHAR(512), -- 解冻原因
    operator        VARCHAR(64),
    approver        VARCHAR(64),
    approve_time    TIMESTAMP,
    unfreeze_time   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_inv_unfreeze PRIMARY KEY (id),
    CONSTRAINT uk_wms_inv_unfreeze_no UNIQUE (unfreeze_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inv_unfreeze_freeze ON wms_inventory_unfreeze_log(freeze_no);
CREATE INDEX idx_wms_inv_unfreeze_sku ON wms_inventory_unfreeze_log(sku_code);


-- 注释
ALTER TABLE wms_inventory_freeze COMMENT='库存冻结单表';
ALTER TABLE wms_inventory_freeze_detail COMMENT='库存冻结明细表';
ALTER TABLE wms_freeze_reason COMMENT='冻结原因配置表';
ALTER TABLE wms_inventory_unfreeze_log COMMENT='解冻记录表';
