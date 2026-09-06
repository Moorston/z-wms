-- ============================================================
-- X WMS 库存预占管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 库存预占单/库存预占明细/库存预占流水/库存锁
-- ============================================================

-- 1. 库存预占单表
CREATE TABLE wms_inventory_reserve (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    reserve_no      VARCHAR(64)  NOT NULL, -- 预占单号
    reserve_type    VARCHAR(32)  NOT NULL, -- OUTBOUND出库/TRANSFER调拨/VAS增值/REPLENISH补货
    ref_type        VARCHAR(32), -- 关联单据类型
    ref_no          VARCHAR(64), -- 关联单号
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    status          VARCHAR(32) DEFAULT 'RESERVED', -- RESERVED已预占/PARTIAL部分释放/RELEASED已释放/CONFIRMED已确认/CANCELLED已取消
    total_sku       INT    DEFAULT 0,
    total_qty       INT(18,4)  DEFAULT 0,
    reserved_qty    INT(18,4)  DEFAULT 0,
    released_qty    INT(18,4)  DEFAULT 0,
    confirmed_qty   INT(18,4)  DEFAULT 0,
    expire_time     TIMESTAMP, -- 预占过期时间
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    confirmed_time  TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_inventory_reserve PRIMARY KEY (id),
    CONSTRAINT uk_wms_inventory_reserve_no UNIQUE (reserve_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_reserve_warehouse ON wms_inventory_reserve(warehouse_code);
CREATE INDEX idx_wms_reserve_status ON wms_inventory_reserve(status);
CREATE INDEX idx_wms_reserve_ref ON wms_inventory_reserve(ref_type, ref_no);
CREATE INDEX idx_wms_reserve_expire ON wms_inventory_reserve(expire_time);

-- 2. 库存预占明细表
CREATE TABLE wms_inventory_reserve_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    reserve_no      VARCHAR(64)  NOT NULL,
    line_no         INT    NOT NULL,
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    batch_no        VARCHAR(128),
    location_code   VARCHAR(64),
    container_no    VARCHAR(64),
    plan_qty        INT(18,4)  NOT NULL, -- 计划预占数量
    reserved_qty    INT(18,4)  DEFAULT 0, -- 已预占数量
    released_qty    INT(18,4)  DEFAULT 0, -- 已释放数量
    confirmed_qty   INT(18,4)  DEFAULT 0, -- 已确认数量
    status          VARCHAR(32) DEFAULT 'RESERVED', -- RESERVED/PARTIAL/RELEASED/CONFIRMED
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_inventory_reserve_d PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_reserve_d_reserve ON wms_inventory_reserve_detail(reserve_no);
CREATE INDEX idx_wms_reserve_d_sku ON wms_inventory_reserve_detail(sku_code);

-- 3. 库存预占流水表
CREATE TABLE wms_inventory_reserve_log (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    log_no          VARCHAR(64)  NOT NULL,
    reserve_no      VARCHAR(64),
    ref_type        VARCHAR(32),
    ref_no          VARCHAR(64),
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64)  NOT NULL,
    batch_no        VARCHAR(128),
    location_code   VARCHAR(64),
    action_type     VARCHAR(32)  NOT NULL, -- RESERVE预占/RELEASE释放/CONFIRM确认/EXPIRE过期
    before_qty      INT(18,4), -- 操作前预占数量
    change_qty      INT(18,4)  NOT NULL, -- 变化数量
    after_qty       INT(18,4), -- 操作后预占数量
    operator        VARCHAR(64),
    action_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_inventory_reserve_log PRIMARY KEY (id),
    CONSTRAINT uk_wms_inventory_reserve_log UNIQUE (log_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_reserve_log_sku ON wms_inventory_reserve_log(sku_code);
CREATE INDEX idx_wms_reserve_log_time ON wms_inventory_reserve_log(action_time);

-- 4. 库存锁表（Redis预占失败时的数据库兜底）
CREATE TABLE wms_inventory_lock (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    lock_key        VARCHAR(256) NOT NULL, -- 锁键: warehouse:sku:batch:location
    warehouse_code  VARCHAR(64)  NOT NULL,
    sku_code        VARCHAR(64)  NOT NULL,
    batch_no        VARCHAR(128),
    location_code   VARCHAR(64),
    locked_qty      INT(18,4)  NOT NULL, -- 锁定数量
    lock_owner      VARCHAR(64), -- 锁持有者(预占单号)
    expire_time     TIMESTAMP, -- 锁过期时间
    status          VARCHAR(32) DEFAULT 'LOCKED', -- LOCKED/RELEASED/EXPIRED
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_inventory_lock PRIMARY KEY (id),
    CONSTRAINT uk_wms_inventory_lock_key UNIQUE (lock_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_lock_sku ON wms_inventory_lock(sku_code);
CREATE INDEX idx_wms_lock_expire ON wms_inventory_lock(expire_time);


-- 注释
ALTER TABLE wms_inventory_reserve COMMENT='库存预占单表';
ALTER TABLE wms_inventory_reserve_detail COMMENT='库存预占明细表';
ALTER TABLE wms_inventory_reserve_log COMMENT='库存预占流水表';
ALTER TABLE wms_inventory_lock COMMENT='库存锁表';
