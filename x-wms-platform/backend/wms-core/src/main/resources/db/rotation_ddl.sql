-- ============================================================
-- X WMS 库存周转管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 周转规则/周转批次队列/效期预警/周转分析
-- ============================================================

-- 1. 周转规则表
CREATE TABLE wms_rotation_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL, -- 规则编码
    rule_name       VARCHAR(128) NOT NULL, -- 规则名称
    sku_code        VARCHAR(64), -- 适用SKU(空表示所有)
    category_code   VARCHAR(64), -- 适用品类
    owner_code      VARCHAR(64), -- 适用货主
    rotation_type   VARCHAR(32)  NOT NULL, -- FIFO先进先出/FEFO先效期先出/LIFO后进先出/FIFO_FEFO混合
    sort_field      VARCHAR(32), -- 排序字段: PRODUCTION_DATE/EXPIRE_DATE/RECEIVE_DATE/BATCH_NO
    sort_order      VARCHAR(8)   DEFAULT 'ASC', -- ASC升序/DESC降序
    priority        SMALLINT     DEFAULT 5, -- 优先级
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_rotation_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_rotation_rule_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_rotation_sku ON wms_rotation_rule(sku_code);
CREATE INDEX idx_wms_rotation_category ON wms_rotation_rule(category_code);

-- 2. 周转批次队列表（按SKU+库位维护批次顺序）
CREATE TABLE wms_rotation_queue (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    queue_id        VARCHAR(64)  NOT NULL, -- 队列ID(SKU+库位+货主)
    sku_code        VARCHAR(64)  NOT NULL,
    batch_no        VARCHAR(128) NOT NULL,
    location_code   VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    quantity        INT(18,4)  NOT NULL, -- 可用数量
    production_date TIMESTAMP, -- 生产日期
    expire_date     TIMESTAMP, -- 有效期
    receive_date    TIMESTAMP, -- 收货日期
    sort_value      VARCHAR(128), -- 排序值(根据周转规则生成)
    sort_order      INT, -- 排序序号
    status          VARCHAR(32) DEFAULT 'ACTIVE', -- ACTIVE/EXPIRED/FROZEN
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_rotation_queue PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_rot_queue_sku ON wms_rotation_queue(sku_code);
CREATE INDEX idx_wms_rot_queue_location ON wms_rotation_queue(location_code);
CREATE INDEX idx_wms_rot_queue_sort ON wms_rotation_queue(queue_id, sort_order);
CREATE INDEX idx_wms_rot_queue_expire ON wms_rotation_queue(expire_date);

-- 3. 效期预警表
CREATE TABLE wms_expiry_alert (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    alert_no        VARCHAR(64)  NOT NULL, -- 预警单号
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    batch_no        VARCHAR(128) NOT NULL,
    location_code   VARCHAR(64),
    owner_code      VARCHAR(64),
    quantity        INT(18,4)  NOT NULL,
    production_date TIMESTAMP,
    expire_date     TIMESTAMP,
    days_to_expire  SMALLINT, -- 距过期天数
    alert_level     VARCHAR(32), -- WARNING警告/CRITICAL严重/EXPIRED已过期
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING待处理/PROCESSING处理中/RESOLVED已解决/IGNORED已忽略
    handle_action   VARCHAR(32), -- 处理方式: PROMOTION促销/RETURN退货/SCRAP报废/TRANSFER调拨
    handled_by      VARCHAR(64),
    handled_time    TIMESTAMP,
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_expiry_alert PRIMARY KEY (id),
    CONSTRAINT uk_wms_expiry_alert_no UNIQUE (alert_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_expiry_sku ON wms_expiry_alert(sku_code);
CREATE INDEX idx_wms_expiry_status ON wms_expiry_alert(status);
CREATE INDEX idx_wms_expiry_level ON wms_expiry_alert(alert_level);

-- 4. 周转分析表（按日/周/月统计）
CREATE TABLE wms_rotation_analysis (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    analysis_date   TIMESTAMP     NOT NULL, -- 统计日期
    period_type     VARCHAR(16)  NOT NULL, -- DAY/WEEK/MONTH
    sku_code        VARCHAR(64),
    category_code   VARCHAR(64),
    warehouse_code  VARCHAR(64),
    owner_code      VARCHAR(64),
    opening_qty     INT(18,4)  DEFAULT 0, -- 期初数量
    inbound_qty     INT(18,4)  DEFAULT 0, -- 入库数量
    outbound_qty    INT(18,4)  DEFAULT 0, -- 出库数量
    closing_qty     INT(18,4)  DEFAULT 0, -- 期末数量
    turnover_rate   INT(18,4), -- 周转率
    turnover_days   INT(18,4), -- 周转天数
    avg_inventory   INT(18,4), -- 平均库存
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_rotation_analysis PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_rot_analysis_date ON wms_rotation_analysis(analysis_date);
CREATE INDEX idx_wms_rot_analysis_sku ON wms_rotation_analysis(sku_code);


-- 注释
ALTER TABLE wms_rotation_rule COMMENT='周转规则表';
ALTER TABLE wms_rotation_queue COMMENT='周转批次队列表';
ALTER TABLE wms_expiry_alert COMMENT='效期预警表';
ALTER TABLE wms_rotation_analysis COMMENT='周转分析表';
