-- ============================================================
-- X WMS 采购订单（PO）管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 采购订单主表/采购订单明细表/PO-ASN关联表
-- ============================================================

-- 1. 采购订单主表
CREATE TABLE wms_purchase_order (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    po_no           VARCHAR(64)  NOT NULL, -- 采购订单号
    external_po_no  VARCHAR(64), -- 外部订单号（ERP订单号）
    po_type         VARCHAR(32)  NOT NULL, -- 订单类型: STANDARD标准采购/RETURN退货采购/CONSIGNMENT寄售/VMI供应商管理库存
    supplier_code   VARCHAR(64), -- 供应商编码
    supplier_name   VARCHAR(256), -- 供应商名称
    owner_code      VARCHAR(64), -- 货主编码
    warehouse_code  VARCHAR(64), -- 仓库编码
    po_date         DATE, -- 采购日期
    expected_arrival_date DATE, -- 预期到货日期
    expected_start_time TIMESTAMP, -- 预期到货开始时间
    expected_end_time TIMESTAMP, -- 预期到货结束时间
    total_qty       INT(18,4)  DEFAULT 0, -- 订单总数量
    released_qty    INT(18,4)  DEFAULT 0, -- 已释放数量（提取到ASN的数量）
    received_qty    INT(18,4)  DEFAULT 0, -- 已收货数量
    putaway_qty     INT(18,4)  DEFAULT 0, -- 已入库数量
    total_amount    INT(18,4), -- 订单总金额
    currency        VARCHAR(8), -- 币种
    status          VARCHAR(32)  DEFAULT 'CREATED', -- 状态: CREATED/RELEASED/PARTIAL_RECEIVED/FULLY_RECEIVED/COMPLETED/CANCELLED
    release_status  VARCHAR(32)  DEFAULT 'UNRELEASED', -- 释放状态: UNRELEASED/PARTIAL/FULLY
    asn_linked      VARCHAR(1)   DEFAULT 'Y', -- 是否与ASN关联
    contract_no     VARCHAR(64), -- 采购合同号
    buyer           VARCHAR(64), -- 采购员
    approval_status VARCHAR(32)  DEFAULT 'DRAFT', -- 审批状态: DRAFT/PENDING/APPROVED/REJECTED
    approver        VARCHAR(64), -- 审批人
    approval_time   TIMESTAMP, -- 审批时间
    remark          VARCHAR(512), -- 备注
    source          VARCHAR(32), -- 来源: ERP/MANUAL/EXCEL
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_purchase_order PRIMARY KEY (id),
    CONSTRAINT uk_wms_po_no UNIQUE (po_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_po_supplier ON wms_purchase_order(supplier_code);
CREATE INDEX idx_wms_po_warehouse ON wms_purchase_order(warehouse_code);
CREATE INDEX idx_wms_po_owner ON wms_purchase_order(owner_code);
CREATE INDEX idx_wms_po_status ON wms_purchase_order(status);
CREATE INDEX idx_wms_po_type ON wms_purchase_order(po_type);
CREATE INDEX idx_wms_po_external ON wms_purchase_order(external_po_no);

-- 2. 采购订单明细表
CREATE TABLE wms_purchase_order_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    detail_no       VARCHAR(64)  NOT NULL, -- 明细号
    po_no           VARCHAR(64)  NOT NULL, -- 采购订单号
    line_no         INT    NOT NULL, -- 行号
    sku_code        VARCHAR(64)  NOT NULL, -- 商品编码
    sku_name        VARCHAR(256), -- 商品名称
    barcode         VARCHAR(64), -- 商品条码
    spec            VARCHAR(256), -- 规格型号
    unit            VARCHAR(32), -- 单位
    package_code    VARCHAR(64), -- 包装代码
    package_qty     INT(18,4), -- 包装数量（每包装件数）
    order_qty       INT(18,4)  DEFAULT 0, -- 订购数量
    released_qty    INT(18,4)  DEFAULT 0, -- 已释放数量
    received_qty    INT(18,4)  DEFAULT 0, -- 已收货数量
    putaway_qty     INT(18,4)  DEFAULT 0, -- 已入库数量
    unit_price      INT(18,4), -- 单价
    amount          INT(18,4), -- 金额
    tax_rate        INT(10,4), -- 税率
    tax_amount      INT(18,4), -- 税额
    batch_managed   VARCHAR(1)   DEFAULT 'N', -- 是否需要批次管理
    serial_managed  VARCHAR(1)   DEFAULT 'N', -- 是否需要序列号管理
    expiry_managed  VARCHAR(1)   DEFAULT 'N', -- 是否需要效期管理
    qc_required     VARCHAR(1)   DEFAULT 'N', -- 是否需要质检
    qc_type         VARCHAR(32), -- 质检类型: FULL/SAMPLE/NONE
    default_receive_location VARCHAR(64), -- 默认收货库位
    default_putaway_area VARCHAR(64), -- 默认上架库区
    status          VARCHAR(32)  DEFAULT 'CREATED', -- 状态
    close_reason    VARCHAR(256), -- 关闭原因
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_po_detail PRIMARY KEY (id),
    CONSTRAINT uk_wms_po_detail_no UNIQUE (detail_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_po_detail_po ON wms_purchase_order_detail(po_no);
CREATE INDEX idx_wms_po_detail_sku ON wms_purchase_order_detail(sku_code);
CREATE INDEX idx_wms_po_detail_status ON wms_purchase_order_detail(status);

-- 3. PO-ASN关联表
CREATE TABLE wms_po_asn_relation (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    relation_no     VARCHAR(64)  NOT NULL, -- 关联号
    po_no           VARCHAR(64)  NOT NULL, -- 采购订单号
    po_line_no      INT, -- PO行号
    po_detail_no    VARCHAR(64), -- PO明细号
    asn_no          VARCHAR(64)  NOT NULL, -- ASN编号
    asn_line_no     INT, -- ASN行号
    sku_code        VARCHAR(64), -- 商品编码
    release_qty     INT(18,4)  DEFAULT 0, -- 提取数量
    received_qty    INT(18,4)  DEFAULT 0, -- 已收货数量
    putaway_qty     INT(18,4)  DEFAULT 0, -- 已入库数量
    release_time    TIMESTAMP, -- 提取时间
    released_by     VARCHAR(64), -- 提取人
    status          VARCHAR(32)  DEFAULT 'RELEASED', -- 状态: RELEASED/RECEIVED/PUTAWAY/CANCELLED
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_po_asn_rel PRIMARY KEY (id),
    CONSTRAINT uk_wms_po_asn_rel_no UNIQUE (relation_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_po_asn_rel_po ON wms_po_asn_relation(po_no);
CREATE INDEX idx_wms_po_asn_rel_asn ON wms_po_asn_relation(asn_no);
CREATE INDEX idx_wms_po_asn_rel_sku ON wms_po_asn_relation(sku_code);
CREATE INDEX idx_wms_po_asn_rel_status ON wms_po_asn_relation(status);


-- 注释
ALTER TABLE wms_purchase_order COMMENT='采购订单主表';
ALTER TABLE wms_purchase_order_detail COMMENT='采购订单明细表';
ALTER TABLE wms_po_asn_relation COMMENT='PO-ASN关联表';
