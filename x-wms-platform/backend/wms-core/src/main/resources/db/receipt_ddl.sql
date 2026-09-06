-- ============================================================
-- X WMS 收货管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 收货任务表/收货任务明细表/收货记录表/扫描收货日志表/盲收记录表
-- ============================================================

-- 1. 收货任务表
CREATE TABLE wms_receipt_task (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    task_no         VARCHAR(64)  NOT NULL, -- 收货任务号
    asn_no          VARCHAR(64), -- 关联ASN号
    inbound_no      VARCHAR(64), -- 关联入库单号
    po_no           VARCHAR(64), -- 关联PO号
    receipt_type    VARCHAR(32), -- 收货方式: ASN/PARTIAL/PALLET/SCAN/BOX/QUICK/VISUAL/MIX/COMPONENT/SORT/PRE/BLIND
    scan_mode       VARCHAR(32), -- 扫描模式: BATCH/PIECE/BOX/SERIAL
    supplier_code   VARCHAR(64), -- 供应商编码
    owner_code      VARCHAR(64), -- 货主编码
    warehouse_code  VARCHAR(64), -- 仓库编码
    receive_area    VARCHAR(64), -- 收货库区
    receive_location VARCHAR(64), -- 收货库位
    dock_no         VARCHAR(32), -- 月台号
    expected_qty    INT(18,4)  DEFAULT 0, -- 预期数量
    received_qty    INT(18,4)  DEFAULT 0, -- 已收货数量
    difference_qty  INT(18,4)  DEFAULT 0, -- 差异数量
    status          VARCHAR(32)  DEFAULT 'PENDING', -- 状态: PENDING/RECEIVING/PARTIAL/COMPLETED/CANCELLED
    qc_required     VARCHAR(1)   DEFAULT 'N', -- 是否需要质检
    qc_status       VARCHAR(32), -- 质检状态
    direct_putaway  VARCHAR(1)   DEFAULT 'N', -- 是否直接上架
    vehicle_no      VARCHAR(32), -- 车牌号
    driver_name     VARCHAR(64), -- 司机姓名
    driver_phone    VARCHAR(32), -- 司机电话
    start_time      TIMESTAMP, -- 开始时间
    complete_time   TIMESTAMP, -- 完成时间
    receiver        VARCHAR(64), -- 收货人
    remark          VARCHAR(512), -- 备注
    source          VARCHAR(32), -- 来源: ASN/MANUAL/BLIND
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_receipt_task PRIMARY KEY (id),
    CONSTRAINT uk_wms_receipt_task_no UNIQUE (task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_receipt_task_asn ON wms_receipt_task(asn_no);
CREATE INDEX idx_wms_receipt_task_inbound ON wms_receipt_task(inbound_no);
CREATE INDEX idx_wms_receipt_task_warehouse ON wms_receipt_task(warehouse_code);
CREATE INDEX idx_wms_receipt_task_status ON wms_receipt_task(status);
CREATE INDEX idx_wms_receipt_task_type ON wms_receipt_task(receipt_type);

-- 2. 收货任务明细表
CREATE TABLE wms_receipt_task_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    detail_no       VARCHAR(64)  NOT NULL, -- 明细号
    task_no         VARCHAR(64)  NOT NULL, -- 收货任务号
    line_no         INT, -- 行号
    asn_line_no     INT, -- 关联ASN行号
    inbound_detail_no VARCHAR(64), -- 关联入库明细号
    sku_code        VARCHAR(64)  NOT NULL, -- 商品编码
    sku_name        VARCHAR(256), -- 商品名称
    barcode         VARCHAR(64), -- 商品条码
    spec            VARCHAR(256), -- 规格型号
    unit            VARCHAR(32), -- 单位
    package_code    VARCHAR(64), -- 包装代码
    package_qty     INT(18,4), -- 包装数量
    expected_qty    INT(18,4)  DEFAULT 0, -- 预期数量
    received_qty    INT(18,4)  DEFAULT 0, -- 已收货数量
    difference_qty  INT(18,4)  DEFAULT 0, -- 差异数量
    batch_no        VARCHAR(64), -- 批次号
    production_date TIMESTAMP, -- 生产日期
    expiry_date     TIMESTAMP, -- 失效日期
    serial_no       VARCHAR(64), -- 序列号
    batch_managed   VARCHAR(1)   DEFAULT 'N', -- 是否需要批次管理
    serial_managed  VARCHAR(1)   DEFAULT 'N', -- 是否需要序列号管理
    expiry_managed  VARCHAR(1)   DEFAULT 'N', -- 是否需要效期管理
    qc_required     VARCHAR(1)   DEFAULT 'N', -- 是否需要质检
    receive_location VARCHAR(64), -- 收货库位
    status          VARCHAR(32)  DEFAULT 'PENDING', -- 状态
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_receipt_task_detail PRIMARY KEY (id),
    CONSTRAINT uk_wms_receipt_task_detail_no UNIQUE (detail_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_receipt_task_detail_task ON wms_receipt_task_detail(task_no);
CREATE INDEX idx_wms_receipt_task_detail_sku ON wms_receipt_task_detail(sku_code);
CREATE INDEX idx_wms_receipt_task_detail_status ON wms_receipt_task_detail(status);

-- 3. 收货记录表
CREATE TABLE wms_receipt_record (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    record_no       VARCHAR(64)  NOT NULL, -- 收货记录号
    task_no         VARCHAR(64), -- 收货任务号
    asn_no          VARCHAR(64), -- 关联ASN号
    inbound_no      VARCHAR(64), -- 关联入库单号
    task_detail_no  VARCHAR(64), -- 收货任务明细号
    inbound_detail_no VARCHAR(64), -- 关联入库明细号
    sku_code        VARCHAR(64), -- 商品编码
    sku_name        VARCHAR(256), -- 商品名称
    barcode         VARCHAR(64), -- 商品条码
    batch_no        VARCHAR(64), -- 批次号
    production_date TIMESTAMP, -- 生产日期
    expiry_date     TIMESTAMP, -- 失效日期
    serial_no       VARCHAR(64), -- 序列号
    receive_qty     INT(18,4)  DEFAULT 0, -- 收货数量
    unit            VARCHAR(32), -- 单位
    package_code    VARCHAR(64), -- 包装代码
    package_qty     INT(18,4), -- 包装数量
    lpn_no          VARCHAR(64), -- 箱号/LPN号
    receive_location VARCHAR(64), -- 收货库位
    receive_area    VARCHAR(64), -- 收货库区
    dock_no         VARCHAR(32), -- 月台号
    receipt_type    VARCHAR(32), -- 收货方式
    scan_mode       VARCHAR(32), -- 扫描模式
    difference_qty  INT(18,4), -- 差异数量
    difference_type VARCHAR(16), -- 差异类型: OVER/SHORT/NONE
    qc_required     VARCHAR(1)   DEFAULT 'N', -- 是否需要质检
    qc_status       VARCHAR(32), -- 质检状态
    direct_putaway  VARCHAR(1)   DEFAULT 'N', -- 是否直接上架
    operator        VARCHAR(64), -- 收货人
    receive_time    TIMESTAMP, -- 收货时间
    device_no       VARCHAR(64), -- 设备号
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_receipt_record PRIMARY KEY (id),
    CONSTRAINT uk_wms_receipt_record_no UNIQUE (record_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_receipt_record_task ON wms_receipt_record(task_no);
CREATE INDEX idx_wms_receipt_record_asn ON wms_receipt_record(asn_no);
CREATE INDEX idx_wms_receipt_record_inbound ON wms_receipt_record(inbound_no);
CREATE INDEX idx_wms_receipt_record_sku ON wms_receipt_record(sku_code);
CREATE INDEX idx_wms_receipt_record_type ON wms_receipt_record(receipt_type);

-- 4. 扫描收货日志表
CREATE TABLE wms_receipt_scan_log (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    scan_no         VARCHAR(64)  NOT NULL, -- 扫描日志号
    task_no         VARCHAR(64), -- 收货任务号
    record_no       VARCHAR(64), -- 收货记录号
    scan_type       VARCHAR(32), -- 扫描类型: BARCODE/SERIAL/LPN/BATCH
    scan_content    VARCHAR(256), -- 扫描内容
    sku_code        VARCHAR(64), -- 商品编码
    sku_name        VARCHAR(256), -- 商品名称
    batch_no        VARCHAR(64), -- 批次号
    serial_no       VARCHAR(64), -- 序列号
    lpn_no          VARCHAR(64), -- 箱号/LPN号
    scan_qty        INT(18,4), -- 扫描数量
    scan_mode       VARCHAR(32), -- 扫描模式
    scan_result     VARCHAR(32), -- 扫描结果: SUCCESS/FAIL/DUPLICATE/UNKNOWN
    fail_reason     VARCHAR(512), -- 失败原因
    full_box_alert  VARCHAR(1)   DEFAULT 'N', -- 是否满箱提醒
    scan_location   VARCHAR(64), -- 扫描库位
    operator        VARCHAR(64), -- 扫描人
    scan_time       TIMESTAMP, -- 扫描时间
    device_no       VARCHAR(64), -- 设备号
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_receipt_scan_log PRIMARY KEY (id),
    CONSTRAINT uk_wms_receipt_scan_no UNIQUE (scan_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_receipt_scan_task ON wms_receipt_scan_log(task_no);
CREATE INDEX idx_wms_receipt_scan_content ON wms_receipt_scan_log(scan_content);
CREATE INDEX idx_wms_receipt_scan_result ON wms_receipt_scan_log(scan_result);

-- 5. 盲收记录表
CREATE TABLE wms_blind_receipt (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    blind_no        VARCHAR(64)  NOT NULL, -- 盲收单号
    blind_mode      VARCHAR(32), -- 盲收模式: NORMAL/SIMPLIFIED
    supplier_code   VARCHAR(64), -- 供应商编码
    supplier_name   VARCHAR(256), -- 供应商名称
    owner_code      VARCHAR(64), -- 货主编码
    warehouse_code  VARCHAR(64), -- 仓库编码
    receive_area    VARCHAR(64), -- 收货库区
    receive_location VARCHAR(64), -- 收货库位
    dock_no         VARCHAR(32), -- 月台号
    sku_code        VARCHAR(64), -- 商品编码
    sku_name        VARCHAR(256), -- 商品名称
    barcode         VARCHAR(64), -- 商品条码
    spec            VARCHAR(256), -- 规格型号
    unit            VARCHAR(32), -- 单位
    blind_qty       INT(18,4)  DEFAULT 0, -- 盲收数量
    batch_no        VARCHAR(64), -- 批次号
    production_date TIMESTAMP, -- 生产日期
    expiry_date     TIMESTAMP, -- 失效日期
    serial_no       VARCHAR(64), -- 序列号
    lpn_no          VARCHAR(64), -- 箱号/LPN号
    status          VARCHAR(32)  DEFAULT 'BLIND_RECEIVED', -- 状态
    matched_po_no   VARCHAR(64), -- 匹配的PO号
    matched_asn_no  VARCHAR(64), -- 匹配的ASN号
    matched_inbound_no VARCHAR(64), -- 匹配的入库单号
    match_time      TIMESTAMP, -- 匹配时间
    matched_by      VARCHAR(64), -- 匹配人
    asn_create_time TIMESTAMP, -- 生成ASN时间
    vehicle_no      VARCHAR(32), -- 车牌号
    driver_name     VARCHAR(64), -- 司机姓名
    operator        VARCHAR(64), -- 收货人
    receive_time    TIMESTAMP, -- 收货时间
    device_no       VARCHAR(64), -- 设备号
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_blind_receipt PRIMARY KEY (id),
    CONSTRAINT uk_wms_blind_receipt_no UNIQUE (blind_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_blind_receipt_sku ON wms_blind_receipt(sku_code);
CREATE INDEX idx_wms_blind_receipt_status ON wms_blind_receipt(status);
CREATE INDEX idx_wms_blind_receipt_warehouse ON wms_blind_receipt(warehouse_code);


-- 注释
ALTER TABLE wms_receipt_task COMMENT='收货任务表';
ALTER TABLE wms_receipt_task_detail COMMENT='收货任务明细表';
ALTER TABLE wms_receipt_record COMMENT='收货记录表';
ALTER TABLE wms_receipt_scan_log COMMENT='扫描收货日志表';
ALTER TABLE wms_blind_receipt COMMENT='盲收记录表';
