-- ============================================================
-- X WMS 系统监控与安全审计模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 操作日志/登录日志/安全审计/系统监控指标
-- ============================================================

-- 1. 操作日志表
CREATE TABLE sys_operation_log (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    log_no          VARCHAR(64)  NOT NULL,
    user_id         VARCHAR(64),
    user_name       VARCHAR(64),
    module          VARCHAR(64),            -- 模块: 入库/出库/库存/系统等
    operation       VARCHAR(64),            -- 操作: 创建/修改/删除/审核等
    method          VARCHAR(256),           -- 方法名
    request_url     VARCHAR(512),
    request_method  VARCHAR(16),            -- GET/POST/PUT/DELETE
    request_params  TEXT,                    -- 请求参数
    response_result TEXT,                    -- 响应结果
    ip_address      VARCHAR(64),
    user_agent      VARCHAR(512),
    cost_time       INT,              -- 耗时(ms)
    status          VARCHAR(16),            -- SUCCESS成功/FAILED失败
    error_msg       TEXT,
    business_type   VARCHAR(32),            -- 业务类型
    business_no     VARCHAR(64),            -- 业务单号
    trace_id        VARCHAR(64),            -- 链路追踪ID
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sys_operation_log PRIMARY KEY (id),
    CONSTRAINT uk_sys_op_log_no UNIQUE (log_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_sys_op_log_user ON sys_operation_log(user_id);
CREATE INDEX idx_sys_op_log_module ON sys_operation_log(module);
CREATE INDEX idx_sys_op_log_time ON sys_operation_log(created_time);
CREATE INDEX idx_sys_op_log_biz ON sys_operation_log(business_type, business_no);

-- 2. 登录日志表
CREATE TABLE sys_login_log (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    log_no          VARCHAR(64)  NOT NULL,
    user_id         VARCHAR(64),
    user_name       VARCHAR(64),
    login_type      VARCHAR(16),            -- LOGIN登录/LOGOUT登出
    login_status    VARCHAR(16),            -- SUCCESS成功/FAILED失败
    fail_reason     VARCHAR(256),           -- 失败原因: 密码错误/账号锁定/验证码错误等
    ip_address      VARCHAR(64),
    user_agent      VARCHAR(512),
    device_type     VARCHAR(32),            -- PC/PDA/MOBILE
    browser         VARCHAR(64),
    os              VARCHAR(64),
    location        VARCHAR(128),           -- 登录地点
    session_id      VARCHAR(128),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sys_login_log PRIMARY KEY (id),
    CONSTRAINT uk_sys_login_log_no UNIQUE (log_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_sys_login_user ON sys_login_log(user_id);
CREATE INDEX idx_sys_login_time ON sys_login_log(created_time);
CREATE INDEX idx_sys_login_status ON sys_login_log(login_status);

-- 3. 安全审计表
CREATE TABLE sys_security_audit (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    audit_no        VARCHAR(64)  NOT NULL,
    audit_type      VARCHAR(32)  NOT NULL,  -- LOGIN异常/PERMISSION越权/DATA_EXPORT数据导出/CONFIG_CHANGE配置变更/SENSITIVE_ACCESS敏感数据访问
    user_id         VARCHAR(64),
    user_name       VARCHAR(64),
    risk_level      VARCHAR(16),            -- LOW低/MEDIUM中/HIGH高/CRITICAL严重
    description     VARCHAR(1024),
    resource_type   VARCHAR(32),            -- 资源类型: USER/ROLE/DATA/API/CONFIG
    resource_id     VARCHAR(64),
    action          VARCHAR(64),            -- 操作: ACCESS/MODIFY/DELETE/EXPORT
    before_value    TEXT,
    after_value     TEXT,
    ip_address      VARCHAR(64),
    status          VARCHAR(16)  DEFAULT 'PENDING', -- PENDING待处理/PROCESSING处理中/RESOLVED已解决/IGNORED已忽略
    handled_by      VARCHAR(64),
    handled_time    TIMESTAMP,
    handle_remark   VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sys_security_audit PRIMARY KEY (id),
    CONSTRAINT uk_sys_audit_no UNIQUE (audit_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_sys_audit_type ON sys_security_audit(audit_type);
CREATE INDEX idx_sys_audit_risk ON sys_security_audit(risk_level);
CREATE INDEX idx_sys_audit_status ON sys_security_audit(status);

-- 4. 系统监控指标表
CREATE TABLE sys_monitor_metric (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    metric_name     VARCHAR(64)  NOT NULL,  -- CPU/MEMORY/DISK/DB_CONN/API_QPS/API_LATENCY等
    metric_category VARCHAR(32),            -- SYSTEM系统/APPLICATION应用/DATABASE数据库/BUSINESS业务
    metric_value    INT(18,4),
    metric_unit     VARCHAR(16),            -- %/MB/GB/ms/count等
    threshold_warn  INT(18,4),            -- 告警阈值
    threshold_critical INT(18,4),         -- 严重阈值
    status          VARCHAR(16),            -- NORMAL正常/WARN告警/CRITICAL严重
    instance_id     VARCHAR(64),            -- 实例ID(多实例时)
    collected_time  TIMESTAMP     NOT NULL,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sys_monitor_metric PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_sys_metric_name ON sys_monitor_metric(metric_name);
CREATE INDEX idx_sys_metric_time ON sys_monitor_metric(collected_time);
CREATE INDEX idx_sys_metric_status ON sys_monitor_metric(status);


-- 注释
ALTER TABLE sys_operation_log COMMENT='操作日志表';
ALTER TABLE sys_login_log COMMENT='登录日志表';
ALTER TABLE sys_security_audit COMMENT='安全审计表';
ALTER TABLE sys_monitor_metric COMMENT='系统监控指标表';
