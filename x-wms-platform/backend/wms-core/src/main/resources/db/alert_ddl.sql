-- ============================================================
-- X WMS 库存预警/告警管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 预警规则/预警记录/预警处理/预警通知配置
-- ============================================================

-- 1. 预警规则表
CREATE TABLE wms_alert_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL, -- 规则编码
    rule_name       VARCHAR(128) NOT NULL, -- 规则名称
    alert_type      VARCHAR(32)  NOT NULL, -- 预警类型: STOCK库存/EXPIRY效期/SAFETY安全/ABNORMAL异常/THRESHOLD阈值
    warehouse_code  VARCHAR(64), -- 仓库
    owner_code      VARCHAR(64), -- 货主
    category_code   VARCHAR(64), -- 品类
    sku_code        VARCHAR(64), -- SKU
    condition_type  VARCHAR(32)  NOT NULL, -- 条件类型: LT小于/GT大于/EQ等于/LTE小于等于/GTE大于等于/BETWEEN区间
    threshold_value INT(18,4), -- 阈值
    threshold_value2 INT(18,4), -- 阈值2(区间)
    alert_level     VARCHAR(16)  NOT NULL, -- 预警级别: INFO信息/WARNING警告/CRITICAL严重
    check_frequency VARCHAR(32) DEFAULT 'DAILY', -- 检查频率: REALTIME实时/HOURLY每小时/DAILY每天
    notify_channels VARCHAR(256), -- 通知渠道: SMS,EMAIL,WEBHOOK,SYSTEM
    notify_users    VARCHAR(512), -- 通知用户
    auto_handle     VARCHAR(8)   DEFAULT 'N', -- 是否自动处理: Y/N
    handle_action   VARCHAR(64), -- 自动处理动作
    effective_date  DATE, -- 生效日期
    expire_date     DATE, -- 失效日期
    priority        SMALLINT     DEFAULT 5, -- 优先级
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_alert_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_alert_rule_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_alert_rule_type ON wms_alert_rule(alert_type);
CREATE INDEX idx_wms_alert_rule_wh ON wms_alert_rule(warehouse_code);

-- 2. 预警记录表
CREATE TABLE wms_alert_record (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    alert_id        VARCHAR(64)  NOT NULL, -- 预警ID
    rule_code       VARCHAR(64)  NOT NULL, -- 关联规则
    alert_type      VARCHAR(32)  NOT NULL, -- 预警类型
    alert_level     VARCHAR(16)  NOT NULL, -- 预警级别
    warehouse_code  VARCHAR(64),
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64),
    sku_name        VARCHAR(256),
    location_code   VARCHAR(64),
    batch_no        VARCHAR(64),
    current_value   INT(18,4), -- 当前值
    threshold_value INT(18,4), -- 阈值
    deviation_value INT(18,4), -- 偏差值
    deviation_rate  INT(10,4), -- 偏差率(%)
    alert_title     VARCHAR(256), -- 预警标题
    alert_content   TEXT, -- 预警内容
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING待处理/PROCESSING处理中/RESOLVED已解决/IGNORED已忽略
    trigger_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP, -- 触发时间
    resolve_time    TIMESTAMP, -- 解决时间
    resolved_by     VARCHAR(64), -- 处理人
    resolve_note    VARCHAR(512), -- 处理备注
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_alert_record PRIMARY KEY (id),
    CONSTRAINT uk_wms_alert_record_id UNIQUE (alert_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_alert_record_type ON wms_alert_record(alert_type);
CREATE INDEX idx_wms_alert_record_status ON wms_alert_record(status);
CREATE INDEX idx_wms_alert_record_sku ON wms_alert_record(sku_code);
CREATE INDEX idx_wms_alert_record_time ON wms_alert_record(trigger_time);

-- 3. 预警处理记录表
CREATE TABLE wms_alert_handle (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    handle_id       VARCHAR(64)  NOT NULL, -- 处理ID
    alert_id        VARCHAR(64)  NOT NULL, -- 关联预警
    handle_type     VARCHAR(32)  NOT NULL, -- 处理类型: MANUAL手动/AUTO自动/ESCALATE升级
    handle_action   VARCHAR(64), -- 处理动作
    handle_note     VARCHAR(512), -- 处理备注
    before_status   VARCHAR(32), -- 处理前状态
    after_status    VARCHAR(32), -- 处理后状态
    operator        VARCHAR(64), -- 操作人
    handle_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_alert_handle PRIMARY KEY (id),
    CONSTRAINT uk_wms_alert_handle_id UNIQUE (handle_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_alert_handle_alert ON wms_alert_handle(alert_id);


-- 注释
ALTER TABLE wms_alert_rule COMMENT='预警规则表';
ALTER TABLE wms_alert_record COMMENT='预警记录表';
ALTER TABLE wms_alert_handle COMMENT='预警处理记录表';
