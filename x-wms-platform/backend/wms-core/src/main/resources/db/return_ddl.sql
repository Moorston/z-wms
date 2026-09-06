-- ============================================================
-- 退货入库模块DDL
-- 支持退货单管理、ASN编组、播种初分、二分、动态播种
-- ============================================================

-- 1. 退货单主表
CREATE TABLE wms_return_order (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    return_no       VARCHAR(64)  NOT NULL, -- 退货单号
    return_type     VARCHAR(32), -- 退货类型：CUSTOMER/SUPPLIER/TRANSFER/INTERNAL
    original_outbound_no VARCHAR(64), -- 原出库单号
    original_inbound_no VARCHAR(64), -- 原入库单号
    customer_code   VARCHAR(64), -- 客户编码
    customer_name   VARCHAR(256), -- 客户名称
    supplier_code   VARCHAR(64), -- 供应商编码
    supplier_name   VARCHAR(256), -- 供应商名称
    owner_code      VARCHAR(64), -- 货主编码
    warehouse_code  VARCHAR(64), -- 仓库编码
    return_reason   VARCHAR(512), -- 退货原因
    reason_code     VARCHAR(64), -- 退货原因代码
    total_qty       INT(18,4)  DEFAULT 0, -- 退货总数量
    received_qty    INT(18,4)  DEFAULT 0, -- 已收货数量
    putaway_qty     INT(18,4)  DEFAULT 0, -- 已上架数量
    qualified_qty   INT(18,4)  DEFAULT 0, -- 合格数量
    unqualified_qty INT(18,4)  DEFAULT 0, -- 不合格数量
    status          VARCHAR(32)  DEFAULT 'CREATED', -- 状态
    need_qc         VARCHAR(1)   DEFAULT 'N', -- 是否需要质检
    qc_status       VARCHAR(32)  DEFAULT 'NOT_NEEDED', -- 质检状态
    asn_no          VARCHAR(64), -- 关联ASN号
    group_no        VARCHAR(64), -- 关联ASN编组号
    apply_time      TIMESTAMP, -- 退货申请时间
    expected_arrival_time TIMESTAMP, -- 预计到货时间
    actual_arrival_time TIMESTAMP, -- 实际到货时间
    receive_complete_time TIMESTAMP, -- 收货完成时间
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_return_order PRIMARY KEY (id),
    CONSTRAINT uk_wms_return_no UNIQUE (return_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_return_customer ON wms_return_order(customer_code);
CREATE INDEX idx_wms_return_supplier ON wms_return_order(supplier_code);
CREATE INDEX idx_wms_return_owner ON wms_return_order(owner_code);
CREATE INDEX idx_wms_return_warehouse ON wms_return_order(warehouse_code);
CREATE INDEX idx_wms_return_status ON wms_return_order(status);
CREATE INDEX idx_wms_return_original ON wms_return_order(original_outbound_no);
CREATE INDEX idx_wms_return_group ON wms_return_order(group_no);

-- 2. 退货单明细表
CREATE TABLE wms_return_order_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    return_no       VARCHAR(64)  NOT NULL, -- 退货单号
    line_no         INT, -- 明细行号
    sku_code        VARCHAR(64), -- 商品编码
    sku_name        VARCHAR(256), -- 商品名称
    barcode         VARCHAR(64), -- 商品条码
    spec            VARCHAR(256), -- 规格型号
    unit            VARCHAR(32), -- 单位
    batch_no        VARCHAR(64), -- 批次号
    production_date TIMESTAMP, -- 生产日期
    expiry_date     TIMESTAMP, -- 失效日期
    return_qty      INT(18,4)  DEFAULT 0, -- 退货数量
    received_qty    INT(18,4)  DEFAULT 0, -- 已收货数量
    putaway_qty     INT(18,4)  DEFAULT 0, -- 已上架数量
    qualified_qty   INT(18,4)  DEFAULT 0, -- 合格数量
    unqualified_qty INT(18,4)  DEFAULT 0, -- 不合格数量
    return_reason   VARCHAR(512), -- 退货原因
    reason_code     VARCHAR(64), -- 退货原因代码
    original_outbound_qty INT(18,4) DEFAULT 0, -- 原出库数量
    original_price  INT(18,4), -- 原出库单价
    location_code   VARCHAR(64), -- 库位编码
    status          VARCHAR(32)  DEFAULT 'PENDING', -- 状态
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_return_detail PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_return_detail_return ON wms_return_order_detail(return_no);
CREATE INDEX idx_wms_return_detail_sku ON wms_return_order_detail(sku_code);
CREATE INDEX idx_wms_return_detail_status ON wms_return_order_detail(status);

-- 3. ASN编组表
CREATE TABLE wms_asn_group (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    group_no        VARCHAR(64)  NOT NULL, -- 编组号
    group_name      VARCHAR(256), -- 编组名称
    group_type      VARCHAR(32), -- 编组类型：RETURN/INBOUND/CROSSDOCK
    owner_code      VARCHAR(64), -- 货主编码
    warehouse_code  VARCHAR(64), -- 仓库编码
    asn_count       INT    DEFAULT 0, -- ASN数量
    sku_count       INT    DEFAULT 0, -- SKU种类数
    total_qty       INT(18,4)  DEFAULT 0, -- 总数量
    sku_overlap_rate INT(10,4) DEFAULT 0, -- SKU重合度
    status          VARCHAR(32)  DEFAULT 'CREATED', -- 状态
    sowing_mode     VARCHAR(32)  DEFAULT 'STATIC', -- 播种模式：STATIC/DYNAMIC
    sowing_location_count INT DEFAULT 0, -- 播种位数量
    sowing_start_time TIMESTAMP, -- 播种开始时间
    sowing_finish_time TIMESTAMP, -- 播种完成时间
    operator        VARCHAR(64), -- 操作人
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_asn_group PRIMARY KEY (id),
    CONSTRAINT uk_wms_group_no UNIQUE (group_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_group_owner ON wms_asn_group(owner_code);
CREATE INDEX idx_wms_group_warehouse ON wms_asn_group(warehouse_code);
CREATE INDEX idx_wms_group_status ON wms_asn_group(status);

-- 4. ASN编组明细表
CREATE TABLE wms_asn_group_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    group_no        VARCHAR(64)  NOT NULL, -- 编组号
    asn_no          VARCHAR(64), -- ASN号
    return_no       VARCHAR(64), -- 退货单号
    sku_code        VARCHAR(64), -- 商品编码
    sku_name        VARCHAR(256), -- 商品名称
    batch_no        VARCHAR(64), -- 批次号
    qty             INT(18,4)  DEFAULT 0, -- 数量
    sowed_qty       INT(18,4)  DEFAULT 0, -- 已播种数量
    sowing_location VARCHAR(64), -- 播种位编码
    sowing_seq      INT, -- 播种次序
    status          VARCHAR(32)  DEFAULT 'PENDING', -- 状态
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_group_detail PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_group_detail_group ON wms_asn_group_detail(group_no);
CREATE INDEX idx_wms_group_detail_sku ON wms_asn_group_detail(sku_code);
CREATE INDEX idx_wms_group_detail_return ON wms_asn_group_detail(return_no);

-- 5. 播种任务表
CREATE TABLE wms_sowing_task (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    task_no         VARCHAR(64)  NOT NULL, -- 播种任务号
    group_no        VARCHAR(64), -- 编组号
    sowing_stage    VARCHAR(32), -- 播种阶段：FIRST初分/SECOND二分
    sku_code        VARCHAR(64), -- 商品编码
    sku_name        VARCHAR(256), -- 商品名称
    batch_no        VARCHAR(64), -- 批次号
    total_qty       INT(18,4)  DEFAULT 0, -- 总数量
    sowed_qty       INT(18,4)  DEFAULT 0, -- 已播种数量
    remaining_qty   INT(18,4)  DEFAULT 0, -- 剩余数量
    sowing_location VARCHAR(64), -- 播种位编码
    target_location VARCHAR(64), -- 目标库位
    status          VARCHAR(32)  DEFAULT 'PENDING', -- 状态
    sowing_mode     VARCHAR(32)  DEFAULT 'STATIC', -- 播种模式
    sowing_seq      INT, -- 播种次序
    operator        VARCHAR(64), -- 操作人
    start_time      TIMESTAMP, -- 播种开始时间
    finish_time     TIMESTAMP, -- 播种完成时间
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_sowing_task PRIMARY KEY (id),
    CONSTRAINT uk_wms_sowing_task_no UNIQUE (task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_sowing_group ON wms_sowing_task(group_no);
CREATE INDEX idx_wms_sowing_sku ON wms_sowing_task(sku_code);
CREATE INDEX idx_wms_sowing_stage ON wms_sowing_task(sowing_stage);
CREATE INDEX idx_wms_sowing_status ON wms_sowing_task(status);


-- 注释
ALTER TABLE wms_return_order COMMENT='退货单主表';
ALTER TABLE wms_return_order_detail COMMENT='退货单明细表';
ALTER TABLE wms_asn_group COMMENT='ASN编组表';
ALTER TABLE wms_asn_group_detail COMMENT='ASN编组明细表';
ALTER TABLE wms_sowing_task COMMENT='播种任务表';
