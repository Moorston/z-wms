-- ============================================================
-- X WMS 库存查询/报表管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 库存快照/库存日报/库存月报/库存分析指标
-- ============================================================

-- 1. 库存快照表
CREATE TABLE wms_inventory_snapshot (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    snapshot_id     VARCHAR(64)  NOT NULL, -- 快照ID
    snapshot_date   DATE          NOT NULL, -- 快照日期
    snapshot_type   VARCHAR(32)  NOT NULL, -- 快照类型: DAILY日报/WEEKLY周报/MONTHLY月报
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    location_code   VARCHAR(64),
    batch_no        VARCHAR(64),
    total_qty       INT(18,4) DEFAULT 0, -- 总库存
    available_qty   INT(18,4) DEFAULT 0, -- 可用库存
    allocated_qty   INT(18,4) DEFAULT 0, -- 预占库存
    picking_qty     INT(18,4) DEFAULT 0, -- 拣货中
    sorting_qty     INT(18,4) DEFAULT 0, -- 分拣中
    shipping_qty    INT(18,4) DEFAULT 0, -- 待发运
    frozen_qty      INT(18,4) DEFAULT 0, -- 冻结库存
    unit_cost       INT(18,4), -- 单位成本
    total_cost      INT(18,4), -- 总成本
    inbound_qty     INT(18,4) DEFAULT 0, -- 当日入库
    outbound_qty    INT(18,4) DEFAULT 0, -- 当日出库
    adjust_qty      INT(18,4) DEFAULT 0, -- 当日调整
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_inv_snapshot PRIMARY KEY (id),
    CONSTRAINT uk_wms_inv_snapshot UNIQUE (snapshot_id, warehouse_code, sku_code, location_code, batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inv_snapshot_date ON wms_inventory_snapshot(snapshot_date);
CREATE INDEX idx_wms_inv_snapshot_sku ON wms_inventory_snapshot(sku_code);
CREATE INDEX idx_wms_inv_snapshot_wh ON wms_inventory_snapshot(warehouse_code);

-- 2. 库存日报表
CREATE TABLE wms_inventory_daily (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    report_date     DATE          NOT NULL, -- 报表日期
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    begin_qty       INT(18,4) DEFAULT 0, -- 期初库存
    inbound_qty     INT(18,4) DEFAULT 0, -- 入库数量
    outbound_qty    INT(18,4) DEFAULT 0, -- 出库数量
    adjust_in_qty   INT(18,4) DEFAULT 0, -- 调整入库
    adjust_out_qty  INT(18,4) DEFAULT 0, -- 调整出库
    end_qty         INT(18,4) DEFAULT 0, -- 期末库存
    begin_cost      INT(18,4) DEFAULT 0, -- 期初成本
    inbound_cost    INT(18,4) DEFAULT 0, -- 入库成本
    outbound_cost   INT(18,4) DEFAULT 0, -- 出库成本
    end_cost        INT(18,4) DEFAULT 0, -- 期末成本
    avg_cost        INT(18,4), -- 平均成本
    turnover_days   INT(10,2), -- 周转天数
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_inv_daily PRIMARY KEY (id),
    CONSTRAINT uk_wms_inv_daily UNIQUE (report_date, warehouse_code, owner_code, sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inv_daily_date ON wms_inventory_daily(report_date);
CREATE INDEX idx_wms_inv_daily_sku ON wms_inventory_daily(sku_code);

-- 3. 库存月报表
CREATE TABLE wms_inventory_monthly (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    report_month    VARCHAR(7)   NOT NULL, -- 报表月份 YYYY-MM
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    begin_qty       INT(18,4) DEFAULT 0, -- 月初库存
    inbound_qty     INT(18,4) DEFAULT 0, -- 月入库
    outbound_qty    INT(18,4) DEFAULT 0, -- 月出库
    end_qty         INT(18,4) DEFAULT 0, -- 月末库存
    begin_cost      INT(18,4) DEFAULT 0,
    inbound_cost    INT(18,4) DEFAULT 0,
    outbound_cost   INT(18,4) DEFAULT 0,
    end_cost        INT(18,4) DEFAULT 0,
    avg_cost        INT(18,4),
    turnover_rate   INT(10,2), -- 周转率
    turnover_days   INT(10,2), -- 周转天数
    stockout_days   INT, -- 缺货天数
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_inv_monthly PRIMARY KEY (id),
    CONSTRAINT uk_wms_inv_monthly UNIQUE (report_month, warehouse_code, owner_code, sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inv_monthly_month ON wms_inventory_monthly(report_month);
CREATE INDEX idx_wms_inv_monthly_sku ON wms_inventory_monthly(sku_code);

-- 4. 库存分析指标表
CREATE TABLE wms_inventory_metric (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    metric_id       VARCHAR(64)  NOT NULL, -- 指标ID
    metric_date     DATE          NOT NULL, -- 指标日期
    metric_type     VARCHAR(32)  NOT NULL, -- 指标类型: TURNOVER周转/ACCURACY准确率/OBSOLETE呆滞/ABC分类
    warehouse_code  VARCHAR(64),
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64),
    category_code   VARCHAR(64),
    metric_value    INT(18,4), -- 指标值
    metric_value2   INT(18,4), -- 指标值2
    metric_value3   INT(18,4), -- 指标值3
    metric_text     VARCHAR(512), -- 指标文本
    rank_no         INT, -- 排名
    level_code      VARCHAR(32), -- 等级: A/B/C
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_inv_metric PRIMARY KEY (id),
    CONSTRAINT uk_wms_inv_metric UNIQUE (metric_id, metric_date, metric_type, warehouse_code, sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inv_metric_date ON wms_inventory_metric(metric_date);
CREATE INDEX idx_wms_inv_metric_type ON wms_inventory_metric(metric_type);


-- 注释
ALTER TABLE wms_inventory_snapshot COMMENT='库存快照表';
ALTER TABLE wms_inventory_daily COMMENT='库存日报表';
ALTER TABLE wms_inventory_monthly COMMENT='库存月报表';
ALTER TABLE wms_inventory_metric COMMENT='库存分析指标表';
