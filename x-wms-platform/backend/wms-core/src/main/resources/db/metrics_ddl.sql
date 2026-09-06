-- ============================================================
-- X WMS 库存指标体系管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 指标体系/指标分类/指标计算/指标监控
-- ============================================================

-- 1. 指标体系表
CREATE TABLE wms_metrics_system (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    system_id       VARCHAR(64)  NOT NULL,
    system_name     VARCHAR(128) NOT NULL,
    system_code     VARCHAR(64)  NOT NULL,
    system_type     VARCHAR(32)  NOT NULL,
    warehouse_code  VARCHAR(64),
    owner_code      VARCHAR(64),
    description     VARCHAR(512),
    system_config   TEXT,
    metric_count    INT DEFAULT 0,
    category_count  INT DEFAULT 0,
    is_default      VARCHAR(8) DEFAULT 'N',
    is_active       VARCHAR(8) DEFAULT 'Y',
    sort_order      INT DEFAULT 100,
    created_by      VARCHAR(64),
    updated_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_metrics_system PRIMARY KEY (id),
    CONSTRAINT uk_wms_metrics_system_id UNIQUE (system_id),
    CONSTRAINT uk_wms_metrics_system_code UNIQUE (system_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_metrics_system_type ON wms_metrics_system(system_type);
CREATE INDEX idx_wms_metrics_system_wh ON wms_metrics_system(warehouse_code);
CREATE INDEX idx_wms_metrics_system_active ON wms_metrics_system(is_active);

-- 2. 指标分类表
CREATE TABLE wms_metrics_category (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    category_id     VARCHAR(64)  NOT NULL,
    category_name   VARCHAR(128) NOT NULL,
    category_code   VARCHAR(64)  NOT NULL,
    system_id       VARCHAR(64)  NOT NULL,
    parent_category_id VARCHAR(64),
    category_level  SMALLINT DEFAULT 1,
    description     VARCHAR(512),
    category_config TEXT,
    metric_count    INT DEFAULT 0,
    is_active       VARCHAR(8) DEFAULT 'Y',
    sort_order      INT DEFAULT 100,
    created_by      VARCHAR(64),
    updated_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_metrics_category PRIMARY KEY (id),
    CONSTRAINT uk_wms_metrics_category_id UNIQUE (category_id),
    CONSTRAINT uk_wms_metrics_category_code UNIQUE (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_metrics_category_system ON wms_metrics_category(system_id);
CREATE INDEX idx_wms_metrics_category_parent ON wms_metrics_category(parent_category_id);
CREATE INDEX idx_wms_metrics_category_active ON wms_metrics_category(is_active);

-- 3. 指标计算表
CREATE TABLE wms_metrics_calculation (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    calc_id         VARCHAR(64)  NOT NULL,
    metric_id       VARCHAR(64)  NOT NULL,
    metric_code     VARCHAR(64)  NOT NULL,
    metric_name     VARCHAR(128),
    system_id       VARCHAR(64),
    warehouse_code  VARCHAR(64),
    owner_code      VARCHAR(64),
    period_type     VARCHAR(32) DEFAULT 'DAILY',
    period_start    TIMESTAMP,
    period_end      TIMESTAMP,
    calc_formula    VARCHAR(1024),
    calc_params     TEXT,
    calc_result     INT(19,4),
    calc_unit       VARCHAR(32),
    target_value    INT(19,4),
    benchmark_value INT(19,4),
    deviation       INT(19,4),
    deviation_rate  INT(10,2),
    status          VARCHAR(32) DEFAULT 'PENDING',
    error_message   VARCHAR(1024),
    calc_start_time TIMESTAMP,
    calc_end_time   TIMESTAMP,
    duration_ms     BIGINT,
    operator        VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_metrics_calculation PRIMARY KEY (id),
    CONSTRAINT uk_wms_metrics_calculation_id UNIQUE (calc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_metrics_calculation_metric ON wms_metrics_calculation(metric_id);
CREATE INDEX idx_wms_metrics_calculation_system ON wms_metrics_calculation(system_id);
CREATE INDEX idx_wms_metrics_calculation_wh ON wms_metrics_calculation(warehouse_code);
CREATE INDEX idx_wms_metrics_calculation_status ON wms_metrics_calculation(status);
CREATE INDEX idx_wms_metrics_calculation_period ON wms_metrics_calculation(period_start, period_end);

-- 4. 指标监控表
CREATE TABLE wms_metrics_monitor (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    monitor_id      VARCHAR(64)  NOT NULL,
    metric_id       VARCHAR(64)  NOT NULL,
    metric_code     VARCHAR(64)  NOT NULL,
    metric_name     VARCHAR(128),
    system_id       VARCHAR(64),
    warehouse_code  VARCHAR(64),
    owner_code      VARCHAR(64),
    monitor_type    VARCHAR(32) DEFAULT 'THRESHOLD',
    monitor_config  TEXT,
    current_value   INT(19,4),
    target_value    INT(19,4),
    threshold_warning INT(19,4),
    threshold_critical INT(19,4),
    unit            VARCHAR(32),
    status          VARCHAR(32) DEFAULT 'NORMAL',
    trend           VARCHAR(16),
    change_rate     INT(10,2),
    alert_count     INT DEFAULT 0,
    last_alert_time TIMESTAMP,
    last_check_time TIMESTAMP,
    check_interval  INT DEFAULT 60,
    is_active       VARCHAR(8) DEFAULT 'Y',
    operator        VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_metrics_monitor PRIMARY KEY (id),
    CONSTRAINT uk_wms_metrics_monitor_id UNIQUE (monitor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_metrics_monitor_metric ON wms_metrics_monitor(metric_id);
CREATE INDEX idx_wms_metrics_monitor_system ON wms_metrics_monitor(system_id);
CREATE INDEX idx_wms_metrics_monitor_wh ON wms_metrics_monitor(warehouse_code);
CREATE INDEX idx_wms_metrics_monitor_status ON wms_metrics_monitor(status);
CREATE INDEX idx_wms_metrics_monitor_active ON wms_metrics_monitor(is_active);


-- 注释
ALTER TABLE wms_metrics_system COMMENT='指标体系表';
ALTER TABLE wms_metrics_category COMMENT='指标分类表';
ALTER TABLE wms_metrics_calculation COMMENT='指标计算表';
ALTER TABLE wms_metrics_monitor COMMENT='指标监控表';
