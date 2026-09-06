-- ============================================================
-- X WMS P2模块DDL（耗材管理/可视化收货/门店收货/码盘预算）
-- 数据库: MySQL 8.x
-- ============================================================

-- 1. 可视化收货 - 产品图片表
CREATE TABLE wms_product_image (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    image_code      VARCHAR(64)  NOT NULL,
    sku_code        VARCHAR(64),
    sku_name        VARCHAR(256),
    image_url       VARCHAR(512),
    image_type      VARCHAR(32), -- MAIN主图/DETAIL细节图/PACKAGE包装图
    sort_order      INT    DEFAULT 0,
    image_size      VARCHAR(64),
    enabled         VARCHAR(1)   DEFAULT 'Y',
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_product_image PRIMARY KEY (id),
    CONSTRAINT uk_wms_image_code UNIQUE (image_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_image_sku ON wms_product_image(sku_code);

-- 2. 门店收货记录表
CREATE TABLE wms_store_receipt (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    receipt_no      VARCHAR(64)  NOT NULL,
    store_code      VARCHAR(64),
    store_name      VARCHAR(256),
    outbound_no     VARCHAR(64),
    wave_no         VARCHAR(64),
    carrier_code    VARCHAR(64),
    tracking_no     VARCHAR(128),
    expected_qty    INT(18,4)  DEFAULT 0,
    received_qty    INT(18,4)  DEFAULT 0,
    difference_qty  INT(18,4)  DEFAULT 0,
    status          VARCHAR(32)  DEFAULT 'PENDING', -- PENDING/RECEIVED/EXCEPTION
    receiver        VARCHAR(64),
    receive_time    TIMESTAMP,
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_store_receipt PRIMARY KEY (id),
    CONSTRAINT uk_wms_store_receipt_no UNIQUE (receipt_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_store_receipt_store ON wms_store_receipt(store_code);
CREATE INDEX idx_wms_store_receipt_outbound ON wms_store_receipt(outbound_no);

-- 3. 码盘预算表
CREATE TABLE wms_pallet_budget (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    budget_no       VARCHAR(64)  NOT NULL,
    asn_no          VARCHAR(64),
    inbound_no      VARCHAR(64),
    sku_code        VARCHAR(64),
    sku_name        VARCHAR(256),
    total_qty       INT(18,4)  DEFAULT 0,
    box_qty         INT    DEFAULT 0,
    pallet_qty      INT    DEFAULT 0,
    location_code   VARCHAR(64),
    location_height INT(18,4), -- 库位高度(mm)
    layers_per_pallet INT DEFAULT 0, -- 每托盘层数
    boxes_per_layer INT   DEFAULT 0, -- 每层箱数
    box_height      INT(18,4), -- 箱高(mm)
    box_weight      INT(18,4), -- 箱重(kg)
    status          VARCHAR(32)  DEFAULT 'PENDING', -- PENDING/BUDGETED/EXECUTED
    operator        VARCHAR(64),
    budget_time     TIMESTAMP,
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_pallet_budget PRIMARY KEY (id),
    CONSTRAINT uk_wms_pallet_budget_no UNIQUE (budget_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_pallet_budget_asn ON wms_pallet_budget(asn_no);
CREATE INDEX idx_wms_pallet_budget_sku ON wms_pallet_budget(sku_code);


-- 注释
ALTER TABLE wms_product_image COMMENT='产品图片表（可视化收货）';
ALTER TABLE wms_store_receipt COMMENT='门店收货记录表';
ALTER TABLE wms_pallet_budget COMMENT='码盘预算表（复杂组盘/库位高度计算）';
