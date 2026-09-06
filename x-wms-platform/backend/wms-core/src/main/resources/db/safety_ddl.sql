-- ============================================================
-- X WMS 库存安全库存管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 安全库存规则/安全库存结果/补货点/安全库存预警
-- ============================================================

-- 1. 安全库存规则表
CREATE TABLE wms_safety_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL, -- 规则编码
    rule_name       VARCHAR(128) NOT NULL, -- 规则名称
    warehouse_code  VARCHAR(64), -- 适用仓库
    owner_code      VARCHAR(64), -- 适用货主
    sku_code        VARCHAR(64), -- 适用SKU(空表示所有)
    category_code   VARCHAR(64), -- 适用品类
    abc_class       VARCHAR(8), -- 适用ABC分类
    calc_method     VARCHAR(32)  NOT NULL, -- FIXED固定值/STATISTICAL统计计算/LEAD_TIME提前期法/SERVICE_LEVEL服务水平法
    fixed_safety    INT(18,4), -- 固定安全库存
    fixed_reorder   INT(18,4), -- 固定补货点
    fixed_max       INT(18,4), -- 固定最高库存
    service_level   INT(5,2), -- 服务水平(%)
    lead_time_days  SMALLINT, -- 提前期(天)
    review_period   SMALLINT, -- 盘点周期(天)
    z_score         INT(18,4), -- Z值(服务水平对应)
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    priority        SMALLINT     DEFAULT 5,
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_safety_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_safety_rule_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 安全库存结果表
CREATE TABLE wms_safety_stock (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    stock_id        VARCHAR(64)  NOT NULL, -- 结果ID
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    category_code   VARCHAR(64),
    abc_class       VARCHAR(8),
    safety_stock    INT(18,4)  NOT NULL, -- 安全库存
    reorder_point   INT(18,4)  NOT NULL, -- 补货点
    max_stock       INT(18,4), -- 最高库存
    avg_demand      INT(18,4), -- 平均日需求
    std_demand      INT(18,4), -- 需求标准差
    lead_time       INT(18,4), -- 提前期
    service_level   INT(5,2), -- 服务水平
    calc_method     VARCHAR(32), -- 计算方法
    current_stock   INT(18,4), -- 当前库存
    stock_status    VARCHAR(32), -- NORMAL正常/BELOW_SAFETY低于安全/BELOW_REORDER低于补货点/OUT_OF_STOCK缺货
    calc_time       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    status          VARCHAR(32) DEFAULT 'CURRENT', -- CURRENT/HISTORY
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_safety_stock PRIMARY KEY (id),
    CONSTRAINT uk_wms_safety_stock_id UNIQUE (stock_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_safety_warehouse ON wms_safety_stock(warehouse_code);
CREATE INDEX idx_wms_safety_sku ON wms_safety_stock(sku_code);
CREATE INDEX idx_wms_safety_status ON wms_safety_stock(stock_status);

-- 3. 补货点表（动态补货建议）
CREATE TABLE wms_reorder_suggestion (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    suggestion_no   VARCHAR(64)  NOT NULL, -- 建议单号
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    current_stock   INT(18,4)  NOT NULL, -- 当前库存
    reorder_point   INT(18,4)  NOT NULL, -- 补货点
    safety_stock    INT(18,4), -- 安全库存
    max_stock       INT(18,4), -- 最高库存
    suggest_qty     INT(18,4)  NOT NULL, -- 建议补货数量
    suggest_type    VARCHAR(32), -- URGENT紧急/NORMAL正常/OPTIMAL优化
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING待处理/CONVERTED已转单/IGNORED已忽略
    source          VARCHAR(32), -- 来源: AUTO自动/MANUAL手动
    ref_no          VARCHAR(64), -- 关联补货单号
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_reorder_sug PRIMARY KEY (id),
    CONSTRAINT uk_wms_reorder_sug_no UNIQUE (suggestion_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_reorder_warehouse ON wms_reorder_suggestion(warehouse_code);
CREATE INDEX idx_wms_reorder_sku ON wms_reorder_suggestion(sku_code);
CREATE INDEX idx_wms_reorder_status ON wms_reorder_suggestion(status);


-- 注释
ALTER TABLE wms_safety_rule COMMENT='安全库存规则表';
ALTER TABLE wms_safety_stock COMMENT='安全库存结果表';
ALTER TABLE wms_reorder_suggestion COMMENT='补货点表';
