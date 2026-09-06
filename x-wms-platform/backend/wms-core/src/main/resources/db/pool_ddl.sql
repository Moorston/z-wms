-- ============================================================
-- X WMS 库存共享/分配池管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 共享规则/分配池/池化库存/池化分配记录
-- ============================================================

-- 1. 库存共享规则表
CREATE TABLE wms_share_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL, -- 规则编码
    rule_name       VARCHAR(128) NOT NULL, -- 规则名称
    warehouse_code  VARCHAR(64)  NOT NULL, -- 仓库
    pool_code       VARCHAR(64)  NOT NULL, -- 关联分配池
    share_type      VARCHAR(32)  NOT NULL, -- 共享类型: OWNER货主共享/SKU品类共享/ALL全共享
    source_owner    VARCHAR(64), -- 源货主
    target_owner    VARCHAR(64), -- 目标货主
    source_sku      VARCHAR(64), -- 源SKU
    target_sku      VARCHAR(64), -- 目标SKU
    category_code   VARCHAR(64), -- 品类
    share_ratio     INT(10,4) DEFAULT 100, -- 共享比例(%)
    priority        SMALLINT     DEFAULT 5, -- 优先级
    effective_date  DATE, -- 生效日期
    expire_date     DATE, -- 失效日期
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_share_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_share_rule_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_share_rule_pool ON wms_share_rule(pool_code);
CREATE INDEX idx_wms_share_rule_wh ON wms_share_rule(warehouse_code);

-- 2. 分配池表
CREATE TABLE wms_allocation_pool (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    pool_code       VARCHAR(64)  NOT NULL, -- 池编码
    pool_name       VARCHAR(128) NOT NULL, -- 池名称
    warehouse_code  VARCHAR(64)  NOT NULL, -- 仓库
    pool_type       VARCHAR(32)  NOT NULL, -- 池类型: SHARED共享池/DEDICATED专用池/VIRTUAL虚拟池
    owner_code      VARCHAR(64), -- 所属货主(专用池)
    category_code   VARCHAR(64), -- 品类
    sku_code        VARCHAR(64), -- SKU
    max_capacity    INT(18,4), -- 最大容量
    min_threshold   INT(18,4), -- 最小阈值
    allocation_mode VARCHAR(32) DEFAULT 'FIFO', -- 分配模式: FIFO/FEFO/LIFO/PRIORITY
    priority        SMALLINT     DEFAULT 5, -- 池优先级
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_alloc_pool PRIMARY KEY (id),
    CONSTRAINT uk_wms_alloc_pool_code UNIQUE (pool_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_alloc_pool_wh ON wms_allocation_pool(warehouse_code);
CREATE INDEX idx_wms_alloc_pool_type ON wms_allocation_pool(pool_type);

-- 3. 池化库存表
CREATE TABLE wms_pool_inventory (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    pool_code       VARCHAR(64)  NOT NULL, -- 池编码
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    batch_no        VARCHAR(64),
    location_code   VARCHAR(64),
    total_qty       INT(18,4) DEFAULT 0, -- 池内总库存
    available_qty   INT(18,4) DEFAULT 0, -- 可用库存
    allocated_qty   INT(18,4) DEFAULT 0, -- 已分配库存
    reserved_qty    INT(18,4) DEFAULT 0, -- 预留库存
    frozen_qty      INT(18,4) DEFAULT 0, -- 冻结库存
    unit_cost       INT(18,4), -- 单位成本
    total_cost      INT(18,4), -- 总成本
    last_inbound_time TIMESTAMP, -- 最后入库时间
    last_outbound_time TIMESTAMP, -- 最后出库时间
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_pool_inv PRIMARY KEY (id),
    CONSTRAINT uk_wms_pool_inv UNIQUE (pool_code, warehouse_code, sku_code, batch_no, location_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_pool_inv_pool ON wms_pool_inventory(pool_code);
CREATE INDEX idx_wms_pool_inv_sku ON wms_pool_inventory(sku_code);
CREATE INDEX idx_wms_pool_inv_wh ON wms_pool_inventory(warehouse_code);

-- 4. 池化分配记录表
CREATE TABLE wms_pool_allocation (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    allocation_id   VARCHAR(64)  NOT NULL, -- 分配ID
    pool_code       VARCHAR(64)  NOT NULL, -- 来源池
    warehouse_code  VARCHAR(64)  NOT NULL,
    order_no        VARCHAR(64), -- 订单号
    order_line      INT, -- 订单行
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    batch_no        VARCHAR(64),
    location_code   VARCHAR(64),
    allocated_qty   INT(18,4)  NOT NULL, -- 分配数量
    picked_qty      INT(18,4) DEFAULT 0, -- 已拣数量
    remain_qty      INT(18,4), -- 剩余数量
    source_owner    VARCHAR(64), -- 源货主
    target_owner    VARCHAR(64), -- 目标货主
    share_rule_code VARCHAR(64), -- 共享规则
    status          VARCHAR(32) DEFAULT 'ALLOCATED', -- ALLOCATED/PICKING/PICKED/RELEASED/CANCELLED
    allocate_time   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    release_time    TIMESTAMP,
    operator        VARCHAR(64),
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_pool_alloc PRIMARY KEY (id),
    CONSTRAINT uk_wms_pool_alloc_id UNIQUE (allocation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_pool_alloc_pool ON wms_pool_allocation(pool_code);
CREATE INDEX idx_wms_pool_alloc_order ON wms_pool_allocation(order_no);
CREATE INDEX idx_wms_pool_alloc_sku ON wms_pool_allocation(sku_code);
CREATE INDEX idx_wms_pool_alloc_status ON wms_pool_allocation(status);


-- 注释
ALTER TABLE wms_share_rule COMMENT='库存共享规则表';
ALTER TABLE wms_allocation_pool COMMENT='分配池表';
ALTER TABLE wms_pool_inventory COMMENT='池化库存表';
ALTER TABLE wms_pool_allocation COMMENT='池化分配记录表';
