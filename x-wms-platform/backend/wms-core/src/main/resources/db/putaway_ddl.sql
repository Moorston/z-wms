-- ============================================================
-- X WMS 上架管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 上架任务表/上架任务明细表/上架记录表
-- ============================================================

-- 1. 上架任务表
CREATE TABLE wms_putaway_task (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    task_no         VARCHAR(64)  NOT NULL, -- 上架任务号
    inbound_no      VARCHAR(64), -- 关联入库单号
    asn_no          VARCHAR(64), -- 关联ASN号
    receipt_task_no VARCHAR(64), -- 关联收货任务号
    putaway_type    VARCHAR(32), -- 上架方式: STANDARD/QUICK/MERGE/BATCH/LPN/DIRECT/RESERVATION
    putaway_strategy VARCHAR(32), -- 上架策略: NEAREST/FIFO/FEFO/ZONE/HEIGHT/WEIGHT
    supplier_code   VARCHAR(64), -- 供应商编码
    owner_code      VARCHAR(64), -- 货主编码
    warehouse_code  VARCHAR(64), -- 仓库编码
    source_area     VARCHAR(64), -- 收货库区（源库区）
    source_location VARCHAR(64), -- 收货库位（源库位）
    target_area     VARCHAR(64), -- 目标库区（推荐）
    target_location VARCHAR(64), -- 目标库位（推荐）
    expected_qty    INT(18,4)  DEFAULT 0, -- 预期数量
    putaway_qty     INT(18,4)  DEFAULT 0, -- 已上架数量
    difference_qty  INT(18,4)  DEFAULT 0, -- 差异数量
    status          VARCHAR(32)  DEFAULT 'PENDING', -- 状态: PENDING/PUTAWAYING/PARTIAL/COMPLETED/CANCELLED
    qc_required     VARCHAR(1)   DEFAULT 'N', -- 是否需要质检
    qc_status       VARCHAR(32), -- 质检状态
    system_recommend VARCHAR(1)  DEFAULT 'Y', -- 是否系统推荐库位
    allow_location_change VARCHAR(1) DEFAULT 'Y', -- 是否允许修改库位
    allow_qty_change VARCHAR(1)  DEFAULT 'Y', -- 是否允许修改数量
    lpn_no          VARCHAR(64), -- 托盘号/LPN号
    start_time      TIMESTAMP, -- 开始时间
    complete_time   TIMESTAMP, -- 完成时间
    operator        VARCHAR(64), -- 上架人
    remark          VARCHAR(512), -- 备注
    source          VARCHAR(32), -- 来源: RECEIPT/DIRECT/MANUAL
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_putaway_task PRIMARY KEY (id),
    CONSTRAINT uk_wms_putaway_task_no UNIQUE (task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_putaway_task_inbound ON wms_putaway_task(inbound_no);
CREATE INDEX idx_wms_putaway_task_asn ON wms_putaway_task(asn_no);
CREATE INDEX idx_wms_putaway_task_warehouse ON wms_putaway_task(warehouse_code);
CREATE INDEX idx_wms_putaway_task_status ON wms_putaway_task(status);
CREATE INDEX idx_wms_putaway_task_type ON wms_putaway_task(putaway_type);

-- 2. 上架任务明细表
CREATE TABLE wms_putaway_task_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    detail_no       VARCHAR(64)  NOT NULL, -- 明细号
    task_no         VARCHAR(64)  NOT NULL, -- 上架任务号
    line_no         INT, -- 行号
    inbound_detail_no VARCHAR(64), -- 关联入库明细号
    sku_code        VARCHAR(64)  NOT NULL, -- 商品编码
    sku_name        VARCHAR(256), -- 商品名称
    barcode         VARCHAR(64), -- 商品条码
    spec            VARCHAR(256), -- 规格型号
    unit            VARCHAR(32), -- 单位
    package_code    VARCHAR(64), -- 包装代码
    package_qty     INT(18,4), -- 包装数量
    product_weight  INT(18,4), -- 商品重量（kg）
    product_height  INT(18,4), -- 商品高度（cm）
    expected_qty    INT(18,4)  DEFAULT 0, -- 预期数量
    putaway_qty     INT(18,4)  DEFAULT 0, -- 已上架数量
    difference_qty  INT(18,4)  DEFAULT 0, -- 差异数量
    batch_no        VARCHAR(64), -- 批次号
    production_date TIMESTAMP, -- 生产日期
    expiry_date     TIMESTAMP, -- 失效日期
    serial_no       VARCHAR(64), -- 序列号
    source_location VARCHAR(64), -- 源库位（收货库位）
    recommend_location VARCHAR(64), -- 推荐目标库位
    actual_location VARCHAR(64), -- 实际目标库位
    recommend_area  VARCHAR(64), -- 推荐库区
    actual_area     VARCHAR(64), -- 实际库区
    location_type   VARCHAR(32), -- 库位类型: STORAGE/PICKING/RECEIVING/SHIPPING/BULK
    batch_managed   VARCHAR(1)   DEFAULT 'N', -- 是否需要批次管理
    serial_managed  VARCHAR(1)   DEFAULT 'N', -- 是否需要序列号管理
    expiry_managed  VARCHAR(1)   DEFAULT 'N', -- 是否需要效期管理
    status          VARCHAR(32)  DEFAULT 'PENDING', -- 状态
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_putaway_task_detail PRIMARY KEY (id),
    CONSTRAINT uk_wms_putaway_task_detail_no UNIQUE (detail_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_putaway_task_detail_task ON wms_putaway_task_detail(task_no);
CREATE INDEX idx_wms_putaway_task_detail_sku ON wms_putaway_task_detail(sku_code);
CREATE INDEX idx_wms_putaway_task_detail_status ON wms_putaway_task_detail(status);
CREATE INDEX idx_wms_putaway_task_detail_recommend ON wms_putaway_task_detail(recommend_location);

-- 3. 上架记录表
CREATE TABLE wms_putaway_record (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    record_no       VARCHAR(64)  NOT NULL, -- 上架记录号
    task_no         VARCHAR(64), -- 上架任务号
    inbound_no      VARCHAR(64), -- 关联入库单号
    asn_no          VARCHAR(64), -- 关联ASN号
    task_detail_no  VARCHAR(64), -- 上架任务明细号
    inbound_detail_no VARCHAR(64), -- 关联入库明细号
    sku_code        VARCHAR(64), -- 商品编码
    sku_name        VARCHAR(256), -- 商品名称
    barcode         VARCHAR(64), -- 商品条码
    batch_no        VARCHAR(64), -- 批次号
    production_date TIMESTAMP, -- 生产日期
    expiry_date     TIMESTAMP, -- 失效日期
    serial_no       VARCHAR(64), -- 序列号
    putaway_qty     INT(18,4)  DEFAULT 0, -- 上架数量
    unit            VARCHAR(32), -- 单位
    package_code    VARCHAR(64), -- 包装代码
    package_qty     INT(18,4), -- 包装数量
    lpn_no          VARCHAR(64), -- 托盘号/LPN号
    source_location VARCHAR(64), -- 源库位
    recommend_location VARCHAR(64), -- 推荐目标库位
    target_location VARCHAR(64), -- 实际目标库位
    target_area     VARCHAR(64), -- 目标库区
    location_type   VARCHAR(32), -- 库位类型
    putaway_type    VARCHAR(32), -- 上架方式
    putaway_strategy VARCHAR(32), -- 上架策略
    use_system_recommend VARCHAR(1), -- 是否使用系统推荐库位
    difference_qty  INT(18,4), -- 差异数量
    difference_type VARCHAR(16), -- 差异类型: OVER/SHORT/NONE
    operator        VARCHAR(64), -- 上架人
    putaway_time    TIMESTAMP, -- 上架时间
    device_no       VARCHAR(64), -- 设备号
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_putaway_record PRIMARY KEY (id),
    CONSTRAINT uk_wms_putaway_record_no UNIQUE (record_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_putaway_record_task ON wms_putaway_record(task_no);
CREATE INDEX idx_wms_putaway_record_inbound ON wms_putaway_record(inbound_no);
CREATE INDEX idx_wms_putaway_record_sku ON wms_putaway_record(sku_code);
CREATE INDEX idx_wms_putaway_record_location ON wms_putaway_record(target_location);
CREATE INDEX idx_wms_putaway_record_type ON wms_putaway_record(putaway_type);


-- 注释
ALTER TABLE wms_putaway_task COMMENT='上架任务表';
ALTER TABLE wms_putaway_task_detail COMMENT='上架任务明细表';
ALTER TABLE wms_putaway_record COMMENT='上架记录表';
