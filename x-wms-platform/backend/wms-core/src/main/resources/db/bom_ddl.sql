-- ============================================================
-- X WMS BOM组件管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 产品BOM主表/BOM明细表/组件扫描收货表/组件扫描收货明细表
-- ============================================================

-- 1. 产品BOM主表
CREATE TABLE wms_product_bom (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    bom_code        VARCHAR(64)  NOT NULL, -- BOM编码
    bom_name        VARCHAR(128), -- BOM名称
    parent_sku_code VARCHAR(64)  NOT NULL, -- 父件商品编码
    parent_sku_name VARCHAR(256), -- 父件商品名称
    parent_spec     VARCHAR(256), -- 父件规格
    parent_unit     VARCHAR(32), -- 父件单位
    parent_qty      INT(18,4)  DEFAULT 1, -- 父件数量（一个BOM组合产出的父件数量）
    owner_code      VARCHAR(64), -- 货主编码
    warehouse_code  VARCHAR(64), -- 仓库编码
    bom_type        VARCHAR(32), -- BOM类型：ASSEMBLY组装/KIT套装/PROMOTION促销组合
    status          VARCHAR(32)  DEFAULT 'ENABLED', -- 状态：ENABLED启用/DISABLED禁用
    version         VARCHAR(32), -- 版本号
    is_default      VARCHAR(1)   DEFAULT 'N', -- 是否默认版本：Y/N
    effective_date  TIMESTAMP, -- 生效日期
    expiry_date     TIMESTAMP, -- 失效日期
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_product_bom PRIMARY KEY (id),
    CONSTRAINT uk_wms_bom_code UNIQUE (bom_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_bom_parent ON wms_product_bom(parent_sku_code);
CREATE INDEX idx_wms_bom_status ON wms_product_bom(status);
CREATE INDEX idx_wms_bom_owner ON wms_product_bom(owner_code);

-- 2. 产品BOM明细表
CREATE TABLE wms_product_bom_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    bom_code        VARCHAR(64)  NOT NULL, -- BOM编码
    line_no         INT    NOT NULL, -- 行号
    child_sku_code  VARCHAR(64)  NOT NULL, -- 子件商品编码
    child_sku_name  VARCHAR(256), -- 子件商品名称
    child_spec      VARCHAR(256), -- 子件规格
    child_unit      VARCHAR(32), -- 子件单位
    child_qty       INT(18,4)  DEFAULT 0, -- 子件数量（一个父件需要的子件数量）
    loss_rate       INT(10,4), -- 损耗率（百分比）
    is_key          VARCHAR(1)   DEFAULT 'N', -- 是否关键件：Y/N
    is_optional     VARCHAR(1)   DEFAULT 'N', -- 可选件：Y/N
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_bom_detail PRIMARY KEY (id),
    CONSTRAINT uk_wms_bom_detail UNIQUE (bom_code, line_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_bom_detail_bom ON wms_product_bom_detail(bom_code);
CREATE INDEX idx_wms_bom_detail_child ON wms_product_bom_detail(child_sku_code);

-- 3. 组件扫描收货表
CREATE TABLE wms_component_receipt (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    receipt_no      VARCHAR(64)  NOT NULL, -- 组件收货单号
    bom_code        VARCHAR(64), -- BOM编码
    parent_sku_code VARCHAR(64), -- 父件商品编码
    parent_sku_name VARCHAR(256), -- 父件商品名称
    asn_no          VARCHAR(64), -- ASN号
    inbound_no      VARCHAR(64), -- 入库单号
    owner_code      VARCHAR(64), -- 货主编码
    warehouse_code  VARCHAR(64), -- 仓库编码
    batch_no        VARCHAR(64), -- 批次号
    location_code   VARCHAR(64), -- 收货库位
    parent_qty      INT(18,4)  DEFAULT 0, -- 父件收货数量
    scanned_child_count INT DEFAULT 0, -- 已扫描子件种类数
    required_child_count INT DEFAULT 0, -- 需要子件种类数
    status          VARCHAR(32)  DEFAULT 'SCANNING', -- 状态：SCANNING扫描中/COMPLETED已完成/CANCELLED已取消
    bom_matched     VARCHAR(1)   DEFAULT 'N', -- 是否满足BOM组合：Y/N
    operator        VARCHAR(64), -- 操作人
    start_time      TIMESTAMP, -- 扫描开始时间
    finish_time     TIMESTAMP, -- 扫描完成时间
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_component_receipt PRIMARY KEY (id),
    CONSTRAINT uk_wms_component_receipt_no UNIQUE (receipt_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_comp_receipt_bom ON wms_component_receipt(bom_code);
CREATE INDEX idx_wms_comp_receipt_asn ON wms_component_receipt(asn_no);
CREATE INDEX idx_wms_comp_receipt_status ON wms_component_receipt(status);

-- 4. 组件扫描收货明细表
CREATE TABLE wms_component_receipt_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    receipt_no      VARCHAR(64)  NOT NULL, -- 组件收货单号
    bom_code        VARCHAR(64), -- BOM编码
    child_sku_code  VARCHAR(64), -- 子件商品编码
    child_sku_name  VARCHAR(256), -- 子件商品名称
    required_qty    INT(18,4)  DEFAULT 0, -- BOM要求数量
    scanned_qty     INT(18,4)  DEFAULT 0, -- 已扫描数量
    satisfied       VARCHAR(1)   DEFAULT 'N', -- 是否满足：Y/N
    is_optional     VARCHAR(1)   DEFAULT 'N', -- 是否可选件：Y/N
    scan_time       TIMESTAMP, -- 扫描时间
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_comp_receipt_detail PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_comp_detail_receipt ON wms_component_receipt_detail(receipt_no);
CREATE INDEX idx_wms_comp_detail_child ON wms_component_receipt_detail(child_sku_code);


-- 注释
ALTER TABLE wms_product_bom COMMENT='产品BOM主表';
ALTER TABLE wms_product_bom_detail COMMENT='产品BOM明细表';
ALTER TABLE wms_component_receipt COMMENT='组件扫描收货表';
ALTER TABLE wms_component_receipt_detail COMMENT='组件扫描收货明细表';
