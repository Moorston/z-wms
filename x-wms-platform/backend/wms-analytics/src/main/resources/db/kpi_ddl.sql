-- ============================================================
-- X WMS KPI报表体系模块 DDL
-- 数据库: MySQL 8.x
-- 包含: KPI指标定义/KPI日数据/KPI目标/KPI报表
-- ============================================================

-- 1. KPI指标定义表
CREATE TABLE wms_kpi_define (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    kpi_code        VARCHAR(64)  NOT NULL,
    kpi_name        VARCHAR(128) NOT NULL,
    kpi_category    VARCHAR(32)  NOT NULL, -- EFFICIENCY效率/QUALITY质量/COST成本/INVENTORY库存/SAFETY安全/SERVICE服务
    kpi_type        VARCHAR(16),  -- RATIO比率/COUNT数量/TIME时间/AMOUNT金额
    unit            VARCHAR(16),  -- %/件/小时/元/次
    description     VARCHAR(512),
    calc_formula    VARCHAR(1024), -- 计算公式
    data_source     VARCHAR(256),  -- 数据来源(表/接口)
    target_direction VARCHAR(8),   -- UP越高越好/DOWN越低越好
    sort_order      SMALLINT       DEFAULT 0,
    enabled         TINYINT(1)       DEFAULT 1,
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_kpi_define PRIMARY KEY (id),
    CONSTRAINT uk_wms_kpi_code UNIQUE (kpi_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_kpi_category ON wms_kpi_define(kpi_category);

-- 2. KPI日数据表
CREATE TABLE wms_kpi_daily (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    kpi_code        VARCHAR(64)  NOT NULL,
    kpi_name        VARCHAR(128),
    kpi_category    VARCHAR(32),
    stat_date       DATE          NOT NULL,
    warehouse_code  VARCHAR(64),
    department      VARCHAR(64),
    employee_id     VARCHAR(64),
    actual_value    INT(18,4)  DEFAULT 0,
    target_value    INT(18,4),
    target_rate     INT(10,4), -- 达成率
    compare_value   INT(18,4), -- 同比值
    compare_rate    INT(10,4), -- 同比增长率
    ring_value      INT(18,4),  -- 环比值
    ring_rate       INT(10,4),  -- 环比增长率
    data_detail     TEXT,          -- 明细数据(JSON)
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_kpi_daily PRIMARY KEY (id),
    CONSTRAINT uk_wms_kpi_daily UNIQUE (kpi_code, stat_date, warehouse_code, department, employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_kpi_daily_date ON wms_kpi_daily(stat_date);
CREATE INDEX idx_wms_kpi_daily_wh ON wms_kpi_daily(warehouse_code);

-- 3. KPI目标表
CREATE TABLE wms_kpi_target (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    target_no       VARCHAR(64)  NOT NULL,
    kpi_code        VARCHAR(64)  NOT NULL,
    kpi_name        VARCHAR(128),
    target_period   VARCHAR(16)  NOT NULL, -- DAILY日/MONTHLY月/QUARTERLY季/ANNUAL年
    period_value    VARCHAR(16),  -- 期间值: 202608/2026Q3/2026
    warehouse_code  VARCHAR(64),
    department      VARCHAR(64),
    target_value    INT(18,4)  NOT NULL,
    challenge_value INT(18,4), -- 挑战值
    baseline_value  INT(18,4), -- 基准值
    status          VARCHAR(16)  DEFAULT 'ACTIVE', -- ACTIVE生效/EXPIRED过期
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_kpi_target PRIMARY KEY (id),
    CONSTRAINT uk_wms_kpi_target_no UNIQUE (target_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_kpi_target_kpi ON wms_kpi_target(kpi_code);
CREATE INDEX idx_wms_kpi_target_period ON wms_kpi_target(target_period, period_value);

-- 4. KPI报表配置表
CREATE TABLE wms_kpi_report (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    report_code     VARCHAR(64)  NOT NULL,
    report_name     VARCHAR(128) NOT NULL,
    report_type     VARCHAR(32),  -- DAILY日报/WEEKLY周报/MONTHLY月报/QUARTERLY季报/ANNUAL年报/CUSTOM自定义
    kpi_codes       TEXT,          -- 包含的KPI编码(JSON数组)
    dimensions      TEXT,          -- 分析维度(JSON数组): 仓库/部门/人员/时间
    chart_type      VARCHAR(32),  -- TABLE表格/LINE折线/BAR柱状/PIE饼图
    schedule_cron   VARCHAR(64),  -- 定时生成cron
    last_generate_time TIMESTAMP,
    enabled         TINYINT(1)       DEFAULT 1,
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_kpi_report PRIMARY KEY (id),
    CONSTRAINT uk_wms_kpi_report_code UNIQUE (report_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 注释
ALTER TABLE wms_kpi_define COMMENT='KPI指标定义表';
ALTER TABLE wms_kpi_daily COMMENT='KPI日数据表';
ALTER TABLE wms_kpi_target COMMENT='KPI目标表';
ALTER TABLE wms_kpi_report COMMENT='KPI报表配置表';
