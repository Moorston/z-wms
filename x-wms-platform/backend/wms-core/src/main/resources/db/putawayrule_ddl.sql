-- ============================================================
-- X WMS 上架规则管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 上架规则/上架策略/上架日志/上架详情
-- ============================================================

-- 1. 上架规则表
CREATE TABLE wms_putaway_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL,
    rule_name       VARCHAR(128) NOT NULL,
    rule_type       VARCHAR(32)  NOT NULL,
    sku_code        VARCHAR(64),
    category_code   VARCHAR(64),
    owner_code      VARCHAR(64),
    warehouse_code  VARCHAR(64),
    strategy        VARCHAR(32)  NOT NULL,
    target_area_code VARCHAR(64),
    target_location_group VARCHAR(64),
    preferred_area  VARCHAR(64), -- 优先库区(ZONE策略用)
    product_weight  INT(18,4), -- 商品重量(kg)(HEIGHT/WEIGHT策略用)
    allow_mix       VARCHAR(1)   DEFAULT 'N',
    allow_batch_mix VARCHAR(1)   DEFAULT 'N',
    min_capacity_utilization SMALLINT,
    max_capacity_utilization SMALLINT,
    priority        SMALLINT     DEFAULT 5,
    status          VARCHAR(32)  DEFAULT 'ACTIVE',
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_putaway_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_putaway_rule_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_par_sku ON wms_putaway_rule(sku_code);
CREATE INDEX idx_wms_par_owner ON wms_putaway_rule(owner_code);
CREATE INDEX idx_wms_par_warehouse ON wms_putaway_rule(warehouse_code);
CREATE INDEX idx_wms_par_status ON wms_putaway_rule(status);

-- 2. 上架策略表
CREATE TABLE wms_putaway_strategy (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    strategy_code   VARCHAR(64)  NOT NULL,
    strategy_name   VARCHAR(128) NOT NULL,
    strategy_type   VARCHAR(32)  NOT NULL,
    rule_code       VARCHAR(64),
    sort_field      VARCHAR(32),
    sort_direction  VARCHAR(8),
    filter_condition TEXT,
    max_try_locations SMALLINT,
    enable_dynamic_location VARCHAR(1) DEFAULT 'N',
    enable_recommend_location VARCHAR(1) DEFAULT 'Y',
    status          VARCHAR(32)  DEFAULT 'ACTIVE',
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_putaway_strategy PRIMARY KEY (id),
    CONSTRAINT uk_wms_putaway_strategy_code UNIQUE (strategy_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_pas_rule ON wms_putaway_strategy(rule_code);
CREATE INDEX idx_wms_pas_type ON wms_putaway_strategy(strategy_type);

-- 3. 上架执行日志表
CREATE TABLE wms_putaway_log (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    log_no          VARCHAR(64)  NOT NULL,
    inbound_no      VARCHAR(64),
    asn_no          VARCHAR(64),
    sku_code        VARCHAR(64)  NOT NULL,
    batch_no        VARCHAR(128),
    owner_code      VARCHAR(64),
    warehouse_code  VARCHAR(64)  NOT NULL,
    quantity        INT(18,4)  NOT NULL,
    source_location VARCHAR(64),
    target_location VARCHAR(64),
    rule_code       VARCHAR(64),
    strategy_code   VARCHAR(64),
    status          VARCHAR(32)  DEFAULT 'SUCCESS',
    error_message   VARCHAR(1024),
    try_count       SMALLINT     DEFAULT 0,
    duration_ms     BIGINT,
    operator        VARCHAR(64),
    operate_time    TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_putaway_log PRIMARY KEY (id),
    CONSTRAINT uk_wms_putaway_log_no UNIQUE (log_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_pal_inbound ON wms_putaway_log(inbound_no);
CREATE INDEX idx_wms_pal_sku ON wms_putaway_log(sku_code);
CREATE INDEX idx_wms_pal_warehouse ON wms_putaway_log(warehouse_code);
CREATE INDEX idx_wms_pal_status ON wms_putaway_log(status);
CREATE INDEX idx_wms_pal_operate_time ON wms_putaway_log(operate_time);

-- 4. 上架执行详情表
CREATE TABLE wms_putaway_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    detail_no       VARCHAR(64)  NOT NULL,
    log_no          VARCHAR(64),
    inbound_no      VARCHAR(64),
    sku_code        VARCHAR(64)  NOT NULL,
    batch_no        VARCHAR(128),
    owner_code      VARCHAR(64),
    warehouse_code  VARCHAR(64)  NOT NULL,
    source_location VARCHAR(64),
    target_location VARCHAR(64),
    target_area     VARCHAR(64),
    quantity        INT(18,4)  NOT NULL,
    original_qty    INT(18,4),
    current_qty     INT(18,4),
    capacity_utilization SMALLINT,
    is_recommend    VARCHAR(1)   DEFAULT 'N',
    sort_order      SMALLINT,
    status          VARCHAR(32)  DEFAULT 'PENDING',
    operator        VARCHAR(64),
    operate_time    TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_putaway_detail PRIMARY KEY (id),
    CONSTRAINT uk_wms_putaway_detail_no UNIQUE (detail_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_pad_log ON wms_putaway_detail(log_no);
CREATE INDEX idx_wms_pad_inbound ON wms_putaway_detail(inbound_no);
CREATE INDEX idx_wms_pad_sku ON wms_putaway_detail(sku_code);
CREATE INDEX idx_wms_pad_target_location ON wms_putaway_detail(target_location);


-- 注释
ALTER TABLE wms_putaway_rule COMMENT='上架规则表';
ALTER TABLE wms_putaway_strategy COMMENT='上架策略表';
ALTER TABLE wms_putaway_log COMMENT='上架执行日志表';
ALTER TABLE wms_putaway_detail COMMENT='上架执行详情表';
