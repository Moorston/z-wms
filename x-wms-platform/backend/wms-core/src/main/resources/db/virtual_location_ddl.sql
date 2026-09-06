-- ============================================================
-- X WMS 虚拟库位体系 DDL
-- 包含：库位表扩展（虚拟库位字段）、播种墙、播种格口、分拣差异
-- 适用数据库：MySQL 8.x
-- ============================================================

-- ========== 1. 库位表扩展：增加虚拟库位字段 ==========
ALTER TABLE wms_location ADD (
    is_virtual        VARCHAR(1) DEFAULT 'N',
    virtual_type      VARCHAR(32),
    sorting_wall_code VARCHAR(32),
    grid_no           VARCHAR(16),
    bound_order_no    VARCHAR(64),
    bound_wave_no     VARCHAR(64),
    virtual_capacity  INT(18,4) DEFAULT 0,
    auto_release      VARCHAR(1) DEFAULT 'Y',
    ref_biz_no        VARCHAR(64)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 虚拟库位索引
CREATE INDEX idx_loc_virtual ON wms_location(is_virtual, virtual_type);
CREATE INDEX idx_loc_bound ON wms_location(bound_order_no, bound_wave_no);

-- ========== 2. 播种墙表 ==========
CREATE TABLE wms_sorting_wall (
    id              INT PRIMARY KEY,
    wall_code       VARCHAR(32) NOT NULL UNIQUE,
    wall_name       VARCHAR(100),
    warehouse_code  VARCHAR(32) NOT NULL,
    area_code       VARCHAR(32),
    grid_count      INT DEFAULT 0,
    grid_rows       INT,
    grid_cols       INT,
    status          VARCHAR(16) DEFAULT 'IDLE',
    current_wave_no VARCHAR(64),
    ptl_device_id   VARCHAR(64),
    remark          VARCHAR(500),
    owner_code      VARCHAR(64),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(64),
    updated_by      VARCHAR(64)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_sorting_wall_wh ON wms_sorting_wall(warehouse_code, status);

-- ========== 3. 播种墙格口表 ==========
CREATE TABLE wms_sorting_grid (
    id              INT PRIMARY KEY,
    wall_code       VARCHAR(32) NOT NULL,
    grid_no         VARCHAR(16) NOT NULL,
    location_code   VARCHAR(32),
    bound_order_no  VARCHAR(64),
    bound_wave_no   VARCHAR(64),
    status          VARCHAR(16) DEFAULT 'EMPTY',
    expected_qty    INT(18,4),
    actual_qty      INT(18,4) DEFAULT 0,
    row_index       INT,
    col_index       INT,
    ptl_address     VARCHAR(32),
    owner_code      VARCHAR(64),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(64),
    updated_by      VARCHAR(64),
    CONSTRAINT uk_sorting_grid UNIQUE (wall_code, grid_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_sorting_grid_wall ON wms_sorting_grid(wall_code, status);
CREATE INDEX idx_sorting_grid_order ON wms_sorting_grid(bound_order_no);

-- ========== 4. 分拣差异表 ==========
CREATE TABLE wms_sorting_difference (
    id              INT PRIMARY KEY,
    diff_no         VARCHAR(32) NOT NULL UNIQUE,
    wave_no         VARCHAR(64),
    order_no        VARCHAR(64),
    sku             VARCHAR(64),
    batch_no        VARCHAR(64),
    difference_type VARCHAR(16),
    difference_qty  INT(18,4),
    status          VARCHAR(16) DEFAULT 'PENDING',
    handle_method   VARCHAR(16),
    reason          VARCHAR(500),
    handler         VARCHAR(64),
    handled_at      TIMESTAMP,
    diff_location_code VARCHAR(32),
    owner_code      VARCHAR(64),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(64),
    updated_by      VARCHAR(64)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_sorting_diff_wave ON wms_sorting_difference(wave_no, status);
CREATE INDEX idx_sorting_diff_sku ON wms_sorting_difference(sku);

-- ========== 5. 库存表扩展：增加库存状态字段 ==========
ALTER TABLE wms_inventory ADD (
    inventory_status VARCHAR(16) DEFAULT 'AVAILABLE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_inv_status ON wms_inventory(inventory_status);

-- ========== 6. 注释说明 ==========

-- ========== 7. 初始化虚拟库区 ==========
INSERT INTO wms_area (id, area_code, area_name, warehouse_code, area_type, status, sort_no)
VALUES (seq_area.NEXTVAL, 'VIRTUAL', '虚拟库区', 'WH001', 'VIRTUAL', 'ACTIVE', 999);
INSERT INTO wms_area (id, area_code, area_name, warehouse_code, area_type, status, sort_no)
VALUES (seq_area.NEXTVAL, 'SORTING_AREA', '分拣区', 'WH001', 'SORTING', 'ACTIVE', 50);
INSERT INTO wms_area (id, area_code, area_name, warehouse_code, area_type, status, sort_no)
VALUES (seq_area.NEXTVAL, 'DIFFERENCE_AREA', '差异区', 'WH001', 'DIFFERENCE', 'ACTIVE', 99);
