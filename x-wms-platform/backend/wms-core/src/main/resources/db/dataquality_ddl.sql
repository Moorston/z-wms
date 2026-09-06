-- ============================================================
-- X WMS 库存数据质量管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 数据质量规则/数据质量检查/数据质量报告/数据质量整改
-- ============================================================

-- 1. 数据质量规则表
CREATE TABLE wms_dq_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_id         VARCHAR(64)  NOT NULL,
    rule_name       VARCHAR(128) NOT NULL,
    rule_code       VARCHAR(64)  NOT NULL,
    rule_type       VARCHAR(32)  NOT NULL,
    rule_category   VARCHAR(32),
    table_name      VARCHAR(128),
    field_name      VARCHAR(128),
    description     VARCHAR(512),
    rule_expression VARCHAR(1024),
    rule_config     TEXT,
    severity        VARCHAR(16) DEFAULT 'WARNING',
    threshold       INT(19,4),
    is_active       VARCHAR(8) DEFAULT 'Y',
    sort_order      INT DEFAULT 100,
    created_by      VARCHAR(64),
    updated_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_dq_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_dq_rule_id UNIQUE (rule_id),
    CONSTRAINT uk_wms_dq_rule_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_dq_rule_type ON wms_dq_rule(rule_type);
CREATE INDEX idx_wms_dq_rule_category ON wms_dq_rule(rule_category);
CREATE INDEX idx_wms_dq_rule_table ON wms_dq_rule(table_name);
CREATE INDEX idx_wms_dq_rule_active ON wms_dq_rule(is_active);

-- 2. 数据质量检查表
CREATE TABLE wms_dq_check (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    check_id        VARCHAR(64)  NOT NULL,
    check_name      VARCHAR(128) NOT NULL,
    rule_id         VARCHAR(64)  NOT NULL,
    rule_code       VARCHAR(64),
    rule_name       VARCHAR(128),
    warehouse_code  VARCHAR(64),
    owner_code      VARCHAR(64),
    check_type      VARCHAR(32) DEFAULT 'SCHEDULED',
    check_config    TEXT,
    total_count     BIGINT,
    pass_count      BIGINT,
    fail_count      BIGINT,
    pass_rate       INT(10,2),
    fail_rate       INT(10,2),
    check_result    TEXT,
    fail_details    TEXT,
    status          VARCHAR(32) DEFAULT 'PENDING',
    error_message   VARCHAR(1024),
    check_start_time TIMESTAMP,
    check_end_time  TIMESTAMP,
    duration_ms     BIGINT,
    operator        VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_dq_check PRIMARY KEY (id),
    CONSTRAINT uk_wms_dq_check_id UNIQUE (check_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_dq_check_rule ON wms_dq_check(rule_id);
CREATE INDEX idx_wms_dq_check_wh ON wms_dq_check(warehouse_code);
CREATE INDEX idx_wms_dq_check_status ON wms_dq_check(status);
CREATE INDEX idx_wms_dq_check_time ON wms_dq_check(check_start_time);

-- 3. 数据质量报告表
CREATE TABLE wms_dq_report (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    report_id       VARCHAR(64)  NOT NULL,
    report_name     VARCHAR(128) NOT NULL,
    report_type     VARCHAR(32)  NOT NULL,
    warehouse_code  VARCHAR(64),
    owner_code      VARCHAR(64),
    period_type     VARCHAR(32) DEFAULT 'DAILY',
    period_start    TIMESTAMP,
    period_end      TIMESTAMP,
    total_rules     INT,
    executed_rules  INT,
    pass_rules      INT,
    fail_rules      INT,
    overall_score   INT(10,2),
    overall_grade   VARCHAR(8),
    report_content  TEXT,
    report_summary  VARCHAR(1024),
    status          VARCHAR(32) DEFAULT 'DRAFT',
    generated_time  TIMESTAMP,
    operator        VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_dq_report PRIMARY KEY (id),
    CONSTRAINT uk_wms_dq_report_id UNIQUE (report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_dq_report_type ON wms_dq_report(report_type);
CREATE INDEX idx_wms_dq_report_wh ON wms_dq_report(warehouse_code);
CREATE INDEX idx_wms_dq_report_period ON wms_dq_report(period_start, period_end);
CREATE INDEX idx_wms_dq_report_status ON wms_dq_report(status);

-- 4. 数据质量整改表
CREATE TABLE wms_dq_issue (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    issue_id        VARCHAR(64)  NOT NULL,
    check_id        VARCHAR(64),
    rule_id         VARCHAR(64),
    rule_code       VARCHAR(64),
    rule_name       VARCHAR(128),
    warehouse_code  VARCHAR(64),
    owner_code      VARCHAR(64),
    table_name      VARCHAR(128),
    field_name      VARCHAR(128),
    issue_type      VARCHAR(32),
    issue_description VARCHAR(512),
    issue_data      TEXT,
    severity        VARCHAR(16) DEFAULT 'WARNING',
    status          VARCHAR(32) DEFAULT 'OPEN',
    priority        VARCHAR(16) DEFAULT 'NORMAL',
    assignee        VARCHAR(64),
    fix_plan        TEXT,
    fix_result      TEXT,
    fix_time        TIMESTAMP,
    verified_time   TIMESTAMP,
    verifier        VARCHAR(64),
    reopen_count    INT DEFAULT 0,
    due_time        TIMESTAMP,
    operator        VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_dq_issue PRIMARY KEY (id),
    CONSTRAINT uk_wms_dq_issue_id UNIQUE (issue_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_dq_issue_check ON wms_dq_issue(check_id);
CREATE INDEX idx_wms_dq_issue_rule ON wms_dq_issue(rule_id);
CREATE INDEX idx_wms_dq_issue_wh ON wms_dq_issue(warehouse_code);
CREATE INDEX idx_wms_dq_issue_status ON wms_dq_issue(status);
CREATE INDEX idx_wms_dq_issue_severity ON wms_dq_issue(severity);
CREATE INDEX idx_wms_dq_issue_assignee ON wms_dq_issue(assignee);


-- 注释
ALTER TABLE wms_dq_rule COMMENT='数据质量规则表';
ALTER TABLE wms_dq_check COMMENT='数据质量检查表';
ALTER TABLE wms_dq_report COMMENT='数据质量报告表';
ALTER TABLE wms_dq_issue COMMENT='数据质量整改表';
