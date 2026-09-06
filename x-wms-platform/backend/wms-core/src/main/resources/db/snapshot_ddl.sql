-- ============================================================
-- X WMS 库存快照管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 库存快照主表/库存快照明细表/快照对比表/快照恢复记录表
-- ============================================================

-- 1. 库存快照主表
CREATE TABLE wms_inventory_snapshot (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    snapshot_no     VARCHAR(64)  NOT NULL, -- 快照号
    snapshot_type   VARCHAR(32)  NOT NULL, -- 快照类型: DAILY日结/MONTHLY月结/MANUAL手动/REALTIME实时
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    snapshot_date   DATE          NOT NULL, -- 快照日期
    snapshot_time   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP, -- 快照时间
    total_sku_count INT, -- SKU总数
    total_qty       INT(18,4), -- 总数量
    total_cost      INT(18,4), -- 总成本
    total_value     INT(18,4), -- 总价值
    status          VARCHAR(32) DEFAULT 'COMPLETED', -- PROCESSING处理中/COMPLETED已完成/FAILED失败
    fail_reason     VARCHAR(512),
    operator        VARCHAR(64),
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_inv_snapshot PRIMARY KEY (id),
    CONSTRAINT uk_wms_inv_snapshot_no UNIQUE (snapshot_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inv_snapshot_date ON wms_inventory_snapshot(snapshot_date);
CREATE INDEX idx_wms_inv_snapshot_warehouse ON wms_inventory_snapshot(warehouse_code);
CREATE INDEX idx_wms_inv_snapshot_type ON wms_inventory_snapshot(snapshot_type);

-- 2. 库存快照明细表
CREATE TABLE wms_inventory_snapshot_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    snapshot_no     VARCHAR(64)  NOT NULL, -- 快照号
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    category_code   VARCHAR(64),
    location_code   VARCHAR(64),
    batch_no        VARCHAR(64),
    serial_no       VARCHAR(64),
    container_no    VARCHAR(64),
    quantity        INT(18,4)  NOT NULL, -- 快照数量
    available_qty   INT(18,4), -- 可用数量
    allocated_qty   INT(18,4), -- 预占数量
    picking_qty     INT(18,4), -- 拣货中数量
    frozen_qty      INT(18,4), -- 冻结数量
    unit_cost       INT(18,4), -- 单位成本
    total_cost      INT(18,4), -- 总成本
    abc_class       VARCHAR(8), -- ABC分类
    inventory_status VARCHAR(32), -- 库存状态
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_inv_snapshot_dtl PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inv_snapshot_dtl_no ON wms_inventory_snapshot_detail(snapshot_no);
CREATE INDEX idx_wms_inv_snapshot_dtl_sku ON wms_inventory_snapshot_detail(sku_code);
CREATE INDEX idx_wms_inv_snapshot_dtl_loc ON wms_inventory_snapshot_detail(location_code);

-- 3. 快照对比表
CREATE TABLE wms_snapshot_compare (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    compare_no      VARCHAR(64)  NOT NULL, -- 对比号
    snapshot_no_1   VARCHAR(64)  NOT NULL, -- 快照1
    snapshot_no_2   VARCHAR(64)  NOT NULL, -- 快照2
    warehouse_code  VARCHAR(64),
    compare_type    VARCHAR(32), -- 对比类型: QUANTITY数量/COST成本/STATUS状态
    total_diff_sku  INT, -- 差异SKU数
    total_diff_qty  INT(18,4), -- 差异总数量
    total_diff_cost INT(18,4), -- 差异总成本
    status          VARCHAR(32) DEFAULT 'COMPLETED',
    operator        VARCHAR(64),
    compare_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_snapshot_compare PRIMARY KEY (id),
    CONSTRAINT uk_wms_snapshot_compare_no UNIQUE (compare_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_snapshot_compare_no1 ON wms_snapshot_compare(snapshot_no_1);
CREATE INDEX idx_wms_snapshot_compare_no2 ON wms_snapshot_compare(snapshot_no_2);

-- 4. 快照对比明细表
CREATE TABLE wms_snapshot_compare_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    compare_no      VARCHAR(64)  NOT NULL,
    sku_code        VARCHAR(64)  NOT NULL,
    location_code   VARCHAR(64),
    batch_no        VARCHAR(64),
    qty_1           INT(18,4), -- 快照1数量
    qty_2           INT(18,4), -- 快照2数量
    diff_qty        INT(18,4), -- 数量差异
    cost_1          INT(18,4), -- 快照1成本
    cost_2          INT(18,4), -- 快照2成本
    diff_cost       INT(18,4), -- 成本差异
    diff_type       VARCHAR(32), -- 差异类型: INCREASE增加/DECREASE减少/NEW新增/REMOVED移除
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_snapshot_compare_dtl PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_snapshot_compare_dtl_no ON wms_snapshot_compare_detail(compare_no);
CREATE INDEX idx_wms_snapshot_compare_dtl_sku ON wms_snapshot_compare_detail(sku_code);

-- 5. 快照恢复记录表
CREATE TABLE wms_snapshot_restore_log (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    restore_no      VARCHAR(64)  NOT NULL, -- 恢复号
    snapshot_no     VARCHAR(64)  NOT NULL, -- 恢复的快照号
    warehouse_code  VARCHAR(64),
    restore_type    VARCHAR(32), -- 恢复类型: FULL全量/PARTIAL部分
    restore_scope   VARCHAR(512), -- 恢复范围(SKU列表)
    restore_sku_count INT, -- 恢复SKU数
    restore_qty     INT(18,4), -- 恢复总数量
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING/PROCESSING/COMPLETED/FAILED
    operator        VARCHAR(64),
    restore_time    TIMESTAMP,
    fail_reason     VARCHAR(512),
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_snapshot_restore PRIMARY KEY (id),
    CONSTRAINT uk_wms_snapshot_restore_no UNIQUE (restore_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_snapshot_restore_no ON wms_snapshot_restore_log(snapshot_no);


-- 注释
ALTER TABLE wms_inventory_snapshot COMMENT='库存快照主表';
ALTER TABLE wms_inventory_snapshot_detail COMMENT='库存快照明细表';
ALTER TABLE wms_snapshot_compare COMMENT='快照对比表';
ALTER TABLE wms_snapshot_compare_detail COMMENT='快照对比明细表';
ALTER TABLE wms_snapshot_restore_log COMMENT='快照恢复记录表';
