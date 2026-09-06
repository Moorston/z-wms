-- ============================================================
-- X WMS 客户/货主档案管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 货主档案/客户档案/供应商档案/联系人档案
-- ============================================================

-- 1. 货主档案表
CREATE TABLE wms_owner (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    owner_code      VARCHAR(64)  NOT NULL,
    owner_name      VARCHAR(128) NOT NULL,
    owner_type      VARCHAR(32)  NOT NULL, -- ENTERPRISE企业/INDIVIDUAL个体/PLATFORM平台
    short_name      VARCHAR(64),
    legal_person    VARCHAR(64),
    business_license VARCHAR(128),
    tax_number      VARCHAR(64),
    contact         VARCHAR(64),
    phone           VARCHAR(32),
    email           VARCHAR(128),
    address         VARCHAR(512),
    country         VARCHAR(32),
    province        VARCHAR(32),
    city            VARCHAR(32),
    district        VARCHAR(32),
    postcode        VARCHAR(16),
    settlement_type VARCHAR(32), -- MONTHLY月结/WEEKLY周结/DAILY日结/PREPAY预付
    credit_limit    INT(18,2),
    credit_used     INT(18,2) DEFAULT 0,
    status          VARCHAR(32) DEFAULT 'ACTIVE', -- ACTIVE/DISABLED/FROZEN
    owner_attrs     TEXT, -- 货主扩展属性(JSON)
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_owner PRIMARY KEY (id),
    CONSTRAINT uk_wms_owner_code UNIQUE (owner_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_owner_type ON wms_owner(owner_type);
CREATE INDEX idx_wms_owner_status ON wms_owner(status);

-- 2. 客户档案表
CREATE TABLE wms_customer (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    customer_code   VARCHAR(64)  NOT NULL,
    customer_name   VARCHAR(128) NOT NULL,
    customer_type   VARCHAR(32), -- B2B企业/B2C个人/CHANNEL渠道/DISTRIBUTOR经销商
    owner_code_col  VARCHAR(64), -- 所属货主
    short_name      VARCHAR(64),
    contact         VARCHAR(64),
    phone           VARCHAR(32),
    email           VARCHAR(128),
    address         VARCHAR(512),
    country         VARCHAR(32),
    province        VARCHAR(32),
    city            VARCHAR(32),
    district        VARCHAR(32),
    postcode        VARCHAR(16),
    delivery_method VARCHAR(32), -- EXPRESS快递/SELF自提/STATION站点
    default_warehouse VARCHAR(64),
    price_level     VARCHAR(32), -- VIP/GOLD/SILVER/NORMAL
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    customer_attrs  TEXT,
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_customer PRIMARY KEY (id),
    CONSTRAINT uk_wms_customer_code UNIQUE (customer_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_customer_owner ON wms_customer(owner_code_col);
CREATE INDEX idx_wms_customer_type ON wms_customer(customer_type);

-- 3. 供应商档案表
CREATE TABLE wms_supplier (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    supplier_code   VARCHAR(64)  NOT NULL,
    supplier_name   VARCHAR(128) NOT NULL,
    supplier_type   VARCHAR(32), -- MANUFACTURER厂商/DISTRIBUTOR经销商/AGENT代理
    owner_code_col  VARCHAR(64),
    short_name      VARCHAR(64),
    contact         VARCHAR(64),
    phone           VARCHAR(32),
    email           VARCHAR(128),
    address         VARCHAR(512),
    country         VARCHAR(32),
    province        VARCHAR(32),
    city            VARCHAR(32),
    district        VARCHAR(32),
    postcode        VARCHAR(16),
    settlement_type VARCHAR(32),
    payment_term    VARCHAR(32), -- NET30/NET60/CASH
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    supplier_attrs  TEXT,
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_supplier PRIMARY KEY (id),
    CONSTRAINT uk_wms_supplier_code UNIQUE (supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_supplier_owner ON wms_supplier(owner_code_col);
CREATE INDEX idx_wms_supplier_type ON wms_supplier(supplier_type);

-- 4. 联系人档案表
CREATE TABLE wms_contact (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    contact_code    VARCHAR(64)  NOT NULL,
    contact_name    VARCHAR(64)  NOT NULL,
    partner_type    VARCHAR(32)  NOT NULL, -- OWNER/CUSTOMER/SUPPLIER
    partner_code    VARCHAR(64)  NOT NULL,
    position        VARCHAR(64),
    department      VARCHAR(64),
    phone           VARCHAR(32),
    mobile          VARCHAR(32),
    email           VARCHAR(128),
    wechat          VARCHAR(64),
    is_primary      TINYINT(1)     DEFAULT 0,
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_contact PRIMARY KEY (id),
    CONSTRAINT uk_wms_contact_code UNIQUE (contact_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_contact_partner ON wms_contact(partner_type, partner_code);


-- 注释
ALTER TABLE wms_owner COMMENT='货主档案表';
ALTER TABLE wms_customer COMMENT='客户档案表';
ALTER TABLE wms_supplier COMMENT='供应商档案表';
ALTER TABLE wms_contact COMMENT='联系人档案表';
