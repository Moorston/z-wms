-- ============================================================
-- X WMS 出库管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 出库单/出库明细/拣货记录/发运记录
-- ============================================================

-- 1. 出库单表
CREATE TABLE wms_outbound_order (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    outbound_no     VARCHAR(64)  NOT NULL,
    outbound_type   VARCHAR(32)  NOT NULL, -- SALE销售/TRANSFER调拨/RETURN退货/VAS增值/SAMPLE样品
    ref_no          VARCHAR(64), -- 来源单号(销售单号/调拨单号等)
    customer_code   VARCHAR(64),
    owner_code_col  VARCHAR(64),
    warehouse_code  VARCHAR(64),
    wave_no         VARCHAR(64), -- 波次号
    total_qty       INT(18,4),
    allocated_qty   INT(18,4) DEFAULT 0, -- 已分配数量
    picked_qty      INT(18,4) DEFAULT 0, -- 已拣货数量
    packed_qty      INT(18,4) DEFAULT 0, -- 已打包数量
    shipped_qty     INT(18,4) DEFAULT 0, -- 已发运数量
    status          VARCHAR(32) DEFAULT 'CREATED', -- CREATED/ALLOCATING/ALLOCATED/PICKING/PICKED/PACKING/PACKED/SHIPPING/SHIPPED/CANCELLED
    carrier         VARCHAR(128), -- 承运商
    tracking_no     VARCHAR(128), -- 运单号
    delivery_method VARCHAR(32), -- 配送方式
    expected_ship_time TIMESTAMP, -- 预计发运时间
    actual_ship_time  TIMESTAMP, -- 实际发运时间
    shipping_address VARCHAR(512),
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_outbound PRIMARY KEY (id),
    CONSTRAINT uk_wms_outbound_no UNIQUE (outbound_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_outbound_customer ON wms_outbound_order(customer_code);
CREATE INDEX idx_wms_outbound_status ON wms_outbound_order(status);
CREATE INDEX idx_wms_outbound_wave ON wms_outbound_order(wave_no);
CREATE INDEX idx_wms_outbound_owner ON wms_outbound_order(owner_code_col);

-- 2. 出库明细表
CREATE TABLE wms_outbound_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    detail_no       VARCHAR(64)  NOT NULL,
    outbound_no     VARCHAR(64)  NOT NULL,
    line_no         SMALLINT     NOT NULL,
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    batch_no        VARCHAR(128), -- 批号
    warehouse_code  VARCHAR(64), -- 分配仓库编码（allocate 时回填）
    location_code   VARCHAR(64), -- 分配库位编码（allocate 时回填）
    expected_qty    INT(18,4),
    allocated_qty   INT(18,4) DEFAULT 0,
    picked_qty      INT(18,4) DEFAULT 0,
    packed_qty      INT(18,4) DEFAULT 0,
    shipped_qty     INT(18,4) DEFAULT 0,
    unit            VARCHAR(32),
    package_code    VARCHAR(64),
    status          VARCHAR(32) DEFAULT 'CREATED', -- CREATED/ALLOCATED/PICKING/PICKED/PACKED/SHIPPED
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_outbound_detail PRIMARY KEY (id),
    CONSTRAINT uk_wms_outbound_detail UNIQUE (outbound_no, line_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_outbound_detail_outbound ON wms_outbound_detail(outbound_no);
CREATE INDEX idx_wms_outbound_detail_sku ON wms_outbound_detail(sku_code);
CREATE INDEX idx_wms_outbound_detail_batch ON wms_outbound_detail(batch_no);

-- 已部署环境追加列（全新部署由上方 CREATE TABLE 已含此两列）
ALTER TABLE wms_outbound_detail ADD (warehouse_code VARCHAR(64), location_code VARCHAR(64));

-- 3. 拣货记录表
CREATE TABLE wms_pick_record (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    record_no       VARCHAR(64)  NOT NULL,
    outbound_no     VARCHAR(64)  NOT NULL,
    detail_no       VARCHAR(64),
    wave_no         VARCHAR(64),
    sku_code        VARCHAR(64),
    batch_no        VARCHAR(128),
    from_location   VARCHAR(64) NOT NULL, -- 拣货库位
    pick_qty        INT(18,4) NOT NULL,
    pick_type       VARCHAR(32), -- NORMAL正常/SHORTAGE少拣/DAMAGE损坏
    difference_qty  INT(18,4) DEFAULT 0,
    operator        VARCHAR(64),
    pick_time       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_pick PRIMARY KEY (id),
    CONSTRAINT uk_wms_pick_no UNIQUE (record_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_pick_outbound ON wms_pick_record(outbound_no);
CREATE INDEX idx_wms_pick_wave ON wms_pick_record(wave_no);
CREATE INDEX idx_wms_pick_sku ON wms_pick_record(sku_code);
CREATE INDEX idx_wms_pick_time ON wms_pick_record(pick_time);

-- 4. 发运记录表
CREATE TABLE wms_ship_record (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    record_no       VARCHAR(64)  NOT NULL,
    outbound_no     VARCHAR(64)  NOT NULL,
    carrier         VARCHAR(128),
    tracking_no     VARCHAR(128),
    ship_qty        INT(18,4) NOT NULL,
    package_count   SMALLINT, -- 包裹数
    weight          INT(18,4), -- 重量
    volume          INT(18,4), -- 体积
    ship_status     VARCHAR(32), -- CREATED/SHIPPED/DELIVERED/FAILED
    operator        VARCHAR(64),
    ship_time       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_ship PRIMARY KEY (id),
    CONSTRAINT uk_wms_ship_no UNIQUE (record_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_ship_outbound ON wms_ship_record(outbound_no);
CREATE INDEX idx_wms_ship_tracking ON wms_ship_record(tracking_no);
CREATE INDEX idx_wms_ship_time ON wms_ship_record(ship_time);


-- 注释
ALTER TABLE wms_outbound_order COMMENT='出库单表';
ALTER TABLE wms_outbound_detail COMMENT='出库明细表';
ALTER TABLE wms_pick_record COMMENT='拣货记录表';
ALTER TABLE wms_ship_record COMMENT='发运记录表';
