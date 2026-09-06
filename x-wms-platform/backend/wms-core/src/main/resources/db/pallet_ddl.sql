-- ============================================================
-- X WMS 托盘/LPN管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 托盘主表/托盘明细表/托盘操作记录表/码盘预约表
-- ============================================================

-- 1. 托盘主表
CREATE TABLE wms_pallet (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    lpn_no          VARCHAR(64)  NOT NULL, -- LPN号/托盘号
    pallet_type     VARCHAR(32), -- 托盘类型: STANDARD/CHEP/SMALL/BIG/CARTON/BAG
    status          VARCHAR(32)  DEFAULT 'EMPTY', -- 状态: EMPTY/IN_USE/FULL/IN_TRANSIT/STORED/SHIPPED/DAMAGED
    owner_code      VARCHAR(64), -- 货主编码
    warehouse_code  VARCHAR(64), -- 仓库编码
    area_code       VARCHAR(64), -- 当前库区
    location_code   VARCHAR(64), -- 当前库位
    source_location VARCHAR(64), -- 源库位
    target_location VARCHAR(64), -- 目标库位
    asn_no          VARCHAR(64), -- 关联ASN号
    inbound_no      VARCHAR(64), -- 关联入库单号
    receipt_task_no VARCHAR(64), -- 关联收货任务号
    outbound_no     VARCHAR(64), -- 关联出库单号
    wave_no         VARCHAR(64), -- 关联波次号
    sku_count       INT    DEFAULT 0, -- SKU种类数
    total_qty       INT(18,4)  DEFAULT 0, -- 总数量
    total_weight    INT(18,4)  DEFAULT 0, -- 总重量
    total_volume    INT(18,4)  DEFAULT 0, -- 总体积
    max_weight      INT(18,4), -- 最大承重
    max_volume      INT(18,4), -- 最大体积
    max_height      INT(18,4), -- 最大高度
    mixed_sku       VARCHAR(1)   DEFAULT 'N', -- 是否混SKU
    mixed_batch     VARCHAR(1)   DEFAULT 'N', -- 是否混批次
    sealed          VARCHAR(1)   DEFAULT 'N', -- 是否封存
    sealed_time     TIMESTAMP, -- 封存时间
    sealed_by       VARCHAR(64), -- 封存人
    palletized_by   VARCHAR(64), -- 码盘人
    palletized_time TIMESTAMP, -- 码盘时间
    last_operator   VARCHAR(64), -- 最后操作人
    last_operation_time TIMESTAMP, -- 最后操作时间
    remark          VARCHAR(512), -- 备注
    source          VARCHAR(32), -- 来源: RECEIVE/MANUAL/OUTBOUND/TRANSFER
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_pallet PRIMARY KEY (id),
    CONSTRAINT uk_wms_pallet_lpn UNIQUE (lpn_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_pallet_status ON wms_pallet(status);
CREATE INDEX idx_wms_pallet_location ON wms_pallet(location_code);
CREATE INDEX idx_wms_pallet_asn ON wms_pallet(asn_no);
CREATE INDEX idx_wms_pallet_inbound ON wms_pallet(inbound_no);
CREATE INDEX idx_wms_pallet_warehouse ON wms_pallet(warehouse_code);
CREATE INDEX idx_wms_pallet_type ON wms_pallet(pallet_type);

-- 2. 托盘明细表
CREATE TABLE wms_pallet_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    detail_no       VARCHAR(64)  NOT NULL, -- 明细号
    lpn_no          VARCHAR(64)  NOT NULL, -- LPN号
    line_no         INT, -- 行号
    asn_no          VARCHAR(64), -- 关联ASN号
    inbound_no      VARCHAR(64), -- 关联入库单号
    inbound_detail_no VARCHAR(64), -- 关联入库明细号
    receipt_detail_no VARCHAR(64), -- 关联收货任务明细号
    sku_code        VARCHAR(64), -- 商品编码
    sku_name        VARCHAR(256), -- 商品名称
    barcode         VARCHAR(64), -- 商品条码
    spec            VARCHAR(256), -- 规格型号
    unit            VARCHAR(32), -- 单位
    package_code    VARCHAR(64), -- 包装代码
    package_qty     INT(18,4), -- 包装数量
    qty             INT(18,4)  DEFAULT 0, -- 数量
    putaway_qty     INT(18,4)  DEFAULT 0, -- 已上架数量
    shipped_qty     INT(18,4)  DEFAULT 0, -- 已出库数量
    remaining_qty   INT(18,4)  DEFAULT 0, -- 剩余数量
    batch_no        VARCHAR(64), -- 批次号
    production_date TIMESTAMP, -- 生产日期
    expiry_date     TIMESTAMP, -- 失效日期
    serial_no       VARCHAR(64), -- 序列号
    product_weight  INT(18,4), -- 商品重量
    product_volume  INT(18,4), -- 商品体积
    product_height  INT(18,4), -- 商品高度
    status          VARCHAR(32)  DEFAULT 'ON_PALLET', -- 状态: ON_PALLET/PUTAWAYED/SHIPPED/REMOVED
    palletized_by   VARCHAR(64), -- 码盘人
    palletized_time TIMESTAMP, -- 码盘时间
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_pallet_detail PRIMARY KEY (id),
    CONSTRAINT uk_wms_pallet_detail_no UNIQUE (detail_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_pallet_detail_lpn ON wms_pallet_detail(lpn_no);
CREATE INDEX idx_wms_pallet_detail_sku ON wms_pallet_detail(sku_code);
CREATE INDEX idx_wms_pallet_detail_asn ON wms_pallet_detail(asn_no);
CREATE INDEX idx_wms_pallet_detail_status ON wms_pallet_detail(status);

-- 3. 托盘操作记录表
CREATE TABLE wms_pallet_operation (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    operation_no    VARCHAR(64)  NOT NULL, -- 操作记录号
    lpn_no          VARCHAR(64), -- LPN号
    operation_type  VARCHAR(32), -- 操作类型: PALLETIZE/DEPALLETIZE/MERGE/SPLIT/MOVE/SEAL/UNSEAL/DAMAGE/REPAIR/SCRAP
    before_status   VARCHAR(32), -- 操作前状态
    after_status    VARCHAR(32), -- 操作后状态
    before_location VARCHAR(64), -- 操作前库位
    after_location  VARCHAR(64), -- 操作后库位
    before_qty      INT(18,4), -- 操作前数量
    after_qty       INT(18,4), -- 操作后数量
    operation_qty   INT(18,4), -- 操作数量
    asn_no          VARCHAR(64), -- 关联ASN号
    inbound_no      VARCHAR(64), -- 关联入库单号
    outbound_no     VARCHAR(64), -- 关联出库单号
    wave_no         VARCHAR(64), -- 关联波次号
    sku_code        VARCHAR(64), -- 商品编码
    sku_name        VARCHAR(256), -- 商品名称
    batch_no        VARCHAR(64), -- 批次号
    serial_no       VARCHAR(64), -- 序列号
    source_lpn      VARCHAR(64), -- 源托盘号
    target_lpn      VARCHAR(64), -- 目标托盘号
    reason          VARCHAR(512), -- 操作原因
    operator        VARCHAR(64), -- 操作人
    operation_time  TIMESTAMP, -- 操作时间
    device_no       VARCHAR(64), -- 设备号
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_pallet_operation PRIMARY KEY (id),
    CONSTRAINT uk_wms_pallet_operation_no UNIQUE (operation_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_pallet_operation_lpn ON wms_pallet_operation(lpn_no);
CREATE INDEX idx_wms_pallet_operation_type ON wms_pallet_operation(operation_type);
CREATE INDEX idx_wms_pallet_operation_asn ON wms_pallet_operation(asn_no);
CREATE INDEX idx_wms_pallet_operation_time ON wms_pallet_operation(operation_time);

-- 4. 码盘预约表
CREATE TABLE wms_pallet_reservation (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    reservation_no  VARCHAR(64)  NOT NULL, -- 预约号
    asn_no          VARCHAR(64), -- 关联ASN号
    inbound_no      VARCHAR(64), -- 关联入库单号
    po_no           VARCHAR(64), -- 关联PO号
    supplier_code   VARCHAR(64), -- 供应商编码
    owner_code      VARCHAR(64), -- 货主编码
    warehouse_code  VARCHAR(64), -- 仓库编码
    reserved_pallet_count INT, -- 预约托盘数
    actual_pallet_count INT, -- 实际托盘数
    reserved_total_qty INT(18,4), -- 预约总数量
    actual_total_qty INT(18,4), -- 实际总数量
    reserved_receive_area VARCHAR(64), -- 预约收货库区
    reserved_receive_location VARCHAR(64), -- 预约收货库位
    reserved_dock_no VARCHAR(32), -- 预约月台号
    reserved_putaway_area VARCHAR(64), -- 预约上架库区
    reserved_putaway_locations VARCHAR(512), -- 预约上架库位
    palletize_strategy VARCHAR(32), -- 码盘策略: MIX_SKU/SINGLE_SKU/MIX_BATCH/SINGLE_BATCH
    putaway_strategy VARCHAR(32), -- 上架策略: NEAREST/FIFO/FEFO/ZONE/HEIGHT/WEIGHT
    status          VARCHAR(32)  DEFAULT 'PENDING', -- 状态: PENDING/RESERVED/IN_PROGRESS/COMPLETED/CANCELLED
    reservation_time TIMESTAMP, -- 预约时间
    reserved_by     VARCHAR(64), -- 预约人
    start_time      TIMESTAMP, -- 开始时间
    complete_time   TIMESTAMP, -- 完成时间
    operator        VARCHAR(64), -- 操作人
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_pallet_reservation PRIMARY KEY (id),
    CONSTRAINT uk_wms_pallet_reservation_no UNIQUE (reservation_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_pallet_reservation_asn ON wms_pallet_reservation(asn_no);
CREATE INDEX idx_wms_pallet_reservation_status ON wms_pallet_reservation(status);
CREATE INDEX idx_wms_pallet_reservation_warehouse ON wms_pallet_reservation(warehouse_code);


-- 注释
ALTER TABLE wms_pallet COMMENT='托盘/LPN主表';
ALTER TABLE wms_pallet_detail COMMENT='托盘明细表';
ALTER TABLE wms_pallet_operation COMMENT='托盘操作记录表';
ALTER TABLE wms_pallet_reservation COMMENT='码盘预约表';
