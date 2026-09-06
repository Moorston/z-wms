-- ============================================================
-- X WMS 库存效期管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 效期规则/效期批次/临期预警/过期处理记录
-- ============================================================

-- 1. 效期规则表
CREATE TABLE wms_expiry_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL, -- 规则编码
    rule_name       VARCHAR(128) NOT NULL, -- 规则名称
    warehouse_code  VARCHAR(64), -- 适用仓库
    owner_code      VARCHAR(64), -- 适用货主
    category_code   VARCHAR(64), -- 适用品类
    sku_code        VARCHAR(64), -- 适用SKU
    shelf_life_days INT, -- 保质期(天)
    warning_days_1  INT, -- 一级预警(天)
    warning_days_2  INT, -- 二级预警(天)
    warning_days_3  INT, -- 三级预警(天)
    expiry_action   VARCHAR(32), -- 过期处理: FREEZE冻结/RETURN退货/DESTROY销毁/SELL折价销售
    fefo_enable     VARCHAR(8)   DEFAULT 'Y', -- 是否启用FEFO
    auto_freeze     VARCHAR(8)   DEFAULT 'N', -- 过期自动冻结
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    priority        SMALLINT     DEFAULT 5,
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_expiry_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_expiry_rule_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 效期批次表
CREATE TABLE wms_expiry_batch (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    batch_no        VARCHAR(64)  NOT NULL, -- 批次号
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    production_date DATE, -- 生产日期
    expiry_date     DATE NOT NULL, -- 过期日期
    shelf_life_days INT, -- 保质期(天)
    remain_days     INT, -- 剩余天数
    total_qty       INT(18,4), -- 批次总数量
    available_qty   INT(18,4), -- 可用数量
    warning_level   VARCHAR(8), -- 预警级别: NORMAL/W1/W2/W3/EXPIRED
    expiry_status   VARCHAR(32), -- 效期状态: NORMAL正常/NEAR_EXPIRY临期/EXPIRED已过期
    inbound_no      VARCHAR(64), -- 入库单号
    supplier_code   VARCHAR(64), -- 供应商
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_expiry_batch PRIMARY KEY (id),
    CONSTRAINT uk_wms_expiry_batch UNIQUE (batch_no, warehouse_code, sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_expiry_batch_sku ON wms_expiry_batch(sku_code);
CREATE INDEX idx_wms_expiry_batch_expiry ON wms_expiry_batch(expiry_date);
CREATE INDEX idx_wms_expiry_batch_status ON wms_expiry_batch(expiry_status);
CREATE INDEX idx_wms_expiry_batch_warning ON wms_expiry_batch(warning_level);

-- 3. 临期预警表
CREATE TABLE wms_expiry_alert (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    alert_no        VARCHAR(64)  NOT NULL, -- 预警单号
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    batch_no        VARCHAR(64)  NOT NULL,
    expiry_date     DATE,
    remain_days     INT,
    warning_level   VARCHAR(8), -- W1/W2/W3
    alert_qty       INT(18,4), -- 预警数量
    alert_type      VARCHAR(32), -- NEAR_EXPIRY临期/EXPIRED过期
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING/PROCESSING/RESOLVED/IGNORED
    handle_action   VARCHAR(512), -- 处理措施
    handled_by      VARCHAR(64),
    handled_time    TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_expiry_alert PRIMARY KEY (id),
    CONSTRAINT uk_wms_expiry_alert_no UNIQUE (alert_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_expiry_alert_sku ON wms_expiry_alert(sku_code);
CREATE INDEX idx_wms_expiry_alert_batch ON wms_expiry_alert(batch_no);
CREATE INDEX idx_wms_expiry_alert_status ON wms_expiry_alert(status);

-- 4. 过期处理记录表
CREATE TABLE wms_expiry_handle_log (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    handle_no       VARCHAR(64)  NOT NULL, -- 处理单号
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64)  NOT NULL,
    batch_no        VARCHAR(64)  NOT NULL,
    expiry_date     DATE,
    handle_type     VARCHAR(32)  NOT NULL, -- FREEZE冻结/RETURN退货/DESTROY销毁/SELL折价销售/ADJUST调整
    handle_qty      INT(18,4)  NOT NULL, -- 处理数量
    handle_reason   VARCHAR(512), -- 处理原因
    ref_no          VARCHAR(64), -- 关联单号
    operator        VARCHAR(64),
    approver        VARCHAR(64),
    approve_time    TIMESTAMP,
    handle_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_expiry_handle PRIMARY KEY (id),
    CONSTRAINT uk_wms_expiry_handle_no UNIQUE (handle_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_expiry_handle_sku ON wms_expiry_handle_log(sku_code);
CREATE INDEX idx_wms_expiry_handle_batch ON wms_expiry_handle_log(batch_no);


-- 注释
ALTER TABLE wms_expiry_rule COMMENT='效期规则表';
ALTER TABLE wms_expiry_batch COMMENT='效期批次表';
ALTER TABLE wms_expiry_alert COMMENT='临期预警表';
ALTER TABLE wms_expiry_handle_log COMMENT='过期处理记录表';

-- 5. 收货人效期管理表（不同收货人对效期要求不同）
CREATE TABLE wms_customer_expiry (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    customer_code   VARCHAR(64)  NOT NULL, -- 收货人编码
    customer_name   VARCHAR(256), -- 收货人名称
    sku_code        VARCHAR(64), -- 商品编码（为空表示通用）
    sku_name        VARCHAR(256), -- 商品名称
    owner_code      VARCHAR(64), -- 货主编码
    warehouse_code  VARCHAR(64), -- 仓库编码
    outbound_expiry_days INT, -- 出库效期要求（出库日到失效日期有效天数）
    inbound_expiry_days INT, -- 入库效期要求（入库后安全存放最大天数）
    min_remaining_days INT, -- 最小剩余效期（天数）
    warning_days    INT, -- 预警提前天数
    insufficient_handle_type VARCHAR(32), -- 效期不足处理：REJECT拒绝/WARNING预警/ALLOW允许
    enabled         VARCHAR(1)   DEFAULT 'Y', -- 是否启用
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_customer_expiry PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_cust_expiry_customer ON wms_customer_expiry(customer_code);
CREATE INDEX idx_wms_cust_expiry_sku ON wms_customer_expiry(sku_code);

-- 效期规则表补充字段（入库效期/出库效期）
ALTER TABLE wms_expiry_rule ADD (inbound_expiry_days INT);
ALTER TABLE wms_expiry_rule ADD (outbound_expiry_days INT);
ALTER TABLE wms_expiry_rule ADD (inbound_warning_days INT);
ALTER TABLE wms_expiry_rule ADD (outbound_warning_days INT);
ALTER TABLE wms_expiry_rule ADD (enabled VARCHAR(1) DEFAULT 'Y');

ALTER TABLE wms_customer_expiry COMMENT='收货人效期管理表';

-- 新增序列
