-- ============================================================
-- X WMS 费收计费模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 计费规则/费用项目/账单/账单明细/结算记录
-- ============================================================

-- 1. 计费规则表
CREATE TABLE wms_billing_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL,
    rule_name       VARCHAR(128) NOT NULL,
    fee_type        VARCHAR(32)  NOT NULL, -- STORAGE仓储费/INBOUND入库费/OUTBOUND出库费/HANDLING操作费/VAS增值费/OTHER其他
    charge_mode     VARCHAR(32)  NOT NULL, -- PER_UNIT按件/PER_ORDER按单/PER_WEIGHT按重量/PER_VOLUME按体积/PER_DAY按天/PER_MONTH按月/FLAT固定/STEP阶梯
    unit_price      DECIMAL(14,4)  DEFAULT 0, -- 单价
    currency        VARCHAR(8)   DEFAULT 'CNY',
    min_charge      DECIMAL(14,4),            -- 最低收费
    max_charge      DECIMAL(14,4),            -- 最高收费
    free_qty        DECIMAL(14,4),            -- 免计费数量
    step_config     TEXT,                    -- 阶梯计费配置(JSON)
    effective_date  DATE,                    -- 生效日期
    expire_date     DATE,                    -- 失效日期
    owner_code      VARCHAR(64),            -- 适用货主(空=全部)
    customer_code   VARCHAR(64),            -- 适用客户(空=全部)
    warehouse_code  VARCHAR(64),
    priority        SMALLINT     DEFAULT 5,
    status          VARCHAR(16)  DEFAULT 'ENABLED',
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    deleted         TINYINT(1)     DEFAULT 0,
    CONSTRAINT pk_wms_billing_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_billing_rule_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_bill_rule_type ON wms_billing_rule(fee_type);
CREATE INDEX idx_wms_bill_rule_owner ON wms_billing_rule(owner_code);

-- 2. 费用项目表(流水)
CREATE TABLE wms_billing_fee_item (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    fee_no          VARCHAR(64)  NOT NULL,
    fee_type        VARCHAR(32)  NOT NULL,
    rule_id         BIGINT,
    rule_code       VARCHAR(64),
    owner_code      VARCHAR(64),
    customer_code   VARCHAR(64),
    warehouse_code  VARCHAR(64),
    ref_type        VARCHAR(32),            -- 关联业务类型: INBOUND/OUTBOUND/STORAGE/VAS
    ref_no          VARCHAR(64),            -- 关联业务单号
    ref_item_id     BIGINT,
    sku             VARCHAR(64),
    product_name    VARCHAR(256),
    batch_no        VARCHAR(64),
    quantity        DECIMAL(14,4),            -- 计费数量
    weight          DECIMAL(14,4),            -- 计费重量
    volume          DECIMAL(14,4),            -- 计费体积
    days            INT,              -- 计费天数(仓储费)
    charge_mode     VARCHAR(32),
    unit_price      DECIMAL(14,4),
    amount          DECIMAL(14,4)  NOT NULL,  -- 费用金额
    currency        VARCHAR(8)   DEFAULT 'CNY',
    fee_date        DATE,                    -- 费用发生日期
    fee_period      VARCHAR(16),            -- 费用期间: 202608
    status          VARCHAR(16)  DEFAULT 'PENDING', -- PENDING待计费/BILLED已入账/SETTLED已结算/WRITTEN_OFF已核销
    bill_id         BIGINT,              -- 关联账单ID
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_billing_fee_item PRIMARY KEY (id),
    CONSTRAINT uk_wms_billing_fee_no UNIQUE (fee_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_fee_type ON wms_billing_fee_item(fee_type);
CREATE INDEX idx_wms_fee_owner ON wms_billing_fee_item(owner_code);
CREATE INDEX idx_wms_fee_period ON wms_billing_fee_item(fee_period);
CREATE INDEX idx_wms_fee_status ON wms_billing_fee_item(status);
CREATE INDEX idx_wms_fee_ref ON wms_billing_fee_item(ref_no);

-- 3. 账单表
CREATE TABLE wms_billing_bill (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    bill_no         VARCHAR(64)  NOT NULL,
    bill_type       VARCHAR(16)  NOT NULL, -- MONTHLY月结/DAILY日结/ADHOC临时
    owner_code      VARCHAR(64)  NOT NULL,
    customer_code   VARCHAR(64),
    warehouse_code  VARCHAR(64),
    bill_period     VARCHAR(16)  NOT NULL, -- 账单期间: 202608
    start_date      DATE,
    end_date        DATE,
    total_amount    DECIMAL(14,4)  DEFAULT 0, -- 总金额
    paid_amount     DECIMAL(14,4)  DEFAULT 0, -- 已付金额
    unpaid_amount   DECIMAL(14,4)  DEFAULT 0, -- 未付金额
    discount_amount DECIMAL(14,4)  DEFAULT 0, -- 优惠金额
    final_amount    DECIMAL(14,4)  DEFAULT 0, -- 最终金额
    currency        VARCHAR(8)   DEFAULT 'CNY',
    status          VARCHAR(32)  NOT NULL, -- DRAFT草稿/PENDING待确认/CONFIRMED已确认/INVOICED已开票/PARTIAL_PAID部分付款/PAID已付款/OVERDUE逾期/CANCELLED取消
    issue_date      DATE,                    -- 账单日期
    due_date        DATE,                    -- 到期日期
    paid_date       DATE,
    invoice_no      VARCHAR(64),
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    deleted         TINYINT(1)     DEFAULT 0,
    CONSTRAINT pk_wms_billing_bill PRIMARY KEY (id),
    CONSTRAINT uk_wms_billing_bill_no UNIQUE (bill_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_bill_owner ON wms_billing_bill(owner_code);
CREATE INDEX idx_wms_bill_period ON wms_billing_bill(bill_period);
CREATE INDEX idx_wms_bill_status ON wms_billing_bill(status);

-- 4. 账单明细表
CREATE TABLE wms_billing_bill_item (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    bill_id         BIGINT    NOT NULL,
    bill_no         VARCHAR(64)  NOT NULL,
    fee_item_id     BIGINT,
    fee_type        VARCHAR(32)  NOT NULL,
    fee_name        VARCHAR(128),
    quantity        DECIMAL(14,4),
    unit_price      DECIMAL(14,4),
    amount          DECIMAL(14,4)  NOT NULL,
    currency        VARCHAR(8)   DEFAULT 'CNY',
    fee_date        DATE,
    ref_no          VARCHAR(64),
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_billing_bill_item PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_bill_item_bill ON wms_billing_bill_item(bill_id);
CREATE INDEX idx_wms_bill_item_type ON wms_billing_bill_item(fee_type);

-- 5. 结算记录表
CREATE TABLE wms_billing_settlement (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    settlement_no   VARCHAR(64)  NOT NULL,
    bill_id         BIGINT    NOT NULL,
    bill_no         VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    settlement_type VARCHAR(16),  -- FULL全额/PARTIAL部分/ADVANCE预付
    amount          DECIMAL(14,4)  NOT NULL,
    currency        VARCHAR(8)   DEFAULT 'CNY',
    payment_method  VARCHAR(32),  -- BANK_TRANSFER银行转账/CREDIT_CREDIT信用额度/CASH现金/OTHER
    payment_ref     VARCHAR(128), -- 支付凭证号
    settlement_date DATE,
    status          VARCHAR(16)  DEFAULT 'COMPLETED', -- PENDING/COMPLETED/FAILED
    operator        VARCHAR(64),
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_billing_settlement PRIMARY KEY (id),
    CONSTRAINT uk_wms_settlement_no UNIQUE (settlement_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_settle_bill ON wms_billing_settlement(bill_id);


-- 注释
ALTER TABLE wms_billing_rule COMMENT='计费规则表';
ALTER TABLE wms_billing_fee_item COMMENT='费用项目流水表';
ALTER TABLE wms_billing_bill COMMENT='账单表';
ALTER TABLE wms_billing_bill_item COMMENT='账单明细表';
ALTER TABLE wms_billing_settlement COMMENT='结算记录表';
