-- ============================================================
-- X WMS 库存数据集市管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 数据集市/指标管理/维度管理/数据模型
-- ============================================================

-- 1. 数据集市表
CREATE TABLE wms_data_mart (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    mart_id         VARCHAR(64)  NOT NULL,
    mart_name       VARCHAR(128) NOT NULL,
    mart_code       VARCHAR(64)  NOT NULL,
    mart_type       VARCHAR(32)  NOT NULL,
    warehouse_code  VARCHAR(64),
    owner_code      VARCHAR(64),
    description     VARCHAR(512),
    data_source     TEXT,
    mart_config     TEXT,
    refresh_strategy VARCHAR(32) DEFAULT 'SCHEDULED',
    refresh_cron    VARCHAR(64),
    last_refresh_time TIMESTAMP,
    next_refresh_time TIMESTAMP,
    record_count    BIGINT,
    data_size_mb    INT(19,4),
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    is_active       VARCHAR(8) DEFAULT 'Y',
    sort_order      INT DEFAULT 100,
    created_by      VARCHAR(64),
    updated_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_data_mart PRIMARY KEY (id),
    CONSTRAINT uk_wms_data_mart_id UNIQUE (mart_id),
    CONSTRAINT uk_wms_data_mart_code UNIQUE (mart_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_data_mart_type ON wms_data_mart(mart_type);
CREATE INDEX idx_wms_data_mart_wh ON wms_data_mart(warehouse_code);
CREATE INDEX idx_wms_data_mart_status ON wms_data_mart(status);

-- 2. 指标管理表
CREATE TABLE wms_metric_define (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    metric_id       VARCHAR(64)  NOT NULL,
    metric_name     VARCHAR(128) NOT NULL,
    metric_code     VARCHAR(64)  NOT NULL,
    metric_type     VARCHAR(32)  NOT NULL,
    metric_category VARCHAR(32),
    mart_id         VARCHAR(64),
    description     VARCHAR(512),
    calculation_formula VARCHAR(1024),
    data_source     TEXT,
    unit            VARCHAR(32),
    precision       SMALLINT DEFAULT 2,
    aggregation_type VARCHAR(32) DEFAULT 'SUM',
    is_derived      VARCHAR(8) DEFAULT 'N',
    parent_metric_id VARCHAR(64),
    target_value    INT(19,4),
    benchmark_value INT(19,4),
    threshold_warning INT(19,4),
    threshold_critical INT(19,4),
    direction       VARCHAR(16) DEFAULT 'HIGHER',
    is_active       VARCHAR(8) DEFAULT 'Y',
    sort_order      INT DEFAULT 100,
    created_by      VARCHAR(64),
    updated_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_metric_define PRIMARY KEY (id),
    CONSTRAINT uk_wms_metric_define_id UNIQUE (metric_id),
    CONSTRAINT uk_wms_metric_define_code UNIQUE (metric_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_metric_define_type ON wms_metric_define(metric_type);
CREATE INDEX idx_wms_metric_define_category ON wms_metric_define(metric_category);
CREATE INDEX idx_wms_metric_define_mart ON wms_metric_define(mart_id);
CREATE INDEX idx_wms_metric_define_active ON wms_metric_define(is_active);

-- 3. 维度管理表
CREATE TABLE wms_dimension_define (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    dimension_id    VARCHAR(64)  NOT NULL,
    dimension_name  VARCHAR(128) NOT NULL,
    dimension_code  VARCHAR(64)  NOT NULL,
    dimension_type  VARCHAR(32)  NOT NULL,
    mart_id         VARCHAR(64),
    description     VARCHAR(512),
    data_type       VARCHAR(32) DEFAULT 'STRING',
    data_source     TEXT,
    dimension_config TEXT,
    hierarchy_level SMALLINT DEFAULT 1,
    parent_dimension_id VARCHAR(64),
    is_time_dimension VARCHAR(8) DEFAULT 'N',
    is_geo_dimension VARCHAR(8) DEFAULT 'N',
    is_active       VARCHAR(8) DEFAULT 'Y',
    sort_order      INT DEFAULT 100,
    created_by      VARCHAR(64),
    updated_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_dimension_define PRIMARY KEY (id),
    CONSTRAINT uk_wms_dimension_define_id UNIQUE (dimension_id),
    CONSTRAINT uk_wms_dimension_define_code UNIQUE (dimension_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_dimension_define_type ON wms_dimension_define(dimension_type);
CREATE INDEX idx_wms_dimension_define_mart ON wms_dimension_define(mart_id);
CREATE INDEX idx_wms_dimension_define_active ON wms_dimension_define(is_active);

-- 4. 数据模型表
CREATE TABLE wms_data_model (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    model_id        VARCHAR(64)  NOT NULL,
    model_name      VARCHAR(128) NOT NULL,
    model_code      VARCHAR(64)  NOT NULL,
    model_type      VARCHAR(32)  NOT NULL,
    mart_id         VARCHAR(64),
    description     VARCHAR(512),
    model_config    TEXT,
    table_name      VARCHAR(128),
    fields          TEXT,
    relations       TEXT,
    partitions      TEXT,
    indexes         TEXT,
    storage_engine  VARCHAR(32) DEFAULT 'OLAP',
    refresh_strategy VARCHAR(32) DEFAULT 'SCHEDULED',
    refresh_cron    VARCHAR(64),
    last_refresh_time TIMESTAMP,
    record_count    BIGINT,
    data_size_mb    INT(19,4),
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    is_active       VARCHAR(8) DEFAULT 'Y',
    sort_order      INT DEFAULT 100,
    created_by      VARCHAR(64),
    updated_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_data_model PRIMARY KEY (id),
    CONSTRAINT uk_wms_data_model_id UNIQUE (model_id),
    CONSTRAINT uk_wms_data_model_code UNIQUE (model_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_data_model_type ON wms_data_model(model_type);
CREATE INDEX idx_wms_data_model_mart ON wms_data_model(mart_id);
CREATE INDEX idx_wms_data_model_status ON wms_data_model(status);


-- 注释
ALTER TABLE wms_data_mart COMMENT='数据集市表';
ALTER TABLE wms_metric_define COMMENT='指标管理表';
ALTER TABLE wms_dimension_define COMMENT='维度管理表';
ALTER TABLE wms_data_model COMMENT='数据模型表';
