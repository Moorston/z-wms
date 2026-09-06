-- ============================================================
-- X WMS 库存分配策略管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 分配规则/分配明细/分配日志/分配策略配置
-- ============================================================

-- 1. 分配规则表
CREATE TABLE wms_allocation_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL, -- 规则编码
    rule_name       VARCHAR(128) NOT NULL, -- 规则名称
    warehouse_code  VARCHAR(64), -- 适用仓库
    owner_code      VARCHAR(64), -- 适用货主
    category_code   VARCHAR(64), -- 适用品类
    sku_code        VARCHAR(64), -- 适用SKU
    order_type      VARCHAR(32), -- 适用订单类型
    priority        SMALLINT     DEFAULT 5, -- 优先级
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_alloc_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_alloc_rule_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 分配策略配置表
CREATE TABLE wms_allocation_strategy (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL, -- 关联规则编码
    strategy_type   VARCHAR(32)  NOT NULL, -- 策略类型: FIFO/FEFO/LIFO/LEFO/MANUAL
    sort_field      VARCHAR(64), -- 排序字段
    sort_order      VARCHAR(8), -- 排序方向: ASC/DESC
    location_priority VARCHAR(32), -- 库位优先级: GOLDEN黄金区/NORMAL普通区/REMOTE偏远区
    batch_priority  VARCHAR(32), -- 批次优先级: EARLIEST最早/LATEST最晚/NEAREST最近
    allow_split     VARCHAR(8)   DEFAULT 'Y', -- 是否允许拆单
    min_alloc_qty   INT(18,4), -- 最小分配数量
    max_alloc_qty   INT(18,4), -- 最大分配数量
    reserve_hours   INT, -- 预占保留时间(小时)
    auto_release    VARCHAR(8)   DEFAULT 'Y', -- 超时自动释放
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_alloc_strategy PRIMARY KEY (id),
    CONSTRAINT uk_wms_alloc_strategy_rule UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 分配明细表
CREATE TABLE wms_allocation_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    allocation_no   VARCHAR(64)  NOT NULL, -- 分配单号
    order_no        VARCHAR(64)  NOT NULL, -- 订单号
    order_line      INT, -- 订单行号
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    location_code   VARCHAR(64),
    batch_no        VARCHAR(64),
    serial_no       VARCHAR(64),
    container_no    VARCHAR(64),
    allocated_qty   INT(18,4)  NOT NULL, -- 已分配数量
    picked_qty      INT(18,4) DEFAULT 0, -- 已拣货数量
    remain_qty      INT(18,4), -- 剩余分配数量
    unit_cost       INT(18,4), -- 单位成本
    total_cost      INT(18,4), -- 总成本
    status          VARCHAR(32) DEFAULT 'ALLOCATED', -- ALLOCATED已分配/PICKING拣货中/PICKED已拣货/RELEASED已释放/CANCELLED已取消
    allocate_time   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    release_time    TIMESTAMP,
    operator        VARCHAR(64),
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_alloc_detail PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_alloc_detail_order ON wms_allocation_detail(order_no);
CREATE INDEX idx_wms_alloc_detail_sku ON wms_allocation_detail(sku_code);
CREATE INDEX idx_wms_alloc_detail_loc ON wms_allocation_detail(location_code);
CREATE INDEX idx_wms_alloc_detail_status ON wms_allocation_detail(status);

-- 4. 分配日志表
CREATE TABLE wms_allocation_log (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    log_id          VARCHAR(64)  NOT NULL, -- 日志ID
    allocation_no   VARCHAR(64), -- 分配单号
    order_no        VARCHAR(64), -- 订单号
    sku_code        VARCHAR(64),
    action_type     VARCHAR(32)  NOT NULL, -- 操作类型: ALLOCATE分配/REALLOCATE重新分配/RELEASE释放/PICK拣货/CANCEL取消
    before_qty      INT(18,4), -- 操作前数量
    after_qty       INT(18,4), -- 操作后数量
    change_qty      INT(18,4), -- 变动数量
    location_code   VARCHAR(64),
    batch_no        VARCHAR(64),
    reason          VARCHAR(512), -- 原因
    operator        VARCHAR(64),
    operate_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_alloc_log PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_alloc_log_alloc ON wms_allocation_log(allocation_no);
CREATE INDEX idx_wms_alloc_log_order ON wms_allocation_log(order_no);
CREATE INDEX idx_wms_alloc_log_sku ON wms_allocation_log(sku_code);


-- 注释
ALTER TABLE wms_allocation_rule COMMENT='分配规则表';
ALTER TABLE wms_allocation_strategy COMMENT='分配策略配置表';
ALTER TABLE wms_allocation_detail COMMENT='分配明细表';
ALTER TABLE wms_allocation_log COMMENT='分配日志表';
