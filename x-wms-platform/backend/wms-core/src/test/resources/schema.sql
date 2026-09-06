-- WMS集成测试数据库初始化脚本
-- 使用MySQL 8.x语法（TestContainers MySQL容器）

-- 库存表（列名对齐权威 MySQL DDL inventory_ddl.sql 与 InventoryMapper 注解 SQL）
CREATE TABLE IF NOT EXISTS wms_inventory (
    id BIGINT NOT NULL AUTO_INCREMENT,
    inventory_no VARCHAR(64),
    warehouse_code VARCHAR(64) NOT NULL,
    location_code VARCHAR(64) NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    batch_no VARCHAR(128),
    owner_code_col VARCHAR(64),
    quantity DECIMAL(18,4) DEFAULT 0,
    available_qty DECIMAL(18,4) DEFAULT 0,
    allocated_qty DECIMAL(18,4) DEFAULT 0,
    picking_qty DECIMAL(18,4) DEFAULT 0,
    frozen_qty DECIMAL(18,4) DEFAULT 0,
    unit VARCHAR(32),
    status VARCHAR(32) DEFAULT 'NORMAL',
    version INT DEFAULT 0,
    last_in_time TIMESTAMP NULL DEFAULT NULL,
    last_out_time TIMESTAMP NULL DEFAULT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP NULL DEFAULT NULL,
    UNIQUE KEY uk_wms_inv (warehouse_code, location_code, sku_code, batch_no, owner_code_col),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inv_sku ON wms_inventory(sku_code);
CREATE INDEX idx_wms_inv_batch ON wms_inventory(batch_no);
CREATE INDEX idx_wms_inv_location ON wms_inventory(location_code);
CREATE INDEX idx_wms_inv_owner ON wms_inventory(owner_code_col);

-- 入库单表
CREATE TABLE IF NOT EXISTS wms_inbound_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    inbound_type VARCHAR(32),
    warehouse VARCHAR(32),
    owner_code VARCHAR(64),
    supplier_code VARCHAR(64),
    status VARCHAR(32) DEFAULT 'CREATED',
    expected_qty DECIMAL(18,4),
    received_qty DECIMAL(18,4) DEFAULT 0,
    putaway_qty DECIMAL(18,4) DEFAULT 0,
    expected_arrival_time TIMESTAMP NULL DEFAULT NULL,
    actual_arrival_time TIMESTAMP NULL DEFAULT NULL,
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    deleted TINYINT(1) DEFAULT 0,
    UNIQUE KEY uk_inbound_order_no (order_no),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_inbound_status ON wms_inbound_order(status);

-- 出库单表
CREATE TABLE IF NOT EXISTS wms_outbound_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    outbound_type VARCHAR(32),
    warehouse VARCHAR(32),
    owner_code VARCHAR(64),
    customer_code VARCHAR(64),
    status VARCHAR(32) DEFAULT 'CREATED',
    wave_no VARCHAR(64),
    sku VARCHAR(64),
    expected_qty DECIMAL(18,4),
    allocated_qty DECIMAL(18,4) DEFAULT 0,
    picked_qty DECIMAL(18,4) DEFAULT 0,
    shipped_qty DECIMAL(18,4) DEFAULT 0,
    location_code VARCHAR(64),
    batch_no VARCHAR(64),
    expected_ship_time TIMESTAMP NULL DEFAULT NULL,
    actual_ship_time TIMESTAMP NULL DEFAULT NULL,
    express_code VARCHAR(32),
    tracking_no VARCHAR(64),
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    deleted TINYINT(1) DEFAULT 0,
    UNIQUE KEY uk_outbound_order_no (order_no),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_outbound_status ON wms_outbound_order(status);
CREATE INDEX idx_outbound_wave ON wms_outbound_order(wave_no);

-- 批次表
CREATE TABLE IF NOT EXISTS wms_batch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sku VARCHAR(64) NOT NULL,
    batch_no VARCHAR(64) NOT NULL,
    warehouse VARCHAR(32) NOT NULL,
    production_date DATE,
    expire_date DATE,
    qc_status VARCHAR(32) DEFAULT 'PENDING',
    qc_no VARCHAR(64),
    status VARCHAR(32) DEFAULT 'ACTIVE',
    supplier_code VARCHAR(64),
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    deleted TINYINT(1) DEFAULT 0,
    UNIQUE KEY uk_batch (sku, batch_no, warehouse),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_batch_expire ON wms_batch(expire_date);

-- 作业任务表
CREATE TABLE IF NOT EXISTS wms_work_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_no VARCHAR(64) NOT NULL,
    task_type VARCHAR(32),
    warehouse VARCHAR(32),
    status VARCHAR(32) DEFAULT 'PENDING',
    priority INT DEFAULT 5,
    order_no VARCHAR(64),
    sku VARCHAR(64),
    location_code VARCHAR(64),
    batch_no VARCHAR(64),
    expected_qty DECIMAL(18,4),
    actual_qty DECIMAL(18,4),
    operator VARCHAR(64),
    device_id VARCHAR(64),
    actual_start_time TIMESTAMP NULL DEFAULT NULL,
    completed_time TIMESTAMP NULL DEFAULT NULL,
    exception_reason VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    deleted TINYINT(1) DEFAULT 0,
    UNIQUE KEY uk_work_task_no (task_no),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_task_status ON wms_work_task(status, priority);

-- 库位表
CREATE TABLE IF NOT EXISTS wms_location (
    id BIGINT NOT NULL AUTO_INCREMENT,
    location_code VARCHAR(64) NOT NULL,
    warehouse VARCHAR(32),
    area_code VARCHAR(32),
    location_group VARCHAR(32),
    row_no VARCHAR(16),
    column_no VARCHAR(16),
    level_no VARCHAR(16),
    location_type VARCHAR(32),
    temperature_zone VARCHAR(32),
    status VARCHAR(32) DEFAULT 'EMPTY',
    capacity DECIMAL(18,4),
    used_capacity DECIMAL(18,4) DEFAULT 0,
    max_weight DECIMAL(18,4),
    sort_no INT,
    coord_x INT,
    coord_y INT,
    coord_z INT,
    location_attrs TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    deleted TINYINT(1) DEFAULT 0,
    UNIQUE KEY uk_location_code (location_code),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 批次追踪事件表（事件溯源）
CREATE TABLE IF NOT EXISTS wms_batch_trace (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_no VARCHAR(64) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    warehouse VARCHAR(32),
    event_type VARCHAR(32) NOT NULL,
    event_source VARCHAR(64),
    qty_before DECIMAL(18,4),
    qty_change DECIMAL(18,4),
    qty_after DECIMAL(18,4),
    order_no VARCHAR(64),
    location_code VARCHAR(64),
    operator VARCHAR(64),
    event_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_trace_batch ON wms_batch_trace(batch_no, event_time);
