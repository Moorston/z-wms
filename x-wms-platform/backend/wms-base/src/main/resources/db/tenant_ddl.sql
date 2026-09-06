-- ============================================================
-- X WMS 多租户管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 租户/租户配置/租户资源配额/租户套餐
-- ============================================================

-- 1. 租户表
CREATE TABLE wms_tenant (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    tenant_code     VARCHAR(64)  NOT NULL,
    tenant_name     VARCHAR(128) NOT NULL,
    tenant_type     VARCHAR(32)  DEFAULT 'OWNER', -- OWNER货主/WAREHOUSE仓库/PLATFORM平台
    contact_person  VARCHAR(64),
    contact_phone   VARCHAR(32),
    contact_email   VARCHAR(128),
    address         VARCHAR(512),
    industry        VARCHAR(64),  -- 行业: 医药/食品/电商/制造业等
    status          VARCHAR(16)  DEFAULT 'ACTIVE', -- ACTIVE/INACTIVE/FROZEN/EXPIRED
    expire_date     DATE,
    logo_url        VARCHAR(512),
    description     VARCHAR(1024),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_tenant PRIMARY KEY (id),
    CONSTRAINT uk_wms_tenant_code UNIQUE (tenant_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_tenant_type ON wms_tenant(tenant_type);
CREATE INDEX idx_wms_tenant_status ON wms_tenant(status);

-- 2. 租户配置表
CREATE TABLE wms_tenant_config (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    tenant_code     VARCHAR(64)  NOT NULL,
    config_key      VARCHAR(128) NOT NULL,
    config_value    VARCHAR(2048),
    config_type     VARCHAR(32),  -- STRING/INT/BOOLEAN/JSON
    description     VARCHAR(512),
    is_system       TINYINT(1)     DEFAULT 0, -- 是否系统配置(不可删除)
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_tenant_config PRIMARY KEY (id),
    CONSTRAINT uk_wms_tenant_cfg UNIQUE (tenant_code, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_tenant_cfg_tenant ON wms_tenant_config(tenant_code);

-- 3. 租户资源配额表
CREATE TABLE wms_tenant_quota (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    tenant_code     VARCHAR(64)  NOT NULL,
    resource_type   VARCHAR(64)  NOT NULL, -- USER用户/WAREHOUSE仓库/LOCATION库位/SKU商品/ORDER订单/STORAGE存储
    quota_limit     BIGINT    DEFAULT 0, -- 配额上限, 0表示不限
    quota_used      BIGINT    DEFAULT 0, -- 已使用
    quota_unit      VARCHAR(16),  -- 个/GB/次/天
    warning_threshold SMALLINT   DEFAULT 80, -- 告警阈值(%)
    status          VARCHAR(16)  DEFAULT 'NORMAL', -- NORMAL/WARNING/OVER_LIMIT
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_tenant_quota PRIMARY KEY (id),
    CONSTRAINT uk_wms_tenant_quota UNIQUE (tenant_code, resource_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_tenant_quota_tenant ON wms_tenant_quota(tenant_code);

-- 4. 租户套餐表
CREATE TABLE wms_tenant_package (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    package_code    VARCHAR(64)  NOT NULL,
    package_name    VARCHAR(128) NOT NULL,
    package_level   VARCHAR(16),  -- BASIC基础/STANDARD标准/PRO专业/ENTERPRISE企业
    price           INT(10,2),
    price_unit      VARCHAR(16),  -- MONTH月/YEAR年
    description     VARCHAR(1024),
    features        TEXT,          -- 功能特性(JSON)
    quotas          TEXT,          -- 资源配额(JSON)
    enabled         TINYINT(1)     DEFAULT 1,
    sort_order      SMALLINT     DEFAULT 0,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_tenant_pkg PRIMARY KEY (id),
    CONSTRAINT uk_wms_tenant_pkg_code UNIQUE (package_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 注释
ALTER TABLE wms_tenant COMMENT='租户表';
ALTER TABLE wms_tenant_config COMMENT='租户配置表';
ALTER TABLE wms_tenant_quota COMMENT='租户资源配额表';
ALTER TABLE wms_tenant_package COMMENT='租户套餐表';
