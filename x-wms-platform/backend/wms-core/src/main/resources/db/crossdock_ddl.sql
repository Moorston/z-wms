-- ============================================================
-- X WMS 越库管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 越库单/越库明细/越库匹配/越库作业记录
-- ============================================================

-- 1. 越库单表
CREATE TABLE wms_crossdock (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    crossdock_no    VARCHAR(64)  NOT NULL,
    crossdock_type  VARCHAR(32)  NOT NULL, -- FLOW_THROUGH直通/COMBINE合并/BREAK_BULK拆零
    warehouse_code  VARCHAR(64),
    owner_code_col  VARCHAR(64),
    inbound_no      VARCHAR(64), -- 关联入库单
    outbound_no     VARCHAR(64), -- 关联出库单
    status          VARCHAR(32) DEFAULT 'CREATED', -- CREATED/MATCHED/RECEIVING/RECEIVED/SORTING/SORTED/SHIPPING/SHIPPED/CANCELLED
    total_qty       INT(18,4)  DEFAULT 0,
    received_qty    INT(18,4)  DEFAULT 0,
    sorted_qty      INT(18,4)  DEFAULT 0,
    shipped_qty     INT(18,4)  DEFAULT 0,
    inbound_dock    VARCHAR(64), -- 入库月台
    outbound_dock   VARCHAR(64), -- 出库月台
    scheduled_time  TIMESTAMP, -- 计划越库时间
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_crossdock PRIMARY KEY (id),
    CONSTRAINT uk_wms_crossdock_no UNIQUE (crossdock_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_cd_status ON wms_crossdock(status);
CREATE INDEX idx_wms_cd_inbound ON wms_crossdock(inbound_no);
CREATE INDEX idx_wms_cd_outbound ON wms_crossdock(outbound_no);

-- 2. 越库明细表
CREATE TABLE wms_crossdock_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    crossdock_no    VARCHAR(64)  NOT NULL,
    line_no         INT    NOT NULL,
    sku_code        VARCHAR(64)  NOT NULL,
    batch_no        VARCHAR(128),
    expected_qty    INT(18,4)  DEFAULT 0,
    received_qty    INT(18,4)  DEFAULT 0,
    sorted_qty      INT(18,4)  DEFAULT 0,
    shipped_qty     INT(18,4)  DEFAULT 0,
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING/RECEIVED/SORTED/SHIPPED
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_cd_detail PRIMARY KEY (id),
    CONSTRAINT uk_wms_cd_detail UNIQUE (crossdock_no, line_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_cd_detail_cd ON wms_crossdock_detail(crossdock_no);
CREATE INDEX idx_wms_cd_detail_sku ON wms_crossdock_detail(sku_code);

-- 3. 越库匹配表（入库明细与出库明细的匹配关系）
CREATE TABLE wms_crossdock_match (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    match_no        VARCHAR(64)  NOT NULL,
    crossdock_no    VARCHAR(64)  NOT NULL,
    inbound_line_no INT, -- 入库明细行号
    outbound_line_no INT, -- 出库明细行号
    sku_code        VARCHAR(64),
    batch_no        VARCHAR(128),
    match_qty       INT(18,4)  NOT NULL,
    match_type      VARCHAR(32), -- EXACT精确匹配/SUBSTITUTE替代/PARTIAL部分匹配
    status          VARCHAR(32) DEFAULT 'MATCHED', -- MATCHED/RECEIVED/SHIPPED
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_cd_match PRIMARY KEY (id),
    CONSTRAINT uk_wms_cd_match_no UNIQUE (match_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_cd_match_cd ON wms_crossdock_match(crossdock_no);

-- 4. 越库作业记录表
CREATE TABLE wms_crossdock_task (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    task_no         VARCHAR(64)  NOT NULL,
    crossdock_no    VARCHAR(64)  NOT NULL,
    task_type       VARCHAR(32)  NOT NULL, -- RECEIVE收货/SORT分拣/SHIP发运
    sku_code        VARCHAR(64),
    batch_no        VARCHAR(128),
    from_location   VARCHAR(64), -- 来源库位/月台
    to_location     VARCHAR(64), -- 目标库位/月台
    task_qty        INT(18,4)  NOT NULL,
    done_qty        INT(18,4)  DEFAULT 0,
    operator        VARCHAR(64),
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING/PROCESSING/DONE/EXCEPTION
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_cd_task PRIMARY KEY (id),
    CONSTRAINT uk_wms_cd_task_no UNIQUE (task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_cd_task_cd ON wms_crossdock_task(crossdock_no);
CREATE INDEX idx_wms_cd_task_status ON wms_crossdock_task(status);


-- 注释
ALTER TABLE wms_crossdock COMMENT='越库单表';
ALTER TABLE wms_crossdock_detail COMMENT='越库明细表';
ALTER TABLE wms_crossdock_match COMMENT='越库匹配表（入库与出库匹配）';
ALTER TABLE wms_crossdock_task COMMENT='越库作业记录表';

-- 5. 越库预配表（ASN到货前为等待的SO预先匹配）
CREATE TABLE wms_crossdock_pre_alloc (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    alloc_no        VARCHAR(64)  NOT NULL, -- 预配单号
    crossdock_no    VARCHAR(64), -- 越库单号
    asn_no          VARCHAR(64), -- ASN号
    inbound_no      VARCHAR(64), -- 入库单号
    outbound_no     VARCHAR(64), -- 出库单号（SO）
    wave_no         VARCHAR(64), -- 波次号
    sku_code        VARCHAR(64), -- 商品编码
    sku_name        VARCHAR(256), -- 商品名称
    batch_no        VARCHAR(64), -- 批次号
    alloc_qty       INT(18,4)  DEFAULT 0, -- 预配数量
    received_qty    INT(18,4)  DEFAULT 0, -- 已收货数量
    allocated_qty   INT(18,4)  DEFAULT 0, -- 已分配数量
    sowing_location VARCHAR(64), -- 播种位编码
    outbound_dock   VARCHAR(64), -- 出库月台
    match_type      VARCHAR(32), -- 匹配类型：EXACT/FUZZY/MANUAL
    rule_code       VARCHAR(64), -- 匹配规则编码
    status          VARCHAR(32)  DEFAULT 'PENDING', -- 状态
    priority        INT, -- 优先级
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_cd_pre_alloc PRIMARY KEY (id),
    CONSTRAINT uk_wms_cd_alloc_no UNIQUE (alloc_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_cd_alloc_cd ON wms_crossdock_pre_alloc(crossdock_no);
CREATE INDEX idx_wms_cd_alloc_asn ON wms_crossdock_pre_alloc(asn_no);
CREATE INDEX idx_wms_cd_alloc_outbound ON wms_crossdock_pre_alloc(outbound_no);
CREATE INDEX idx_wms_cd_alloc_sku ON wms_crossdock_pre_alloc(sku_code);
CREATE INDEX idx_wms_cd_alloc_status ON wms_crossdock_pre_alloc(status);

-- 6. 越库预分表（RF扫描箱码执行收货确认，后台分配库存）
CREATE TABLE wms_crossdock_pre_sort (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    sort_no         VARCHAR(64)  NOT NULL, -- 预分单号
    crossdock_no    VARCHAR(64), -- 越库单号
    alloc_no        VARCHAR(64), -- 预配单号
    asn_no          VARCHAR(64), -- ASN号
    box_no          VARCHAR(64), -- 箱号/LPN
    box_serial_no   VARCHAR(128), -- 箱序列号
    sku_code        VARCHAR(64), -- 商品编码
    sku_name        VARCHAR(256), -- 商品名称
    batch_no        VARCHAR(64), -- 批次号
    box_qty         INT(18,4)  DEFAULT 0, -- 箱内数量
    received_qty    INT(18,4)  DEFAULT 0, -- 已收货数量
    sorted_qty      INT(18,4)  DEFAULT 0, -- 已分拣数量
    outbound_no     VARCHAR(64), -- 出库单号
    sowing_location VARCHAR(64), -- 播种位编码
    outbound_dock   VARCHAR(64), -- 出库月台
    xdock_type      VARCHAR(32), -- 类型：XDOCK需越库/NONE-XDOCK无需越库
    status          VARCHAR(32)  DEFAULT 'PENDING', -- 状态
    scanner         VARCHAR(64), -- 扫描人
    scan_time       TIMESTAMP, -- 扫描时间
    receive_time    TIMESTAMP, -- 收货时间
    sort_time       TIMESTAMP, -- 分拣时间
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_cd_pre_sort PRIMARY KEY (id),
    CONSTRAINT uk_wms_cd_sort_no UNIQUE (sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_cd_sort_cd ON wms_crossdock_pre_sort(crossdock_no);
CREATE INDEX idx_wms_cd_sort_box ON wms_crossdock_pre_sort(box_no);
CREATE INDEX idx_wms_cd_sort_asn ON wms_crossdock_pre_sort(asn_no);
CREATE INDEX idx_wms_cd_sort_sku ON wms_crossdock_pre_sort(sku_code);
CREATE INDEX idx_wms_cd_sort_status ON wms_crossdock_pre_sort(status);
CREATE INDEX idx_wms_cd_sort_xdock ON wms_crossdock_pre_sort(xdock_type);

-- 新增序列

-- 新增注释
ALTER TABLE wms_crossdock_pre_alloc COMMENT='越库预配表';
ALTER TABLE wms_crossdock_pre_sort COMMENT='越库预分表';
