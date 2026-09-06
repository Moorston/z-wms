-- ============================================================
-- X WMS API网关与平台模块 DDL
-- 数据库: MySQL 8.x
-- 包含: API定义/API调用日志/API密钥/API限流配置
-- ============================================================

-- 1. API定义表
CREATE TABLE wms_api_definition (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    api_code        VARCHAR(64)  NOT NULL,
    api_name        VARCHAR(128) NOT NULL,
    api_path        VARCHAR(256) NOT NULL,
    api_method      VARCHAR(16)  NOT NULL, -- GET/POST/PUT/DELETE
    api_category    VARCHAR(32), -- INBOUND入库/OUTBOUND出库/INVENTORY库存/BASE基础/REPORT报表
    description     VARCHAR(512),
    request_params  TEXT, -- 请求参数(JSON Schema)
    response_schema TEXT, -- 响应结构(JSON Schema)
    auth_required   TINYINT(1)     DEFAULT 1, -- 是否需要鉴权
    rate_limit      INT    DEFAULT 100, -- 限流(QPS)
    timeout         SMALLINT     DEFAULT 30, -- 超时(秒)
    enabled         TINYINT(1)     DEFAULT 1,
    version         VARCHAR(16)  DEFAULT 'v1',
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_api_def PRIMARY KEY (id),
    CONSTRAINT uk_wms_api_code UNIQUE (api_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_api_path ON wms_api_definition(api_path);
CREATE INDEX idx_wms_api_cat ON wms_api_definition(api_category);

-- 2. API调用日志表
CREATE TABLE wms_api_call_log (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    log_no          VARCHAR(64)  NOT NULL,
    api_code        VARCHAR(64),
    api_path        VARCHAR(256),
    api_method      VARCHAR(16),
    app_key         VARCHAR(64), -- 调用方
    request_ip      VARCHAR(64),
    request_headers TEXT,
    request_body    TEXT,
    response_status SMALLINT,
    response_body   TEXT,
    duration_ms     INT, -- 耗时(ms)
    call_status     VARCHAR(32), -- SUCCESS/FAIL/TIMEOUT/RATE_LIMITED
    error_msg       VARCHAR(1024),
    trace_id        VARCHAR(64),
    call_time       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_api_log PRIMARY KEY (id),
    CONSTRAINT uk_wms_api_log_no UNIQUE (log_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_api_log_api ON wms_api_call_log(api_code);
CREATE INDEX idx_wms_api_log_time ON wms_api_call_log(call_time);
CREATE INDEX idx_wms_api_log_app ON wms_api_call_log(app_key);

-- 3. API密钥表
CREATE TABLE wms_api_key (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    app_key         VARCHAR(64)  NOT NULL,
    app_secret      VARCHAR(256) NOT NULL,
    app_name        VARCHAR(128) NOT NULL,
    app_type        VARCHAR(32), -- ERP/TMS/WCS/THIRD_PARTY/INTERNAL
    description     VARCHAR(512),
    rate_limit      INT    DEFAULT 100, -- 全局限流(QPS)
    ip_whitelist    TEXT, -- IP白名单(JSON数组)
    status          VARCHAR(32) DEFAULT 'ACTIVE', -- ACTIVE/DISABLED/EXPIRED
    expire_time     TIMESTAMP,
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_api_key PRIMARY KEY (id),
    CONSTRAINT uk_wms_api_appkey UNIQUE (app_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. API限流配置表
CREATE TABLE wms_api_rate_limit (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    limit_code      VARCHAR(64)  NOT NULL,
    limit_name      VARCHAR(128),
    target_type     VARCHAR(32)  NOT NULL, -- API/APP/IP/GLOBAL
    target_value    VARCHAR(128) NOT NULL, -- api_code/app_key/ip/global
    limit_qps       INT    DEFAULT 100,
    limit_day       INT, -- 日调用次数限制
    burst_size      INT    DEFAULT 10, -- 突发容量
    window_type     VARCHAR(32) DEFAULT 'SLIDING', -- FIXED固定/SLIDING滑动/TOKEN令牌桶
    enabled         TINYINT(1)     DEFAULT 1,
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_api_limit PRIMARY KEY (id),
    CONSTRAINT uk_wms_api_limit UNIQUE (target_type, target_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 注释
ALTER TABLE wms_api_definition COMMENT='API定义表';
ALTER TABLE wms_api_call_log COMMENT='API调用日志表';
ALTER TABLE wms_api_key COMMENT='API密钥表';
ALTER TABLE wms_api_rate_limit COMMENT='API限流配置表';
