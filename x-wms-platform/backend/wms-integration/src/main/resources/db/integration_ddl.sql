-- ============================================================
-- X WMS WCS/TMS/ERP集成模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 外部系统配置/接口调用日志/消息队列/回调记录
-- ============================================================

-- 1. 外部系统配置表
CREATE TABLE wms_external_system (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    system_code     VARCHAR(64)  NOT NULL,
    system_name     VARCHAR(128) NOT NULL,
    system_type     VARCHAR(32)  NOT NULL, -- WCS设备控制/TMS运输/ERP企业资源/OMS订单/CRM客户/BI商业智能/OTHER其他
    base_url        VARCHAR(512),
    api_key         VARCHAR(256),
    api_secret      VARCHAR(512),
    auth_type       VARCHAR(16),  -- NONE/BASIC/BEARER/API_KEY/OAUTH2
    timeout         SMALLINT      DEFAULT 30000, -- 超时ms
    retry_count     TINYINT(1)      DEFAULT 3,
    retry_interval  SMALLINT      DEFAULT 5000, -- 重试间隔ms
    status          VARCHAR(16)   DEFAULT 'ACTIVE', -- ACTIVE/INACTIVE/MAINTENANCE
    description     VARCHAR(512),
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_ext_system PRIMARY KEY (id),
    CONSTRAINT uk_wms_ext_sys_code UNIQUE (system_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_ext_sys_type ON wms_external_system(system_type);

-- 2. 接口调用日志表
CREATE TABLE wms_api_call_log (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    log_no          VARCHAR(64)  NOT NULL,
    system_code     VARCHAR(64)  NOT NULL,
    system_name     VARCHAR(128),
    api_name        VARCHAR(128),
    api_url         VARCHAR(512),
    http_method     VARCHAR(16),
    request_headers TEXT,
    request_body    TEXT,
    response_status SMALLINT,
    response_body   TEXT,
    cost_time       INT,   -- 耗时ms
    status          VARCHAR(16)  DEFAULT 'SUCCESS', -- SUCCESS/FAILED/TIMEOUT
    error_msg       VARCHAR(1024),
    retry_count     TINYINT(1)     DEFAULT 0,
    trace_id        VARCHAR(64),
    business_type   VARCHAR(32),
    business_no     VARCHAR(64),
    direction       VARCHAR(8),  -- INBOUND入站/OUTBOUND出站
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_api_call_log PRIMARY KEY (id),
    CONSTRAINT uk_wms_api_log_no UNIQUE (log_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_api_log_system ON wms_api_call_log(system_code);
CREATE INDEX idx_wms_api_log_time ON wms_api_call_log(created_time);
CREATE INDEX idx_wms_api_log_status ON wms_api_call_log(status);
CREATE INDEX idx_wms_api_log_biz ON wms_api_call_log(business_type, business_no);

-- 3. 消息队列表
CREATE TABLE wms_integration_message (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    message_id      VARCHAR(64)  NOT NULL,
    system_code     VARCHAR(64)  NOT NULL,
    message_type    VARCHAR(64)  NOT NULL, -- ORDER_CREATE/ORDER_UPDATE/INVENTORY_SYNC/SHIPMENT_NOTIFY等
    topic           VARCHAR(128),
    payload         TEXT,
    status          VARCHAR(16)  DEFAULT 'PENDING', -- PENDING待发送/SENDING发送中/SENT已发送/FAILED失败/CONSUMED已消费
    retry_count     TINYINT(1)     DEFAULT 0,
    max_retry       TINYINT(1)     DEFAULT 5,
    next_retry_time TIMESTAMP,
    sent_time       TIMESTAMP,
    consumed_time   TIMESTAMP,
    error_msg       VARCHAR(1024),
    business_type   VARCHAR(32),
    business_no     VARCHAR(64),
    direction       VARCHAR(8),  -- SEND发送/RECEIVE接收
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_int_message PRIMARY KEY (id),
    CONSTRAINT uk_wms_int_msg_id UNIQUE (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_int_msg_status ON wms_integration_message(status);
CREATE INDEX idx_wms_int_msg_system ON wms_integration_message(system_code);
CREATE INDEX idx_wms_int_msg_type ON wms_integration_message(message_type);

-- 4. 回调记录表
CREATE TABLE wms_callback_record (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    callback_no     VARCHAR(64)  NOT NULL,
    system_code     VARCHAR(64)  NOT NULL,
    callback_url    VARCHAR(512),
    callback_type   VARCHAR(64),  -- 回调类型
    request_body    TEXT,
    response_status SMALLINT,
    response_body   TEXT,
    status          VARCHAR(16)  DEFAULT 'PENDING', -- PENDING待回调/CALLBACKING回调中/SUCCESS成功/FAILED失败
    retry_count     TINYINT(1)     DEFAULT 0,
    max_retry       TINYINT(1)     DEFAULT 5,
    next_retry_time TIMESTAMP,
    cost_time       INT,
    error_msg       VARCHAR(1024),
    business_type   VARCHAR(32),
    business_no     VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_callback PRIMARY KEY (id),
    CONSTRAINT uk_wms_callback_no UNIQUE (callback_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_callback_status ON wms_callback_record(status);
CREATE INDEX idx_wms_callback_system ON wms_callback_record(system_code);


-- 注释
ALTER TABLE wms_external_system COMMENT='外部系统配置表';
ALTER TABLE wms_api_call_log COMMENT='接口调用日志表';
ALTER TABLE wms_integration_message COMMENT='集成消息队列表';
ALTER TABLE wms_callback_record COMMENT='回调记录表';
