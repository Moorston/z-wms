-- ============================================================
-- X WMS 库存可视化大屏管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 大屏配置/实时监控/数据看板/可视化组件
-- ============================================================

-- 1. 大屏配置表
CREATE TABLE wms_dashboard_config (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    config_id       VARCHAR(64)  NOT NULL,
    config_name     VARCHAR(128) NOT NULL,
    config_code     VARCHAR(64)  NOT NULL,
    dashboard_type  VARCHAR(32)  NOT NULL,
    warehouse_code  VARCHAR(64),
    owner_code      VARCHAR(64),
    description     VARCHAR(512),
    layout_config   TEXT,
    theme_config    TEXT,
    refresh_interval INT DEFAULT 30,
    is_default      VARCHAR(8) DEFAULT 'N',
    is_active       VARCHAR(8) DEFAULT 'Y',
    sort_order      INT DEFAULT 100,
    created_by      VARCHAR(64),
    updated_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_dashboard_config PRIMARY KEY (id),
    CONSTRAINT uk_wms_dashboard_config_id UNIQUE (config_id),
    CONSTRAINT uk_wms_dashboard_config_code UNIQUE (config_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_dashboard_config_type ON wms_dashboard_config(dashboard_type);
CREATE INDEX idx_wms_dashboard_config_wh ON wms_dashboard_config(warehouse_code);
CREATE INDEX idx_wms_dashboard_config_active ON wms_dashboard_config(is_active);

-- 2. 实时监控表
CREATE TABLE wms_realtime_monitor (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    monitor_id      VARCHAR(64)  NOT NULL,
    monitor_name    VARCHAR(128) NOT NULL,
    monitor_type    VARCHAR(32)  NOT NULL,
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    description     VARCHAR(512),
    monitor_config  TEXT,
    current_value   INT(19,4),
    target_value    INT(19,4),
    threshold_warning INT(19,4),
    threshold_critical INT(19,4),
    unit            VARCHAR(32),
    status          VARCHAR(32) DEFAULT 'NORMAL',
    trend           VARCHAR(16),
    change_rate     INT(10,2),
    last_update_time TIMESTAMP,
    refresh_interval INT DEFAULT 10,
    is_active       VARCHAR(8) DEFAULT 'Y',
    operator        VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_realtime_monitor PRIMARY KEY (id),
    CONSTRAINT uk_wms_realtime_monitor_id UNIQUE (monitor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_realtime_monitor_wh ON wms_realtime_monitor(warehouse_code);
CREATE INDEX idx_wms_realtime_monitor_type ON wms_realtime_monitor(monitor_type);
CREATE INDEX idx_wms_realtime_monitor_status ON wms_realtime_monitor(status);

-- 3. 数据看板表
CREATE TABLE wms_data_board (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    board_id        VARCHAR(64)  NOT NULL,
    board_name      VARCHAR(128) NOT NULL,
    board_code      VARCHAR(64)  NOT NULL,
    board_type      VARCHAR(32)  NOT NULL,
    warehouse_code  VARCHAR(64),
    owner_code      VARCHAR(64),
    description     VARCHAR(512),
    board_config    TEXT,
    data_source     TEXT,
    chart_config    TEXT,
    refresh_interval INT DEFAULT 60,
    period_type     VARCHAR(32) DEFAULT 'REAL_TIME',
    is_active       VARCHAR(8) DEFAULT 'Y',
    sort_order      INT DEFAULT 100,
    created_by      VARCHAR(64),
    updated_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_data_board PRIMARY KEY (id),
    CONSTRAINT uk_wms_data_board_id UNIQUE (board_id),
    CONSTRAINT uk_wms_data_board_code UNIQUE (board_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_data_board_type ON wms_data_board(board_type);
CREATE INDEX idx_wms_data_board_wh ON wms_data_board(warehouse_code);
CREATE INDEX idx_wms_data_board_active ON wms_data_board(is_active);

-- 4. 可视化组件表
CREATE TABLE wms_visual_component (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    component_id    VARCHAR(64)  NOT NULL,
    component_name  VARCHAR(128) NOT NULL,
    component_code  VARCHAR(64)  NOT NULL,
    component_type  VARCHAR(32)  NOT NULL,
    description     VARCHAR(512),
    component_config TEXT,
    data_config     TEXT,
    style_config    TEXT,
    interaction_config TEXT,
    is_builtin      VARCHAR(8) DEFAULT 'N',
    is_active       VARCHAR(8) DEFAULT 'Y',
    sort_order      INT DEFAULT 100,
    created_by      VARCHAR(64),
    updated_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_visual_component PRIMARY KEY (id),
    CONSTRAINT uk_wms_visual_component_id UNIQUE (component_id),
    CONSTRAINT uk_wms_visual_component_code UNIQUE (component_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_visual_component_type ON wms_visual_component(component_type);
CREATE INDEX idx_wms_visual_component_active ON wms_visual_component(is_active);


-- 注释
ALTER TABLE wms_dashboard_config COMMENT='大屏配置表';
ALTER TABLE wms_realtime_monitor COMMENT='实时监控表';
ALTER TABLE wms_data_board COMMENT='数据看板表';
ALTER TABLE wms_visual_component COMMENT='可视化组件表';
