-- ============================================================
-- X WMS 序列号管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 序列号档案/序列号规则/序列号绑定/序列号流转记录
-- ============================================================

-- 1. 序列号规则表
CREATE TABLE wms_serial_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL, -- 规则编码
    rule_name       VARCHAR(128) NOT NULL, -- 规则名称
    sku_code        VARCHAR(64), -- 适用SKU(空表示所有)
    category_code   VARCHAR(64), -- 适用品类
    prefix          VARCHAR(32), -- 前缀
    suffix          VARCHAR(32), -- 后缀
    seq_length      SMALLINT     DEFAULT 10, -- 序列长度
    start_seq       BIGINT    DEFAULT 1, -- 起始序号
    current_seq     BIGINT    DEFAULT 1, -- 当前序号
    check_digit     TINYINT(1)     DEFAULT 0, -- 是否校验位
    date_format     VARCHAR(32), -- 日期格式(如yyyyMMdd)
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_serial_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_serial_rule_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 序列号档案表
CREATE TABLE wms_serial (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    serial_no       VARCHAR(128) NOT NULL, -- 序列号
    sku_code        VARCHAR(64)  NOT NULL,
    batch_no        VARCHAR(128), -- 批号
    status          VARCHAR(32) DEFAULT 'CREATED', -- CREATED已生成/INBOUND已入库/IN_STOCK在库/ALLOCATED已分配/PICKED已拣货/PACKED已打包/SHIPPED已发运/RETURNED已退货/SCRAPPED已报废
    warehouse_code  VARCHAR(64),
    location_code   VARCHAR(64), -- 当前库位
    container_no    VARCHAR(64), -- 所在容器
    owner_code      VARCHAR(64),
    inbound_no      VARCHAR(64), -- 入库单号
    outbound_no     VARCHAR(64), -- 出库单号
    production_date TIMESTAMP, -- 生产日期
    expire_date     TIMESTAMP, -- 有效期
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_serial PRIMARY KEY (id),
    CONSTRAINT uk_wms_serial_no UNIQUE (serial_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_serial_sku ON wms_serial(sku_code);
CREATE INDEX idx_wms_serial_batch ON wms_serial(batch_no);
CREATE INDEX idx_wms_serial_status ON wms_serial(status);
CREATE INDEX idx_wms_serial_location ON wms_serial(location_code);

-- 3. 序列号绑定表（序列号与单据/容器的绑定关系）
CREATE TABLE wms_serial_bind (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    bind_no         VARCHAR(64)  NOT NULL,
    serial_no       VARCHAR(128) NOT NULL,
    ref_type        VARCHAR(32)  NOT NULL, -- INBOUND/OUTBOUND/TRANSFER/MOVE/STORAGE/RETURN
    ref_no          VARCHAR(64)  NOT NULL,
    ref_line_no     INT, -- 明细行号
    container_no    VARCHAR(64),
    status          VARCHAR(32) DEFAULT 'BOUND', -- BOUND/RELEASED
    bind_time       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    release_time    TIMESTAMP,
    operator        VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_serial_bind PRIMARY KEY (id),
    CONSTRAINT uk_wms_serial_bind_no UNIQUE (bind_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_serial_bind_serial ON wms_serial_bind(serial_no);
CREATE INDEX idx_wms_serial_bind_ref ON wms_serial_bind(ref_type, ref_no);

-- 4. 序列号流转记录表
CREATE TABLE wms_serial_trace (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    trace_no        VARCHAR(64)  NOT NULL,
    serial_no       VARCHAR(128) NOT NULL,
    action_type     VARCHAR(32)  NOT NULL, -- GENERATE/INBOUND/PUTAWAY/PICK/PACK/SHIP/RETURN/SCRAP/MOVE
    from_status     VARCHAR(32),
    to_status       VARCHAR(32),
    from_location   VARCHAR(64),
    to_location     VARCHAR(64),
    ref_type        VARCHAR(32),
    ref_no          VARCHAR(64),
    operator        VARCHAR(64),
    action_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_serial_trace PRIMARY KEY (id),
    CONSTRAINT uk_wms_serial_trace_no UNIQUE (trace_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_serial_trace_serial ON wms_serial_trace(serial_no);
CREATE INDEX idx_wms_serial_trace_time ON wms_serial_trace(action_time);


-- 注释
ALTER TABLE wms_serial_rule COMMENT='序列号规则表';
ALTER TABLE wms_serial COMMENT='序列号档案表';
ALTER TABLE wms_serial_bind COMMENT='序列号绑定表';
ALTER TABLE wms_serial_trace COMMENT='序列号流转记录表';
