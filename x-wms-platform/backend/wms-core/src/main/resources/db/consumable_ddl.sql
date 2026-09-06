-- ============================================================
-- X WMS 耗材管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 耗材主表/产品包装关联表/耗材扣减记录表
-- 业务场景: 产品入库再包装，消耗包装盒、填充物等包装耗材
-- ============================================================

-- 1. 耗材主表
CREATE TABLE wms_consumable (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    consumable_code VARCHAR(64)  NOT NULL, -- 耗材编码
    consumable_name VARCHAR(256) NOT NULL, -- 耗材名称
    consumable_type VARCHAR(32), -- 耗材类型：BOX/FILLER/TAPE/LABEL/BAG/OTHER
    spec            VARCHAR(256), -- 规格型号
    unit            VARCHAR(32), -- 单位
    length          INT(18,4), -- 尺寸-长(mm)
    width           INT(18,4), -- 尺寸-宽(mm)
    height          INT(18,4), -- 尺寸-高(mm)
    load_capacity   INT(18,4), -- 承重(kg)
    supplier_code   VARCHAR(64), -- 供应商编码
    supplier_name   VARCHAR(256), -- 供应商名称
    safety_stock    INT(18,4)  DEFAULT 0, -- 安全库存
    current_stock   INT(18,4)  DEFAULT 0, -- 当前库存
    unit_cost       INT(18,4), -- 库存单位成本
    owner_code      VARCHAR(64), -- 货主编码
    warehouse_code  VARCHAR(64), -- 仓库编码
    location_code   VARCHAR(64), -- 存放库位
    status          VARCHAR(32)  DEFAULT 'ENABLED', -- 状态：ENABLED/DISABLED
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_consumable PRIMARY KEY (id),
    CONSTRAINT uk_wms_consumable_code UNIQUE (consumable_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_consumable_type ON wms_consumable(consumable_type);
CREATE INDEX idx_wms_consumable_status ON wms_consumable(status);
CREATE INDEX idx_wms_consumable_warehouse ON wms_consumable(warehouse_code);

-- 2. 产品包装关联表
CREATE TABLE wms_product_packaging (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    packaging_code  VARCHAR(64)  NOT NULL, -- 包装代码
    packaging_name  VARCHAR(256), -- 包装名称
    sku_code        VARCHAR(64)  NOT NULL, -- 商品编码
    sku_name        VARCHAR(256), -- 商品名称
    owner_code      VARCHAR(64), -- 货主编码
    warehouse_code  VARCHAR(64), -- 仓库编码
    consumable_code VARCHAR(64), -- 耗材编码
    consumable_name VARCHAR(256), -- 耗材名称
    consumable_type VARCHAR(32), -- 耗材类型
    usage_per_unit  INT(18,4)  DEFAULT 0, -- 单位产品耗材用量
    packaging_level TINYINT(1)     DEFAULT 1, -- 包装层级：1=内包装/2=中包装/3=外包装
    enabled         VARCHAR(1)   DEFAULT 'Y', -- 是否启用
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_product_packaging PRIMARY KEY (id),
    CONSTRAINT uk_wms_packaging_code UNIQUE (packaging_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_packaging_sku ON wms_product_packaging(sku_code);
CREATE INDEX idx_wms_packaging_consumable ON wms_product_packaging(consumable_code);
CREATE INDEX idx_wms_packaging_level ON wms_product_packaging(packaging_level);

-- 3. 耗材扣减记录表
CREATE TABLE wms_consumable_record (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    record_no       VARCHAR(64)  NOT NULL, -- 记录单号
    business_type   VARCHAR(32), -- 业务类型：INBOUND/OUTBOUND/TRANSFER/ADJUST
    ref_no          VARCHAR(64), -- 关联单据号
    ref_detail_no   VARCHAR(64), -- 关联单据明细号
    asn_no          VARCHAR(64), -- ASN号
    sku_code        VARCHAR(64), -- 商品编码
    sku_name        VARCHAR(256), -- 商品名称
    consumable_code VARCHAR(64), -- 耗材编码
    consumable_name VARCHAR(256), -- 耗材名称
    consumable_type VARCHAR(32), -- 耗材类型
    packaging_code  VARCHAR(64), -- 包装代码
    product_qty     INT(18,4)  DEFAULT 0, -- 产品数量
    usage_per_unit  INT(18,4)  DEFAULT 0, -- 单位用量
    deduct_qty      INT(18,4)  DEFAULT 0, -- 扣减数量
    before_stock    INT(18,4)  DEFAULT 0, -- 扣减前库存
    after_stock     INT(18,4)  DEFAULT 0, -- 扣减后库存
    owner_code      VARCHAR(64), -- 货主编码
    warehouse_code  VARCHAR(64), -- 仓库编码
    operator        VARCHAR(64), -- 操作人
    operate_time    TIMESTAMP, -- 操作时间
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_consumable_record PRIMARY KEY (id),
    CONSTRAINT uk_wms_consumable_record_no UNIQUE (record_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_consumable_record_ref ON wms_consumable_record(ref_no);
CREATE INDEX idx_wms_consumable_record_business ON wms_consumable_record(business_type);
CREATE INDEX idx_wms_consumable_record_consumable ON wms_consumable_record(consumable_code);


-- 注释
ALTER TABLE wms_consumable COMMENT='耗材主表（包装盒/填充物/胶带/标签等）';
ALTER TABLE wms_product_packaging COMMENT='产品包装关联表';
ALTER TABLE wms_consumable_record COMMENT='耗材扣减记录表';
