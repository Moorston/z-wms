-- ============================================================
-- 序列号管理模块DDL
-- 支持1级（单品级唯一码）和2级（箱号+单品序列号）管理
-- ============================================================

-- 1. 序列号主表
CREATE TABLE wms_serial_number (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    serial_no       VARCHAR(128) NOT NULL, -- 序列号（唯一）
    serial_level    TINYINT(1)     DEFAULT 1, -- 序列号级别：1=单品级/2=箱级
    parent_serial_no VARCHAR(128), -- 父序列号（2级时关联箱号）
    sku_code        VARCHAR(64), -- 商品编码
    sku_name        VARCHAR(256), -- 商品名称
    barcode         VARCHAR(64), -- 商品条码
    owner_code      VARCHAR(64), -- 货主编码
    warehouse_code  VARCHAR(64), -- 仓库编码
    batch_no        VARCHAR(64), -- 批次号
    inbound_no      VARCHAR(64), -- 入库单号
    asn_no          VARCHAR(64), -- ASN号
    outbound_no     VARCHAR(64), -- 出库单号
    wave_no         VARCHAR(64), -- 波次号
    location_code   VARCHAR(64), -- 当前库位
    status          VARCHAR(32)  DEFAULT 'IN_STOCK', -- 状态：IN_STOCK/OUT_STOCK/RETURNED/SCRAPPED/FROZEN
    rule_code       VARCHAR(64), -- 序列号规则编码
    production_date TIMESTAMP, -- 生产日期
    expiry_date     TIMESTAMP, -- 失效日期
    inbound_time    TIMESTAMP, -- 入库时间
    outbound_time   TIMESTAMP, -- 出库时间
    supplier_code   VARCHAR(64), -- 供应商编码
    customer_code   VARCHAR(64), -- 客户编码
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_serial_number PRIMARY KEY (id),
    CONSTRAINT uk_wms_serial_no UNIQUE (serial_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_serial_sku ON wms_serial_number(sku_code);
CREATE INDEX idx_wms_serial_owner ON wms_serial_number(owner_code);
CREATE INDEX idx_wms_serial_warehouse ON wms_serial_number(warehouse_code);
CREATE INDEX idx_wms_serial_status ON wms_serial_number(status);
CREATE INDEX idx_wms_serial_inbound ON wms_serial_number(inbound_no);
CREATE INDEX idx_wms_serial_outbound ON wms_serial_number(outbound_no);
CREATE INDEX idx_wms_serial_parent ON wms_serial_number(parent_serial_no);
CREATE INDEX idx_wms_serial_batch ON wms_serial_number(batch_no);

-- 2. 序列号规则表
CREATE TABLE wms_serial_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL, -- 规则编码（唯一）
    rule_name       VARCHAR(128), -- 规则名称
    sku_code        VARCHAR(64), -- 商品编码（为空表示通用规则）
    owner_code      VARCHAR(64), -- 货主编码（为空表示通用规则）
    serial_level    TINYINT(1)     DEFAULT 1, -- 序列号级别：1=单品级/2=箱级
    length          SMALLINT     DEFAULT 0, -- 序列号长度（0表示不限制）
    min_length      SMALLINT, -- 最小长度
    max_length      SMALLINT, -- 最大长度
    prefix          VARCHAR(256), -- 前缀（多个用逗号分隔）
    suffix          VARCHAR(256), -- 后缀（多个用逗号分隔）
    fixed_chars     VARCHAR(512), -- 中间固定字符（位置:字符，多个用逗号分隔）
    char_type       VARCHAR(32)  DEFAULT 'ANY', -- 字符类型：ALPHA/NUMERIC/ALPHANUMERIC/ANY
    validate_expression VARCHAR(1024), -- 合法性校验布尔表达式
    medical_force_length VARCHAR(1), -- 医药行业强制长度（16或20位）
    enabled         VARCHAR(1)   DEFAULT 'Y', -- 是否启用：Y/N
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_serial_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_serial_rule_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_serial_rule_sku ON wms_serial_rule(sku_code);
CREATE INDEX idx_wms_serial_rule_owner ON wms_serial_rule(owner_code);

-- 3. 序列号采集记录表
CREATE TABLE wms_serial_record (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    record_no       VARCHAR(64)  NOT NULL, -- 采集单号
    business_type   VARCHAR(32), -- 业务类型：INBOUND/OUTBOUND/RETURN/TRANSFER/ADJUST
    ref_no          VARCHAR(64), -- 关联单据号
    ref_detail_no   VARCHAR(64), -- 关联单据明细号
    asn_no          VARCHAR(64), -- ASN号
    sku_code        VARCHAR(64), -- 商品编码
    sku_name        VARCHAR(256), -- 商品名称
    owner_code      VARCHAR(64), -- 货主编码
    warehouse_code  VARCHAR(64), -- 仓库编码
    batch_no        VARCHAR(64), -- 批次号
    serial_nos      TEXT, -- 序列号（多个用逗号分隔）
    collect_qty     INT    DEFAULT 0, -- 采集数量
    expected_qty    INT    DEFAULT 0, -- 应采集数量
    status          VARCHAR(32)  DEFAULT 'PENDING', -- 状态：PENDING/COLLECTING/COMPLETED/CANCELLED
    collect_mode    VARCHAR(32)  DEFAULT 'SCAN', -- 采集模式：SCAN/IMPORT/MANUAL
    collector       VARCHAR(64), -- 采集人
    start_time      TIMESTAMP, -- 采集开始时间
    finish_time     TIMESTAMP, -- 采集完成时间
    location_code   VARCHAR(64), -- 库位编码
    lpn_no          VARCHAR(64), -- 托盘号/LPN
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_serial_record PRIMARY KEY (id),
    CONSTRAINT uk_wms_serial_record_no UNIQUE (record_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_serial_record_ref ON wms_serial_record(ref_no);
CREATE INDEX idx_wms_serial_record_business ON wms_serial_record(business_type);
CREATE INDEX idx_wms_serial_record_sku ON wms_serial_record(sku_code);
CREATE INDEX idx_wms_serial_record_status ON wms_serial_record(status);


-- 注释
ALTER TABLE wms_serial_number COMMENT='序列号主表';
ALTER TABLE wms_serial_rule COMMENT='序列号规则表';
ALTER TABLE wms_serial_record COMMENT='序列号采集记录表';
