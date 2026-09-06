-- ============================================================
-- X WMS 行业插件模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 插件注册/插件配置/插件执行日志/行业规则
-- ============================================================

-- 1. 插件注册表
CREATE TABLE wms_plugin (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    plugin_code     VARCHAR(64)  NOT NULL,
    plugin_name     VARCHAR(128) NOT NULL,
    plugin_type     VARCHAR(32)  NOT NULL, -- INDUSTRY行业/BUSINESS业务/TECH技术
    industry        VARCHAR(64),  -- 行业: GSP医药/COLD_CHAIN冷链/ECOMMERCE电商/MANUFACTURING制造
    version         VARCHAR(32),
    description     VARCHAR(1024),
    plugin_class    VARCHAR(512), -- 插件主类
    entry_point     VARCHAR(256), -- 入口点
    priority        SMALLINT     DEFAULT 100, -- 优先级, 数字越小优先级越高
    status          VARCHAR(16)  DEFAULT 'INSTALLED', -- INSTALLED已安装/ENABLED已启用/DISABLED已禁用/ERROR异常
    config_schema   TEXT,          -- 配置Schema(JSON)
    dependencies    TEXT,          -- 依赖插件(JSON数组)
    author          VARCHAR(64),
    installed_time  TIMESTAMP,
    enabled_time    TIMESTAMP,
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_plugin PRIMARY KEY (id),
    CONSTRAINT uk_wms_plugin_code UNIQUE (plugin_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_plugin_industry ON wms_plugin(industry);
CREATE INDEX idx_wms_plugin_status ON wms_plugin(status);

-- 2. 插件配置表
CREATE TABLE wms_plugin_config (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    plugin_code     VARCHAR(64)  NOT NULL,
    config_key      VARCHAR(128) NOT NULL,
    config_value    VARCHAR(2048),
    config_type     VARCHAR(32),  -- STRING/INT/BOOLEAN/JSON
    description     VARCHAR(512),
    is_required     TINYINT(1)     DEFAULT 0,
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_plugin_config PRIMARY KEY (id),
    CONSTRAINT uk_wms_plugin_cfg UNIQUE (plugin_code, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_plugin_cfg_plugin ON wms_plugin_config(plugin_code);

-- 3. 插件执行日志表
CREATE TABLE wms_plugin_log (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    log_no          VARCHAR(64)  NOT NULL,
    plugin_code     VARCHAR(64)  NOT NULL,
    plugin_name     VARCHAR(128),
    business_type   VARCHAR(64),  -- 业务类型: INBOUND/OUTBOUND/INVENTORY等
    business_no     VARCHAR(64),  -- 业务单号
    trigger_point   VARCHAR(128), -- 触发点
    input_data      TEXT,
    output_data     TEXT,
    status          VARCHAR(16)  DEFAULT 'SUCCESS', -- SUCCESS/FAILED/SKIPPED
    error_msg       VARCHAR(1024),
    cost_time       INT,   -- 耗时ms
    trace_id        VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_plugin_log PRIMARY KEY (id),
    CONSTRAINT uk_wms_plugin_log_no UNIQUE (log_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_plugin_log_plugin ON wms_plugin_log(plugin_code);
CREATE INDEX idx_wms_plugin_log_biz ON wms_plugin_log(business_type, business_no);
CREATE INDEX idx_wms_plugin_log_time ON wms_plugin_log(created_time);

-- 4. 行业规则表
CREATE TABLE wms_industry_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL,
    rule_name       VARCHAR(128) NOT NULL,
    industry        VARCHAR(64)  NOT NULL, -- GSP/COLD_CHAIN/ECOMMERCE等
    rule_type       VARCHAR(32),  -- VALIDATION校验/PROCESS流程/NOTIFICATION通知
    trigger_event   VARCHAR(128), -- 触发事件
    rule_expression TEXT,          -- 规则表达式
    rule_action     TEXT,          -- 规则动作(JSON)
    priority        SMALLINT     DEFAULT 100,
    enabled         TINYINT(1)     DEFAULT 1,
    description     VARCHAR(1024),
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_industry_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_ind_rule_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_ind_rule_industry ON wms_industry_rule(industry);
CREATE INDEX idx_wms_ind_rule_event ON wms_industry_rule(trigger_event);


-- 注释
ALTER TABLE wms_plugin COMMENT='插件注册表';
ALTER TABLE wms_plugin_config COMMENT='插件配置表';
ALTER TABLE wms_plugin_log COMMENT='插件执行日志表';
ALTER TABLE wms_industry_rule COMMENT='行业规则表';
