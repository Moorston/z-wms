-- ============================================================
-- X WMS 业务规则引擎模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 规则定义/规则参数/规则执行日志/规则版本
-- ============================================================

-- 1. 规则定义表
CREATE TABLE wms_rule_definition (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL,
    rule_name       VARCHAR(128) NOT NULL,
    rule_type       VARCHAR(32)  NOT NULL, -- PUTAWAY上架/ALLOCATION分配/ROTATION周转/WAVE波次/REPLENISH补货/QC质检/CROSSDOCK越库/DELIVERY配送/PATH路径/AUTOSHIP自动发运/PREALLOC预配/WAVESCHED波次调度
    rule_category   VARCHAR(32), -- SYSTEM系统/CUSTOM自定义/INDUSTRY行业
    description     VARCHAR(512),
    rule_script     TEXT, -- 规则脚本(Groovy/JavaScript/表达式)
    rule_config     TEXT, -- 规则配置(JSON)
    priority        SMALLINT     DEFAULT 100, -- 优先级(越小越高)
    enabled         TINYINT(1)     DEFAULT 1,
    version         SMALLINT     DEFAULT 1,
    owner_code_col  VARCHAR(64),
    warehouse_code  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_rule_def PRIMARY KEY (id),
    CONSTRAINT uk_wms_rule_code UNIQUE (rule_code, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_rule_type ON wms_rule_definition(rule_type);
CREATE INDEX idx_wms_rule_owner ON wms_rule_definition(owner_code_col);

-- 2. 规则参数表
CREATE TABLE wms_rule_param (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL,
    param_code      VARCHAR(64)  NOT NULL,
    param_name      VARCHAR(128) NOT NULL,
    param_type      VARCHAR(32)  NOT NULL, -- STRING/INT/BOOLEAN/ENUM/JSON
    param_value     VARCHAR(1024),
    default_value   VARCHAR(1024),
    is_required     TINYINT(1)     DEFAULT 0,
    description     VARCHAR(512),
    sort_order      SMALLINT     DEFAULT 0,
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_rule_param PRIMARY KEY (id),
    CONSTRAINT uk_wms_rule_param UNIQUE (rule_code, param_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_rule_param_rule ON wms_rule_param(rule_code);

-- 3. 规则执行日志表
CREATE TABLE wms_rule_exec_log (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    exec_no         VARCHAR(64)  NOT NULL,
    rule_code       VARCHAR(64)  NOT NULL,
    rule_type       VARCHAR(32),
    biz_no          VARCHAR(64), -- 业务单号
    biz_type        VARCHAR(32), -- 业务类型
    input_data      TEXT, -- 输入数据(JSON)
    output_data     TEXT, -- 输出数据(JSON)
    exec_result     VARCHAR(32), -- SUCCESS/FAIL/SKIP
    error_msg       VARCHAR(1024),
    exec_duration   INT, -- 执行耗时(ms)
    operator        VARCHAR(64),
    exec_time       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_rule_log PRIMARY KEY (id),
    CONSTRAINT uk_wms_rule_exec_no UNIQUE (exec_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_rule_log_rule ON wms_rule_exec_log(rule_code);
CREATE INDEX idx_wms_rule_log_biz ON wms_rule_exec_log(biz_type, biz_no);
CREATE INDEX idx_wms_rule_log_time ON wms_rule_exec_log(exec_time);

-- 4. 规则版本表
CREATE TABLE wms_rule_version (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL,
    version         SMALLINT     NOT NULL,
    rule_script     TEXT,
    rule_config     TEXT,
    change_desc     VARCHAR(512),
    is_current      TINYINT(1)     DEFAULT 0,
    operator        VARCHAR(64),
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_rule_ver PRIMARY KEY (id),
    CONSTRAINT uk_wms_rule_ver UNIQUE (rule_code, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 注释
ALTER TABLE wms_rule_definition COMMENT='规则定义表';
ALTER TABLE wms_rule_param COMMENT='规则参数表';
ALTER TABLE wms_rule_exec_log COMMENT='规则执行日志表';
ALTER TABLE wms_rule_version COMMENT='规则版本表';
