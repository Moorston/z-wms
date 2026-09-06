-- ============================================================
-- X WMS 调拨管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 调拨单/调拨明细/调拨在途库存/调拨作业记录
-- ============================================================

-- 1. 调拨单表
CREATE TABLE wms_transfer (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    transfer_no     VARCHAR(64)  NOT NULL,
    transfer_type   VARCHAR(32)  NOT NULL, -- NORMAL正常调拨/URGENT紧急调拨/RETURN退货调拨
    from_warehouse  VARCHAR(64)  NOT NULL, -- 调出仓库
    to_warehouse    VARCHAR(64)  NOT NULL, -- 调入仓库
    owner_code_col  VARCHAR(64),
    status          VARCHAR(32) DEFAULT 'CREATED', -- CREATED/APPROVED/SHIPPED/IN_TRANSIT/RECEIVED/DONE/CANCELLED
    total_qty       INT(18,4)  DEFAULT 0,
    shipped_qty     INT(18,4)  DEFAULT 0,
    received_qty    INT(18,4)  DEFAULT 0,
    difference_qty  INT(18,4)  DEFAULT 0, -- 差异数量
    carrier         VARCHAR(64), -- 承运商
    tracking_no     VARCHAR(128), -- 运单号
    expected_arrival TIMESTAMP, -- 预计到达时间
    ship_time       TIMESTAMP, -- 发运时间
    receive_time    TIMESTAMP, -- 收货时间
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    approver        VARCHAR(64),
    approve_time    TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_transfer PRIMARY KEY (id),
    CONSTRAINT uk_wms_transfer_no UNIQUE (transfer_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_transfer_status ON wms_transfer(status);
CREATE INDEX idx_wms_transfer_from ON wms_transfer(from_warehouse);
CREATE INDEX idx_wms_transfer_to ON wms_transfer(to_warehouse);

-- 2. 调拨明细表
CREATE TABLE wms_transfer_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    transfer_no     VARCHAR(64)  NOT NULL,
    line_no         INT    NOT NULL,
    sku_code        VARCHAR(64)  NOT NULL,
    batch_no        VARCHAR(128),
    from_location   VARCHAR(64), -- 调出库位
    to_location     VARCHAR(64), -- 调入库位
    expected_qty    INT(18,4)  DEFAULT 0,
    shipped_qty     INT(18,4)  DEFAULT 0,
    received_qty    INT(18,4)  DEFAULT 0,
    difference_qty  INT(18,4)  DEFAULT 0,
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING/SHIPPED/IN_TRANSIT/RECEIVED/DONE
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_transfer_detail PRIMARY KEY (id),
    CONSTRAINT uk_wms_transfer_detail UNIQUE (transfer_no, line_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_transfer_detail_no ON wms_transfer_detail(transfer_no);
CREATE INDEX idx_wms_transfer_detail_sku ON wms_transfer_detail(sku_code);

-- 3. 调拨在途库存表
CREATE TABLE wms_transfer_in_transit (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    transfer_no     VARCHAR(64)  NOT NULL,
    line_no         INT,
    sku_code        VARCHAR(64)  NOT NULL,
    batch_no        VARCHAR(128),
    from_warehouse  VARCHAR(64),
    to_warehouse    VARCHAR(64),
    in_transit_qty  INT(18,4)  NOT NULL, -- 在途数量
    received_qty    INT(18,4)  DEFAULT 0, -- 已收货数量
    status          VARCHAR(32) DEFAULT 'IN_TRANSIT', -- IN_TRANSIT/PARTIAL_RECEIVED/RECEIVED
    ship_time       TIMESTAMP,
    expected_arrival TIMESTAMP,
    receive_time    TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_transfer_it PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_transfer_it_no ON wms_transfer_in_transit(transfer_no);
CREATE INDEX idx_wms_transfer_it_sku ON wms_transfer_in_transit(sku_code);
CREATE INDEX idx_wms_transfer_it_status ON wms_transfer_in_transit(status);

-- 4. 调拨作业记录表
CREATE TABLE wms_transfer_task (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    task_no         VARCHAR(64)  NOT NULL,
    transfer_no     VARCHAR(64)  NOT NULL,
    task_type       VARCHAR(32)  NOT NULL, -- SHIP发运/RECEIVE收货
    sku_code        VARCHAR(64),
    batch_no        VARCHAR(128),
    from_location   VARCHAR(64),
    to_location     VARCHAR(64),
    task_qty        INT(18,4)  NOT NULL,
    done_qty        INT(18,4)  DEFAULT 0,
    operator        VARCHAR(64),
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING/PROCESSING/DONE/EXCEPTION
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_transfer_task PRIMARY KEY (id),
    CONSTRAINT uk_wms_transfer_task_no UNIQUE (task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_transfer_task_no ON wms_transfer_task(transfer_no);
CREATE INDEX idx_wms_transfer_task_status ON wms_transfer_task(status);


-- 注释
ALTER TABLE wms_transfer COMMENT='调拨单表';
ALTER TABLE wms_transfer_detail COMMENT='调拨明细表';
ALTER TABLE wms_transfer_in_transit COMMENT='调拨在途库存表';
ALTER TABLE wms_transfer_task COMMENT='调拨作业记录表';
