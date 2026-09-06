-- ============================================================
-- X WMS 多仓协同模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 仓库间调拨单/多仓库存快照/订单分配记录/仓库协同规则
-- ============================================================

-- 1. 仓库间调拨单
CREATE TABLE wms_transfer_order (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    transfer_no     VARCHAR(64)  NOT NULL,
    transfer_type   VARCHAR(16)  DEFAULT 'NORMAL', -- NORMAL正常调拨/URGENT紧急调拨/BACKHAUL回程调拨
    from_warehouse  VARCHAR(64)  NOT NULL,
    to_warehouse    VARCHAR(64)  NOT NULL,
    status          VARCHAR(16)  DEFAULT 'DRAFT', -- DRAFT草稿/CONFIRMED已确认/IN_TRANSIT运输中/RECEIVED已收货/COMPLETED已完成/CANCELLED已取消
    priority        VARCHAR(8)   DEFAULT 'NORMAL', -- LOW/NORMAL/HIGH/URGENT
    carrier         VARCHAR(128),                  -- 承运商
    tracking_no     VARCHAR(64),                   -- 运单号
    planned_ship_date DATE,
    actual_ship_date DATE,
    planned_arrival_date DATE,
    actual_arrival_date DATE,
    total_sku       INT    DEFAULT 0,
    total_qty       DECIMAL(14,4)  DEFAULT 0,
    shipped_qty     DECIMAL(14,4)  DEFAULT 0,
    received_qty    DECIMAL(14,4)  DEFAULT 0,
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    confirmed_by    VARCHAR(64),
    confirmed_time  TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_transfer_order PRIMARY KEY (id),
    CONSTRAINT uk_wms_transfer_no UNIQUE (transfer_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_transfer_from ON wms_transfer_order(from_warehouse);
CREATE INDEX idx_wms_transfer_to ON wms_transfer_order(to_warehouse);
CREATE INDEX idx_wms_transfer_status ON wms_transfer_order(status);

-- 2. 仓库间调拨明细
CREATE TABLE wms_transfer_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    transfer_id     BIGINT  NOT NULL,
    transfer_no     VARCHAR(64) NOT NULL,
    sku             VARCHAR(64) NOT NULL,
    product_name    VARCHAR(256),
    batch_no        VARCHAR(64),
    from_location   VARCHAR(64),
    to_location     VARCHAR(64),
    planned_qty     DECIMAL(14,4) DEFAULT 0,
    shipped_qty     DECIMAL(14,4) DEFAULT 0,
    received_qty    DECIMAL(14,4) DEFAULT 0,
    difference_qty  DECIMAL(14,4) DEFAULT 0,
    unit            VARCHAR(16),
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_transfer_detail PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_transfer_dtl_id ON wms_transfer_detail(transfer_id);

-- 3. 多仓库存快照表 (定时同步, 用于跨仓查询)
CREATE TABLE wms_multi_warehouse_stock (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    warehouse_code  VARCHAR(64)  NOT NULL,
    sku             VARCHAR(64)  NOT NULL,
    batch_no        VARCHAR(64),
    available_qty   DECIMAL(14,4)  DEFAULT 0,  -- 可用库存
    allocated_qty   DECIMAL(14,4)  DEFAULT 0,  -- 预占库存
    picking_qty     DECIMAL(14,4)  DEFAULT 0,  -- 拣货中
    in_transit_qty  DECIMAL(14,4)  DEFAULT 0,  -- 在途库存(调入)
    frozen_qty      DECIMAL(14,4)  DEFAULT 0,  -- 冻结库存
    total_qty       DECIMAL(14,4)  DEFAULT 0,  -- 总库存
    safety_stock    DECIMAL(14,4)  DEFAULT 0,  -- 安全库存
    last_sync_time  TIMESTAMP,
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_multi_stock PRIMARY KEY (id),
    CONSTRAINT uk_wms_multi_stock UNIQUE (warehouse_code, sku, batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_multi_sku ON wms_multi_warehouse_stock(sku);
CREATE INDEX idx_wms_multi_wh ON wms_multi_warehouse_stock(warehouse_code);

-- 4. 订单多仓分配记录表
CREATE TABLE wms_order_allocation (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    allocation_no   VARCHAR(64)  NOT NULL,
    order_no        VARCHAR(64)  NOT NULL,
    order_type      VARCHAR(32),
    sku             VARCHAR(64),
    product_name    VARCHAR(256),
    required_qty    DECIMAL(14,4) DEFAULT 0,
    allocated_qty   DECIMAL(14,4) DEFAULT 0,
    warehouse_code  VARCHAR(64),            -- 分配到哪个仓
    allocation_rule VARCHAR(64),            -- 使用的分配规则
    allocation_reason VARCHAR(256),         -- 分配原因
    status          VARCHAR(16)  DEFAULT 'PENDING', -- PENDING待分配/ALLOCATED已分配/SHIPPED已发货/PARTIAL部分分配/FAILED分配失败
    priority        VARCHAR(8)   DEFAULT 'NORMAL',
    customer_address VARCHAR(512),          -- 收货地址(用于就近分配)
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_order_allocation PRIMARY KEY (id),
    CONSTRAINT uk_wms_alloc_no UNIQUE (allocation_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_alloc_order ON wms_order_allocation(order_no);
CREATE INDEX idx_wms_alloc_wh ON wms_order_allocation(warehouse_code);
CREATE INDEX idx_wms_alloc_status ON wms_order_allocation(status);


-- 注释
ALTER TABLE wms_transfer_order COMMENT='仓库间调拨单';
ALTER TABLE wms_transfer_detail COMMENT='仓库间调拨明细';
ALTER TABLE wms_multi_warehouse_stock COMMENT='多仓库存快照表';
ALTER TABLE wms_order_allocation COMMENT='订单多仓分配记录表';
