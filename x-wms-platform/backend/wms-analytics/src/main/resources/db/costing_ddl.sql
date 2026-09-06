-- ============================================================
-- X WMS 库存成本核算管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 成本核算/成本调整/成本分摊/成本报表
-- ============================================================

-- 1. 成本核算表
CREATE TABLE wms_cost_calculate (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    calculate_id    VARCHAR(64)  NOT NULL, -- 核算ID
    calculate_name  VARCHAR(128), -- 核算名称
    calculate_type  VARCHAR(32)  NOT NULL, -- 核算类型: MONTH_END月末/QUARTER_END季末/YEAR_END年末/REAL_TIME实时
    warehouse_code  VARCHAR(64), -- 仓库
    owner_code      VARCHAR(64), -- 货主
    period_start    DATE NOT NULL, -- 期间开始
    period_end      DATE NOT NULL, -- 期间结束
    costing_method  VARCHAR(32)  NOT NULL, -- 成本方法: FIFO先进先出/LIFO后进先出/WEIGHTED_AVG加权平均/MOVING_AVG移动平均/STANDARD标准成本/SPECIFIC个别计价
    total_sku_count INT   DEFAULT 0, -- SKU总数
    calculated_count INT  DEFAULT 0, -- 已核算数
    failed_count    INT   DEFAULT 0, -- 失败数
    total_quantity  INT(19,4), -- 总数量
    total_amount    INT(19,4), -- 总金额
    average_cost    INT(19,4), -- 平均成本
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING待核算/CALCULATING核算中/COMPLETED已完成/FAILED失败/CANCELLED已取消
    error_message   TEXT, -- 错误信息
    operator        VARCHAR(64), -- 操作人
    start_time      TIMESTAMP, -- 开始时间
    end_time        TIMESTAMP, -- 结束时间
    duration_ms     BIGINT, -- 耗时(毫秒)
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_cost_calculate PRIMARY KEY (id),
    CONSTRAINT uk_wms_cost_calculate_id UNIQUE (calculate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_cost_calculate_wh ON wms_cost_calculate(warehouse_code);
CREATE INDEX idx_wms_cost_calculate_period ON wms_cost_calculate(period_start, period_end);
CREATE INDEX idx_wms_cost_calculate_status ON wms_cost_calculate(status);

-- 2. 成本调整表
CREATE TABLE wms_cost_adjust (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    adjust_id       VARCHAR(64)  NOT NULL, -- 调整ID
    adjust_type     VARCHAR(32)  NOT NULL, -- 调整类型: PRICE_ADJUST价格调整/QUANTITY_ADJUST数量调整/DIFFERENCE_ADJUST差异调整/WRITE_DOWN减值/WRITE_OFF核销
    warehouse_code  VARCHAR(64), -- 仓库
    owner_code      VARCHAR(64), -- 货主
    sku_code        VARCHAR(64)  NOT NULL, -- SKU
    batch_no        VARCHAR(64), -- 批次
    location_code   VARCHAR(64), -- 库位
    before_quantity INT(19,4), -- 调整前数量
    after_quantity  INT(19,4), -- 调整后数量
    quantity_diff   INT(19,4), -- 数量差异
    before_cost     INT(19,4), -- 调整前成本
    after_cost      INT(19,4), -- 调整后成本
    cost_diff       INT(19,4), -- 成本差异
    before_amount   INT(19,4), -- 调整前金额
    after_amount    INT(19,4), -- 调整后金额
    amount_diff     INT(19,4), -- 金额差异
    adjust_reason   VARCHAR(512), -- 调整原因
    adjust_basis    VARCHAR(512), -- 调整依据
    related_biz_type VARCHAR(32), -- 关联业务类型
    related_biz_no  VARCHAR(64), -- 关联业务单号
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING待审核/APPROVED已审核/REJECTED已驳回/CANCELLED已取消
    approver        VARCHAR(64), -- 审核人
    approve_time    TIMESTAMP, -- 审核时间
    approve_opinion VARCHAR(512), -- 审核意见
    operator        VARCHAR(64), -- 操作人
    adjust_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP, -- 调整时间
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_cost_adjust PRIMARY KEY (id),
    CONSTRAINT uk_wms_cost_adjust_id UNIQUE (adjust_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_cost_adjust_wh ON wms_cost_adjust(warehouse_code);
CREATE INDEX idx_wms_cost_adjust_sku ON wms_cost_adjust(sku_code);
CREATE INDEX idx_wms_cost_adjust_status ON wms_cost_adjust(status);

-- 3. 成本分摊表
CREATE TABLE wms_cost_allocation (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    allocation_id   VARCHAR(64)  NOT NULL, -- 分摊ID
    allocation_name VARCHAR(128), -- 分摊名称
    allocation_type VARCHAR(32)  NOT NULL, -- 分摊类型: FREIGHT运费/STORAGE仓储费/HANDLING装卸费/INSURANCE保险费/OTHER其他
    warehouse_code  VARCHAR(64), -- 仓库
    owner_code      VARCHAR(64), -- 货主
    period_start    DATE, -- 期间开始
    period_end      DATE, -- 期间结束
    total_amount    INT(19,4) NOT NULL, -- 分摊总金额
    allocation_method VARCHAR(32) NOT NULL, -- 分摊方法: BY_QUANTITY按数量/BY_AMOUNT按金额/BY_WEIGHT按重量/BY_VOLUME按体积/BY_SKU按SKU平均
    allocated_count INT   DEFAULT 0, -- 已分摊数
    allocated_amount INT(19,4) DEFAULT 0, -- 已分摊金额
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING待分摊/ALLOCATING分摊中/COMPLETED已完成/FAILED失败/CANCELLED已取消
    error_message   TEXT, -- 错误信息
    operator        VARCHAR(64), -- 操作人
    start_time      TIMESTAMP, -- 开始时间
    end_time        TIMESTAMP, -- 结束时间
    duration_ms     BIGINT, -- 耗时(毫秒)
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_cost_allocation PRIMARY KEY (id),
    CONSTRAINT uk_wms_cost_allocation_id UNIQUE (allocation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_cost_allocation_wh ON wms_cost_allocation(warehouse_code);
CREATE INDEX idx_wms_cost_allocation_status ON wms_cost_allocation(status);

-- 4. 成本明细表
CREATE TABLE wms_cost_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    detail_id       VARCHAR(64)  NOT NULL, -- 明细ID
    calculate_id    VARCHAR(64), -- 关联核算ID
    adjust_id       VARCHAR(64), -- 关联调整ID
    allocation_id   VARCHAR(64), -- 关联分摊ID
    warehouse_code  VARCHAR(64), -- 仓库
    owner_code      VARCHAR(64), -- 货主
    sku_code        VARCHAR(64)  NOT NULL, -- SKU
    batch_no        VARCHAR(64), -- 批次
    location_code   VARCHAR(64), -- 库位
    biz_type        VARCHAR(32)  NOT NULL, -- 业务类型: INBOUND入库/OUTBOUND出库/TRANSFER调拨/ADJUST调整/ALLOCATION分摊
    biz_no          VARCHAR(64), -- 业务单号
    quantity        INT(19,4), -- 数量
    unit_cost       INT(19,4), -- 单位成本
    total_cost      INT(19,4), -- 总成本
    before_quantity INT(19,4), -- 变动前数量
    after_quantity  INT(19,4), -- 变动后数量
    before_unit_cost INT(19,4), -- 变动前单位成本
    after_unit_cost INT(19,4), -- 变动后单位成本
    before_total_cost INT(19,4), -- 变动前总成本
    after_total_cost INT(19,4), -- 变动后总成本
    cost_diff       INT(19,4), -- 成本差异
    costing_method  VARCHAR(32), -- 成本方法
    period_date     DATE, -- 期间日期
    operator        VARCHAR(64), -- 操作人
    operation_time  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP, -- 操作时间
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_cost_detail PRIMARY KEY (id),
    CONSTRAINT uk_wms_cost_detail_id UNIQUE (detail_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_cost_detail_calc ON wms_cost_detail(calculate_id);
CREATE INDEX idx_wms_cost_detail_sku ON wms_cost_detail(sku_code);
CREATE INDEX idx_wms_cost_detail_biz ON wms_cost_detail(biz_type, biz_no);
CREATE INDEX idx_wms_cost_detail_wh ON wms_cost_detail(warehouse_code);
CREATE INDEX idx_wms_cost_detail_period ON wms_cost_detail(period_date);


-- 注释
ALTER TABLE wms_cost_calculate COMMENT='成本核算表';
ALTER TABLE wms_cost_adjust COMMENT='成本调整表';
ALTER TABLE wms_cost_allocation COMMENT='成本分摊表';
ALTER TABLE wms_cost_detail COMMENT='成本明细表';

-- ============================================================
-- 以下为原cost模块的表（已合并到costing模块）
-- ============================================================

-- 5. 库存成本规则表
CREATE TABLE wms_cost_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL,
    rule_name       VARCHAR(128) NOT NULL,
    sku_code        VARCHAR(64),
    category_code   VARCHAR(64),
    owner_code      VARCHAR(64),
    cost_method     VARCHAR(32)  NOT NULL,
    standard_price  INT(18,4),
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    priority        SMALLINT     DEFAULT 5,
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_cost_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_cost_rule_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. 成本流水表
CREATE TABLE wms_cost_log (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    log_no          VARCHAR(64)  NOT NULL,
    ref_type        VARCHAR(32)  NOT NULL,
    ref_no          VARCHAR(64),
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64)  NOT NULL,
    batch_no        VARCHAR(128),
    location_code   VARCHAR(64),
    quantity        INT(18,4),
    before_unit_cost INT(18,4),
    after_unit_cost  INT(18,4),
    before_total_cost INT(18,4),
    after_total_cost INT(18,4),
    change_amount   INT(18,4),
    operator        VARCHAR(64),
    action_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_cost_log PRIMARY KEY (id),
    CONSTRAINT uk_wms_cost_log_no UNIQUE (log_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_cost_log_sku ON wms_cost_log(sku_code);
CREATE INDEX idx_wms_cost_log_time ON wms_cost_log(action_time);

