-- ============================================================
-- X WMS 库存ABC分类管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: ABC分类规则/ABC分类结果/XYZ分类结果/分类历史
-- ============================================================

-- 1. ABC分类规则表
CREATE TABLE wms_abc_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL, -- 规则编码
    rule_name       VARCHAR(128) NOT NULL, -- 规则名称
    warehouse_code  VARCHAR(64), -- 适用仓库
    owner_code      VARCHAR(64), -- 适用货主
    category_code   VARCHAR(64), -- 适用品类
    classify_by     VARCHAR(32)  NOT NULL, -- 分类依据: SALES_AMOUNT销售额/SALES_QTY销售数量/PROFIT利润/TURNOVER周转率
    period_type     VARCHAR(16)  DEFAULT 'MONTH', -- 统计周期: DAY/WEEK/MONTH/QUARTER/YEAR
    a_ratio         INT(5,2)   DEFAULT 80, -- A类占比(%)
    b_ratio         INT(5,2)   DEFAULT 15, -- B类占比(%)
    c_ratio         INT(5,2)   DEFAULT 5,  -- C类占比(%)
    a_count_ratio   INT(5,2)   DEFAULT 20, -- A类品种占比(%)
    b_count_ratio   INT(5,2)   DEFAULT 30, -- B类品种占比(%)
    c_count_ratio   INT(5,2)   DEFAULT 50, -- C类品种占比(%)
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_abc_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_abc_rule_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. ABC分类结果表
CREATE TABLE wms_abc_classification (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    classify_id     VARCHAR(64)  NOT NULL, -- 分类批次ID
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    category_code   VARCHAR(64),
    abc_class       VARCHAR(8)   NOT NULL, -- A/B/C
    xyz_class       VARCHAR(8), -- X/Y/Z
    sales_amount    INT(18,4), -- 销售额
    sales_qty       INT(18,4), -- 销售数量
    profit          INT(18,4), -- 利润
    turnover_rate   INT(18,4), -- 周转率
    turnover_days   INT(18,4), -- 周转天数
    avg_inventory   INT(18,4), -- 平均库存
    cumulative_ratio INT(5,2), -- 累计占比(%)
    rank_no         INT, -- 排名
    period_start    TIMESTAMP, -- 统计开始时间
    period_end      TIMESTAMP, -- 统计结束时间
    classify_time   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP, -- 分类时间
    status          VARCHAR(32) DEFAULT 'CURRENT', -- CURRENT当前/HISTORY历史
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_abc_class PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_abc_warehouse ON wms_abc_classification(warehouse_code);
CREATE INDEX idx_wms_abc_sku ON wms_abc_classification(sku_code);
CREATE INDEX idx_wms_abc_class ON wms_abc_classification(abc_class);
CREATE INDEX idx_wms_abc_status ON wms_abc_classification(status);

-- 3. XYZ分类结果表（需求波动性分类）
CREATE TABLE wms_xyz_classification (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    classify_id     VARCHAR(64)  NOT NULL,
    warehouse_code  VARCHAR(64)  NOT NULL,
    sku_code        VARCHAR(64)  NOT NULL,
    xyz_class       VARCHAR(8)   NOT NULL, -- X/Y/Z
    avg_demand      INT(18,4), -- 平均需求
    std_dev         INT(18,4), -- 标准差
    cv              INT(18,4), -- 变异系数(CV=标准差/均值)
    max_demand      INT(18,4), -- 最大需求
    min_demand      INT(18,4), -- 最小需求
    demand_cv_threshold_x INT(5,2) DEFAULT 0.2, -- X类CV阈值
    demand_cv_threshold_y INT(5,2) DEFAULT 0.5, -- Y类CV阈值
    period_start    TIMESTAMP,
    period_end      TIMESTAMP,
    classify_time   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    status          VARCHAR(32) DEFAULT 'CURRENT',
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_xyz_class PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_xyz_sku ON wms_xyz_classification(sku_code);
CREATE INDEX idx_wms_xyz_class ON wms_xyz_classification(xyz_class);

-- 4. 分类历史表
CREATE TABLE wms_abc_history (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    history_id      VARCHAR(64)  NOT NULL,
    warehouse_code  VARCHAR(64)  NOT NULL,
    sku_code        VARCHAR(64)  NOT NULL,
    old_abc_class   VARCHAR(8), -- 原ABC分类
    new_abc_class   VARCHAR(8), -- 新ABC分类
    old_xyz_class   VARCHAR(8), -- 原XYZ分类
    new_xyz_class   VARCHAR(8), -- 新XYZ分类
    change_type     VARCHAR(32), -- UPGRADE升级/DOWNGRADE降级/UNCHANGED未变
    change_reason   VARCHAR(512),
    operator        VARCHAR(64),
    change_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_abc_history PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_abc_hist_sku ON wms_abc_history(sku_code);
CREATE INDEX idx_wms_abc_hist_time ON wms_abc_history(change_time);


-- 注释
ALTER TABLE wms_abc_rule COMMENT='ABC分类规则表';
ALTER TABLE wms_abc_classification COMMENT='ABC分类结果表';
ALTER TABLE wms_xyz_classification COMMENT='XYZ分类结果表';
ALTER TABLE wms_abc_history COMMENT='分类历史表';
