-- ============================================================
-- X WMS 产品档案管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 产品分类/产品档案/产品包装/产品条码
-- ============================================================

-- 1. 产品分类表
CREATE TABLE wms_product_category (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    category_code   VARCHAR(64)  NOT NULL,
    category_name   VARCHAR(128) NOT NULL,
    parent_code     VARCHAR(64),
    category_level  TINYINT(1)     DEFAULT 1,
    sort_order      SMALLINT     DEFAULT 0,
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    owner_code_col  VARCHAR(64),
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_product_cat PRIMARY KEY (id),
    CONSTRAINT uk_wms_product_cat_code UNIQUE (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_product_cat_parent ON wms_product_category(parent_code);

-- 2. 产品档案表
CREATE TABLE wms_product (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256) NOT NULL,
    sku_short_name  VARCHAR(128),
    category_code   VARCHAR(64),
    owner_code_col  VARCHAR(64),
    brand           VARCHAR(128),
    spec            VARCHAR(256), -- 规格
    model           VARCHAR(128), -- 型号
    unit            VARCHAR(32)  NOT NULL, -- 基本单位
    secondary_unit  VARCHAR(32), -- 辅助单位
    convert_rate    INT(18,4), -- 换算率
    weight          INT(18,4), -- 重量(kg)
    volume          INT(18,4), -- 体积(m3)
    length          INT(18,4), -- 长(cm)
    width           INT(18,4), -- 宽(cm)
    height          INT(18,4), -- 高(cm)
    price           INT(18,4), -- 标准价
    cost            INT(18,4), -- 成本价
    shelf_life      SMALLINT, -- 保质期(天)
    shelf_life_unit VARCHAR(16), -- DAY/MONTH/YEAR
    storage_temp    VARCHAR(32), -- 存储温度要求
    is_batch_mgmt   TINYINT(1)     DEFAULT 0, -- 是否批次管理
    is_serial_mgmt  TINYINT(1)     DEFAULT 0, -- 是否序列号管理
    is_fragile      TINYINT(1)     DEFAULT 0, -- 是否易碎
    is_hazardous    TINYINT(1)     DEFAULT 0, -- 是否危险品
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    product_attrs   TEXT, -- 产品扩展属性(JSON)
    image_url       VARCHAR(512),
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_product PRIMARY KEY (id),
    CONSTRAINT uk_wms_product_sku UNIQUE (sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_product_cat ON wms_product(category_code);
CREATE INDEX idx_wms_product_owner ON wms_product(owner_code_col);
CREATE INDEX idx_wms_product_status ON wms_product(status);

-- 3. 产品包装表
CREATE TABLE wms_product_package (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    package_code    VARCHAR(64)  NOT NULL,
    package_name    VARCHAR(128) NOT NULL,
    sku_code        VARCHAR(64)  NOT NULL,
    package_type    VARCHAR(32)  NOT NULL, -- BOX箱/PALLET托盘/CARTON纸箱/BAG袋
    quantity        INT(18,4) NOT NULL, -- 包装内数量
    weight          INT(18,4), -- 包装重量
    volume          INT(18,4), -- 包装体积
    length          INT(18,4),
    width           INT(18,4),
    height          INT(18,4),
    max_stack       SMALLINT, -- 最大堆码层数
    is_default      TINYINT(1)     DEFAULT 0, -- 是否默认包装
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_product_pkg PRIMARY KEY (id),
    CONSTRAINT uk_wms_product_pkg_code UNIQUE (package_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_product_pkg_sku ON wms_product_package(sku_code);

-- 4. 产品条码表
CREATE TABLE wms_product_barcode (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    barcode         VARCHAR(128) NOT NULL,
    sku_code        VARCHAR(64)  NOT NULL,
    package_code    VARCHAR(64),
    barcode_type    VARCHAR(32)  NOT NULL, -- EAN13/UPC/CODE128/QR/INTERNAL
    quantity        INT(18,4) DEFAULT 1, -- 条码对应数量
    is_primary      TINYINT(1)     DEFAULT 0, -- 是否主条码
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_product_barcode PRIMARY KEY (id),
    CONSTRAINT uk_wms_product_barcode UNIQUE (barcode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_product_barcode_sku ON wms_product_barcode(sku_code);


-- 注释
ALTER TABLE wms_product_category COMMENT='产品分类表';
ALTER TABLE wms_product COMMENT='产品档案表';
ALTER TABLE wms_product_package COMMENT='产品包装表';
ALTER TABLE wms_product_barcode COMMENT='产品条码表';
