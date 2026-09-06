-- ============================================================
-- X WMS 入库管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: ASN单/入库单/入库明细/收货记录
-- ============================================================

-- 1. ASN(到货通知)单表
CREATE TABLE wms_asn (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    asn_no          VARCHAR(64)  NOT NULL,
    asn_type        VARCHAR(32)  NOT NULL, -- PURCHASE采购/RETURN退货/TRANSFER调拨/PRODUCTION生产
    ref_no          VARCHAR(64), -- 来源单号(PO号/退货单号等)
    supplier_code   VARCHAR(64),
    owner_code_col  VARCHAR(64),
    warehouse_code  VARCHAR(64),
    expected_date   TIMESTAMP, -- 预计到货日期
    actual_date     TIMESTAMP, -- 实际到货日期
    total_qty       INT(18,4),
    received_qty    INT(18,4) DEFAULT 0,
    status          VARCHAR(32) DEFAULT 'CREATED', -- CREATED/SHIPPED/RECEIVING/RECEIVED/CANCELLED
    carrier         VARCHAR(128), -- 承运商
    tracking_no     VARCHAR(128), -- 运单号
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_asn PRIMARY KEY (id),
    CONSTRAINT uk_wms_asn_no UNIQUE (asn_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_asn_supplier ON wms_asn(supplier_code);
CREATE INDEX idx_wms_asn_status ON wms_asn(status);
CREATE INDEX idx_wms_asn_owner ON wms_asn(owner_code_col);

-- 2. 入库单表
CREATE TABLE wms_inbound_order (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    inbound_no      VARCHAR(64)  NOT NULL,
    inbound_type    VARCHAR(32)  NOT NULL, -- PURCHASE/RETURN/TRANSFER/PRODUCTION/BLIND盲收
    asn_no          VARCHAR(64),
    ref_no          VARCHAR(64),
    supplier_code   VARCHAR(64),
    owner_code_col  VARCHAR(64),
    warehouse_code  VARCHAR(64),
    area_code       VARCHAR(64), -- 收货库区
    total_qty       INT(18,4),
    received_qty    INT(18,4) DEFAULT 0,
    putaway_qty     INT(18,4) DEFAULT 0,
    status          VARCHAR(32) DEFAULT 'CREATED', -- CREATED/RECEIVING/RECEIVED/QCING/PUTAWAYING/DONE/CANCELLED
    receive_time    TIMESTAMP,
    putaway_time    TIMESTAMP,
    done_time       TIMESTAMP,
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_inbound PRIMARY KEY (id),
    CONSTRAINT uk_wms_inbound_no UNIQUE (inbound_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inbound_asn ON wms_inbound_order(asn_no);
CREATE INDEX idx_wms_inbound_status ON wms_inbound_order(status);
CREATE INDEX idx_wms_inbound_owner ON wms_inbound_order(owner_code_col);

-- 3. 入库明细表
CREATE TABLE wms_inbound_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    detail_no       VARCHAR(64)  NOT NULL,
    inbound_no      VARCHAR(64)  NOT NULL,
    line_no         SMALLINT     NOT NULL,
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    batch_no        VARCHAR(128), -- 批号
    expected_qty    INT(18,4),
    received_qty    INT(18,4) DEFAULT 0,
    putaway_qty     INT(18,4) DEFAULT 0,
    unit            VARCHAR(32),
    package_code    VARCHAR(64),
    status          VARCHAR(32) DEFAULT 'CREATED', -- CREATED/RECEIVED/PUTAWAYING/DONE
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_inbound_detail PRIMARY KEY (id),
    CONSTRAINT uk_wms_inbound_detail UNIQUE (inbound_no, line_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inbound_detail_inbound ON wms_inbound_detail(inbound_no);
CREATE INDEX idx_wms_inbound_detail_sku ON wms_inbound_detail(sku_code);
CREATE INDEX idx_wms_inbound_detail_batch ON wms_inbound_detail(batch_no);

-- 4. 收货记录表
CREATE TABLE wms_receive_record (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    record_no       VARCHAR(64)  NOT NULL,
    inbound_no      VARCHAR(64)  NOT NULL,
    detail_no       VARCHAR(64),
    sku_code        VARCHAR(64),
    batch_no        VARCHAR(128),
    receive_qty     INT(18,4) NOT NULL,
    receive_location VARCHAR(64), -- 收货库位
    receive_type    VARCHAR(32), -- NORMAL正常/OVERAGE多收/SHORTAGE少收/DAMAGE损坏
    difference_qty  INT(18,4) DEFAULT 0, -- 差异数量
    operator        VARCHAR(64),
    receive_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_receive PRIMARY KEY (id),
    CONSTRAINT uk_wms_receive_no UNIQUE (record_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_receive_inbound ON wms_receive_record(inbound_no);
CREATE INDEX idx_wms_receive_sku ON wms_receive_record(sku_code);
CREATE INDEX idx_wms_receive_time ON wms_receive_record(receive_time);


-- 注释
ALTER TABLE wms_asn COMMENT='ASN到货通知单表';
ALTER TABLE wms_inbound_order COMMENT='入库单表';
ALTER TABLE wms_inbound_detail COMMENT='入库明细表';
ALTER TABLE wms_receive_record COMMENT='收货记录表';
