-- ============================================================
-- X WMS 批次属性管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 批次属性定义/批次属性值/批次追踪规则/批次追踪日志
-- ============================================================

-- 1. 批次属性定义表
CREATE TABLE wms_batch_attribute (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    attr_code       VARCHAR(64)  NOT NULL,
    attr_name       VARCHAR(128) NOT NULL,
    attr_type       VARCHAR(32)  NOT NULL, -- STRING字符串/INT数字/DATE日期/ENUM枚举/BOOLEAN布尔
    attr_category   VARCHAR(32),  -- PRODUCTION生产/QUALITY质量/LOGISTICS物流/REGULATORY合规
    data_length     SMALLINT,
    precision_val   SMALLINT,
    enum_values     TEXT,          -- 枚举值(JSON数组)
    is_required     TINYINT(1)     DEFAULT 0,
    is_unique       TINYINT(1)     DEFAULT 0, -- 是否唯一(如批号)
    is_searchable   TINYINT(1)     DEFAULT 1, -- 是否可搜索
    default_value   VARCHAR(512),
    validation_rule VARCHAR(1024), -- 校验规则(正则/表达式)
    description     VARCHAR(512),
    sort_order      SMALLINT     DEFAULT 0,
    enabled         TINYINT(1)     DEFAULT 1,
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_batch_attr PRIMARY KEY (id),
    CONSTRAINT uk_wms_batch_attr_code UNIQUE (attr_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_batch_attr_cat ON wms_batch_attribute(attr_category);

-- 2. 批次属性值表
CREATE TABLE wms_batch_value (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    batch_no        VARCHAR(128) NOT NULL,
    sku_code        VARCHAR(64)  NOT NULL,
    warehouse_code  VARCHAR(64),
    attr_code       VARCHAR(64)  NOT NULL,
    attr_name       VARCHAR(128),
    attr_value      VARCHAR(1024),
    attr_value_date TIMESTAMP,    -- 日期类型值
    attr_value_num  INT(18,4), -- 数字类型值
    source_type     VARCHAR(32), -- MANUAL手动/SCAN扫码/IMPORT导入/SYSTEM系统
    source_ref      VARCHAR(128),-- 来源单号
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_batch_value PRIMARY KEY (id),
    CONSTRAINT uk_wms_batch_value UNIQUE (batch_no, sku_code, attr_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_batch_value_batch ON wms_batch_value(batch_no);
CREATE INDEX idx_wms_batch_value_sku ON wms_batch_value(sku_code);
CREATE INDEX idx_wms_batch_value_attr ON wms_batch_value(attr_code);

-- 3. 批次追踪规则表
CREATE TABLE wms_batch_trace_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL,
    rule_name       VARCHAR(128) NOT NULL,
    rule_type       VARCHAR(32)  NOT NULL, -- INBOUND入库/OUTBOUND出库/TRANSFER调拨/ADJUST调整/QC质检
    trigger_event   VARCHAR(128), -- 触发事件
    trace_fields    TEXT,          -- 追踪字段(JSON数组)
    trace_depth     TINYINT(1)     DEFAULT 1, -- 追踪深度(正向/反向)
    enabled         TINYINT(1)     DEFAULT 1,
    description     VARCHAR(512),
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_batch_trace_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_batch_trace_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 批次追踪日志表
CREATE TABLE wms_batch_trace_log (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    trace_no        VARCHAR(64)  NOT NULL,
    batch_no        VARCHAR(128) NOT NULL,
    sku_code        VARCHAR(64),
    operation_type  VARCHAR(32)  NOT NULL, -- INBOUND/OUTBOUND/TRANSFER/ADJUST/QC/SPLIT/MERGE
    operation_no    VARCHAR(64),  -- 操作单号
    from_location   VARCHAR(64),  -- 源库位
    to_location     VARCHAR(64),  -- 目标库位
    quantity        INT(18,4),
    operator        VARCHAR(64),
    operation_time  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    trace_data      TEXT,          -- 追踪数据(JSON)
    trace_id        VARCHAR(64),  -- 链路追踪ID
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_batch_trace_log PRIMARY KEY (id),
    CONSTRAINT uk_wms_batch_trace_no UNIQUE (trace_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_batch_trace_batch ON wms_batch_trace_log(batch_no);
CREATE INDEX idx_wms_batch_trace_sku ON wms_batch_trace_log(sku_code);
CREATE INDEX idx_wms_batch_trace_time ON wms_batch_trace_log(operation_time);


-- 注释
ALTER TABLE wms_batch_attribute COMMENT='批次属性定义表';
ALTER TABLE wms_batch_value COMMENT='批次属性值表';
ALTER TABLE wms_batch_trace_rule COMMENT='批次追踪规则表';
ALTER TABLE wms_batch_trace_log COMMENT='批次追踪日志表';
