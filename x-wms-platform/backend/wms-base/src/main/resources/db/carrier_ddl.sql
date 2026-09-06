-- ============================================================
-- X WMS 承运商管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 承运商档案/承运商服务/承运商价格/承运商账户
-- ============================================================

-- 1. 承运商档案表
CREATE TABLE wms_carrier (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    carrier_code    VARCHAR(64)  NOT NULL, -- 承运商编码
    carrier_name    VARCHAR(128) NOT NULL, -- 承运商名称
    carrier_type    VARCHAR(32), -- EXPRESS快递/LOGISTICS物流/SELF自有
    contact_person  VARCHAR(64), -- 联系人
    contact_phone   VARCHAR(64), -- 联系电话
    contact_email   VARCHAR(128), -- 联系邮箱
    address         VARCHAR(512), -- 地址
    api_url         VARCHAR(256), -- API地址
    api_key         VARCHAR(256), -- API密钥(加密存储)
    api_secret      VARCHAR(256), -- API密钥(加密存储)
    customer_code   VARCHAR(64), -- 客户编码
    status          VARCHAR(32) DEFAULT 'ACTIVE', -- ACTIVE/INACTIVE
    priority        SMALLINT     DEFAULT 5, -- 优先级
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_carrier PRIMARY KEY (id),
    CONSTRAINT uk_wms_carrier_code UNIQUE (carrier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_carrier_status ON wms_carrier(status);
CREATE INDEX idx_wms_carrier_type ON wms_carrier(carrier_type);

-- 2. 承运商服务表
CREATE TABLE wms_carrier_service (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    carrier_code    VARCHAR(64)  NOT NULL,
    service_code    VARCHAR(64)  NOT NULL, -- 服务编码
    service_name    VARCHAR(128) NOT NULL, -- 服务名称
    service_type    VARCHAR(32), -- STANDARD/EXPRESS/NEXT_DAY/SAME_DAY/ECONOMY
    estimated_days  SMALLINT, -- 预计时效(天)
    supported_regions VARCHAR(512), -- 支持区域
    weight_limit    INT(18,4), -- 重量限制
    volume_limit    INT(18,4), -- 体积限制
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_carrier_service PRIMARY KEY (id),
    CONSTRAINT uk_wms_carrier_service UNIQUE (carrier_code, service_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_carrier_svc_carrier ON wms_carrier_service(carrier_code);

-- 3. 承运商价格表
CREATE TABLE wms_carrier_price (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    carrier_code    VARCHAR(64)  NOT NULL,
    service_code    VARCHAR(64)  NOT NULL,
    region_code     VARCHAR(64), -- 区域编码
    start_weight    INT(18,4), -- 起始重量
    end_weight      INT(18,4), -- 结束重量
    first_weight    INT(18,4), -- 首重
    first_price     INT(18,4), -- 首重价格
    additional_weight INT(18,4), -- 续重单位
    additional_price INT(18,4), -- 续重价格
    base_fee        INT(18,4), -- 基础费用
    fuel_surcharge  INT(18,4), -- 燃油附加费(百分比)
    other_fee       INT(18,4), -- 其他费用
    effective_date  TIMESTAMP, -- 生效日期
    expire_date     TIMESTAMP, -- 失效日期
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_carrier_price PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_carrier_price_carrier ON wms_carrier_price(carrier_code);
CREATE INDEX idx_wms_carrier_price_service ON wms_carrier_price(service_code);

-- 4. 承运商账户表
CREATE TABLE wms_carrier_account (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    carrier_code    VARCHAR(64)  NOT NULL,
    account_no      VARCHAR(128) NOT NULL, -- 账户号/月结账号
    account_type    VARCHAR(32), -- MONTHLY月结/PREPAID预付/CASH现付
    balance         INT(18,4)  DEFAULT 0, -- 账户余额
    credit_limit    INT(18,4), -- 信用额度
    warning_balance INT(18,4), -- 预警余额
    status          VARCHAR(32) DEFAULT 'ACTIVE', -- ACTIVE/FROZEN/CLOSED
    last_recharge_time TIMESTAMP, -- 最后充值时间
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_carrier_account PRIMARY KEY (id),
    CONSTRAINT uk_wms_carrier_account UNIQUE (carrier_code, account_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_carrier_acc_carrier ON wms_carrier_account(carrier_code);


-- 注释
ALTER TABLE wms_carrier COMMENT='承运商档案表';
ALTER TABLE wms_carrier_service COMMENT='承运商服务表';
ALTER TABLE wms_carrier_price COMMENT='承运商价格表';
ALTER TABLE wms_carrier_account COMMENT='承运商账户表';
