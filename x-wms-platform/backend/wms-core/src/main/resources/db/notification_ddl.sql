-- ============================================================
-- X WMS 消息推送与通知模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 通知模板/通知记录/通知规则/用户通知设置
-- ============================================================

-- 1. 通知模板表
CREATE TABLE wms_notify_template (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    template_code   VARCHAR(64)  NOT NULL,
    template_name   VARCHAR(128) NOT NULL,
    notify_type     VARCHAR(32)  NOT NULL, -- SYSTEM系统/ALERT告警/BUSINESS业务/REMIND提醒
    channel         VARCHAR(32)  NOT NULL, -- IN_APP站内信/SMS短信/EMAIL邮件/DINGTALK钉钉/WECHAT企业微信/FEISHU飞书
    title_template  VARCHAR(256),
    content_template TEXT,                   -- 支持${变量}占位符
    variables       TEXT,                    -- 变量定义(JSON): [{"name":"orderNo","desc":"订单号","required":true}]
    enabled         TINYINT(1)     DEFAULT 1,
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    deleted         TINYINT(1)     DEFAULT 0,
    CONSTRAINT pk_wms_notify_template PRIMARY KEY (id),
    CONSTRAINT uk_wms_notify_tpl_code UNIQUE (template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_notify_tpl_type ON wms_notify_template(notify_type);

-- 2. 通知记录表
CREATE TABLE wms_notify_record (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    record_no       VARCHAR(64)  NOT NULL,
    template_code   VARCHAR(64),
    notify_type     VARCHAR(32),
    channel         VARCHAR(32)  NOT NULL,
    title           VARCHAR(256),
    content         TEXT,
    sender          VARCHAR(64),            -- 发送人(系统/具体用户)
    receiver_type   VARCHAR(16),            -- USER用户/ROLE角色/DEPT部门/ALL全部
    receiver_id     VARCHAR(64),            -- 接收者ID(用户ID/角色编码/部门编码)
    receiver_name   VARCHAR(128),
    business_type   VARCHAR(32),            -- 关联业务类型
    business_no     VARCHAR(64),            -- 关联业务单号
    priority        VARCHAR(8)   DEFAULT 'NORMAL', -- LOW低/NORMAL正常/HIGH高/URGENT紧急
    status          VARCHAR(16)  DEFAULT 'PENDING', -- PENDING待发送/SENDING发送中/SENT已发送/FAILED失败/READ已读
    retry_count     SMALLINT     DEFAULT 0,
    max_retry       SMALLINT     DEFAULT 3,
    error_msg       VARCHAR(1024),
    send_time       TIMESTAMP,
    read_time       TIMESTAMP,
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_notify_record PRIMARY KEY (id),
    CONSTRAINT uk_wms_notify_record_no UNIQUE (record_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_notify_rec_receiver ON wms_notify_record(receiver_id, status);
CREATE INDEX idx_wms_notify_rec_status ON wms_notify_record(status);
CREATE INDEX idx_wms_notify_rec_biz ON wms_notify_record(business_type, business_no);

-- 3. 通知规则表
CREATE TABLE wms_notify_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL,
    rule_name       VARCHAR(128) NOT NULL,
    event_type      VARCHAR(64)  NOT NULL, -- 触发事件: ORDER_CREATED/STOCK_LOW/QC_FAILED/APPOINTMENT_OVERDUE等
    template_code   VARCHAR(64)  NOT NULL,
    channel         VARCHAR(32)  NOT NULL,
    receiver_type   VARCHAR(16),
    receiver_id     VARCHAR(64),
    condition_expr  VARCHAR(1024),          -- 触发条件表达式(Spring EL)
    enabled         TINYINT(1)     DEFAULT 1,
    priority        SMALLINT     DEFAULT 100,
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_notify_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_notify_rule_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_notify_rule_event ON wms_notify_rule(event_type);

-- 4. 用户通知设置表
CREATE TABLE wms_user_notify_setting (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    user_id         BIGINT  NOT NULL,
    notify_type     VARCHAR(32) NOT NULL,
    channel         VARCHAR(32) NOT NULL,
    enabled         TINYINT(1)    DEFAULT 1,
    quiet_start     VARCHAR(8),             -- 免打扰开始时间 HH:mm
    quiet_end       VARCHAR(8),             -- 免打扰结束时间 HH:mm
    created_time    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_user_notify_setting PRIMARY KEY (id),
    CONSTRAINT uk_wms_user_notify UNIQUE (user_id, notify_type, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 注释
ALTER TABLE wms_notify_template COMMENT='通知模板表';
ALTER TABLE wms_notify_record COMMENT='通知记录表';
ALTER TABLE wms_notify_rule COMMENT='通知规则表';
ALTER TABLE wms_user_notify_setting COMMENT='用户通知设置表';
