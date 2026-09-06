-- ============================================================
-- X WMS 发运管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 发运单/发运明细/快递单/发运作业记录
-- ============================================================

-- 1. 发运单表
CREATE TABLE wms_shipment (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    shipment_no     VARCHAR(64)  NOT NULL,
    outbound_no     VARCHAR(64)  NOT NULL,
    wave_no         VARCHAR(64),
    warehouse_code  VARCHAR(64),
    owner_code_col  VARCHAR(64),
    carrier         VARCHAR(64), -- 承运商
    service_type    VARCHAR(32), -- 服务类型: STANDARD/EXPRESS/NEXT_DAY
    status          VARCHAR(32) DEFAULT 'CREATED', -- CREATED/PRINTED/PICKED_UP/IN_TRANSIT/DELIVERED/FAILED/CANCELLED
    total_qty       INT(18,4)  DEFAULT 0,
    shipped_qty     INT(18,4)  DEFAULT 0,
    package_count   INT    DEFAULT 0,
    total_weight    INT(18,4)  DEFAULT 0,
    total_volume    INT(18,4)  DEFAULT 0,
    shipping_fee    INT(18,4)  DEFAULT 0, -- 运费
    sender_name     VARCHAR(128),
    sender_phone    VARCHAR(64),
    sender_address  VARCHAR(512),
    receiver_name   VARCHAR(128),
    receiver_phone  VARCHAR(64),
    receiver_address VARCHAR(512),
    expected_delivery TIMESTAMP, -- 预计送达时间
    ship_time       TIMESTAMP, -- 发运时间
    delivery_time   TIMESTAMP, -- 送达时间
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_shipment PRIMARY KEY (id),
    CONSTRAINT uk_wms_shipment_no UNIQUE (shipment_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_shipment_outbound ON wms_shipment(outbound_no);
CREATE INDEX idx_wms_shipment_carrier ON wms_shipment(carrier);
CREATE INDEX idx_wms_shipment_status ON wms_shipment(status);

-- 2. 发运明细表
CREATE TABLE wms_shipment_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    shipment_no     VARCHAR(64)  NOT NULL,
    line_no         INT    NOT NULL,
    package_no      VARCHAR(64), -- 包裹号
    sku_code        VARCHAR(64),
    batch_no        VARCHAR(128),
    shipped_qty     INT(18,4)  DEFAULT 0,
    weight          INT(18,4),
    volume          INT(18,4),
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING/SHIPPED/DELIVERED
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_shipment_detail PRIMARY KEY (id),
    CONSTRAINT uk_wms_shipment_detail UNIQUE (shipment_no, line_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_shipment_detail_no ON wms_shipment_detail(shipment_no);

-- 3. 快递单表
CREATE TABLE wms_express_order (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    express_no      VARCHAR(64)  NOT NULL,
    tracking_no     VARCHAR(128) NOT NULL, -- 运单号
    shipment_no     VARCHAR(64),
    outbound_no     VARCHAR(64),
    carrier         VARCHAR(64),
    service_type    VARCHAR(32),
    status          VARCHAR(32) DEFAULT 'CREATED', -- CREATED/PRINTED/PICKED_UP/IN_TRANSIT/DELIVERED/FAILED/CANCELLED
    sender_name     VARCHAR(128),
    sender_phone    VARCHAR(64),
    sender_address  VARCHAR(512),
    receiver_name   VARCHAR(128),
    receiver_phone  VARCHAR(64),
    receiver_address VARCHAR(512),
    weight          INT(18,4),
    volume          INT(18,4),
    shipping_fee    INT(18,4),
    print_time      TIMESTAMP,
    pickup_time     TIMESTAMP,
    delivery_time   TIMESTAMP,
    error_msg       VARCHAR(512), -- 获取/打印失败原因
    retry_count     SMALLINT     DEFAULT 0, -- 重试次数
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_express PRIMARY KEY (id),
    CONSTRAINT uk_wms_express_no UNIQUE (express_no),
    CONSTRAINT uk_wms_express_tracking UNIQUE (tracking_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_express_shipment ON wms_express_order(shipment_no);
CREATE INDEX idx_wms_express_outbound ON wms_express_order(outbound_no);
CREATE INDEX idx_wms_express_carrier ON wms_express_order(carrier);
CREATE INDEX idx_wms_express_status ON wms_express_order(status);

-- 4. 发运作业记录表
CREATE TABLE wms_shipment_task (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    task_no         VARCHAR(64)  NOT NULL,
    shipment_no     VARCHAR(64)  NOT NULL,
    task_type       VARCHAR(32)  NOT NULL, -- GET_TRACKING获取运单号/PRINT打印/PICKUP揽收/DELIVER配送
    express_no      VARCHAR(64),
    carrier         VARCHAR(64),
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING/PROCESSING/SUCCESS/FAILED
    retry_count     SMALLINT     DEFAULT 0,
    error_msg       VARCHAR(512),
    operator        VARCHAR(64),
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_shipment_task PRIMARY KEY (id),
    CONSTRAINT uk_wms_shipment_task_no UNIQUE (task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_shipment_task_no ON wms_shipment_task(shipment_no);
CREATE INDEX idx_wms_shipment_task_status ON wms_shipment_task(status);


-- 注释
ALTER TABLE wms_shipment COMMENT='发运单表';
ALTER TABLE wms_shipment_detail COMMENT='发运明细表';
ALTER TABLE wms_express_order COMMENT='快递单表';
ALTER TABLE wms_shipment_task COMMENT='发运作业记录表';
