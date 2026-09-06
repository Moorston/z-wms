-- ============================================================
-- X WMS 整理收货模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 整理收货单表/整理收货明细表/整理收货箱表
-- 业务场景: 服装行业配比箱拆箱整理为独色独码的单一SKU箱
-- ============================================================

-- 1. 整理收货单表
CREATE TABLE wms_sorting_receipt (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    receipt_no      VARCHAR(64)  NOT NULL, -- 整理收货单号
    asn_no          VARCHAR(64), -- ASN号
    inbound_no      VARCHAR(64), -- 入库单号
    owner_code      VARCHAR(64), -- 货主编码
    warehouse_code  VARCHAR(64), -- 仓库编码
    group_no        VARCHAR(64), -- 配比箱组号
    style_code      VARCHAR(64), -- 款式编码
    color_code      VARCHAR(64), -- 颜色编码
    box_count       INT    DEFAULT 0, -- 配比箱数量
    unpacked_box_count INT DEFAULT 0, -- 已拆箱数量
    total_qty       INT(18,4)  DEFAULT 0, -- 总数量
    sorted_qty      INT(18,4)  DEFAULT 0, -- 已整理数量
    status          VARCHAR(32)  DEFAULT 'PENDING', -- 状态：PENDING/SORTING/COMPLETED/CANCELLED
    location_code   VARCHAR(64), -- 收货库位
    operator        VARCHAR(64), -- 操作人
    start_time      TIMESTAMP, -- 开始时间
    finish_time     TIMESTAMP, -- 完成时间
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_sorting_receipt PRIMARY KEY (id),
    CONSTRAINT uk_wms_sorting_receipt_no UNIQUE (receipt_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_sorting_receipt_asn ON wms_sorting_receipt(asn_no);
CREATE INDEX idx_wms_sorting_receipt_style ON wms_sorting_receipt(style_code);
CREATE INDEX idx_wms_sorting_receipt_status ON wms_sorting_receipt(status);

-- 2. 整理收货明细表
CREATE TABLE wms_sorting_receipt_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    receipt_no      VARCHAR(64)  NOT NULL, -- 整理收货单号
    line_no         INT    NOT NULL, -- 行号
    sku_code        VARCHAR(64)  NOT NULL, -- 商品编码（独色独码SKU）
    sku_name        VARCHAR(256), -- 商品名称
    style_code      VARCHAR(64), -- 款式编码
    color_code      VARCHAR(64), -- 颜色编码
    size_code       VARCHAR(32), -- 尺码
    ratio_qty       INT(18,4)  DEFAULT 0, -- 配比数量（一个配比箱中该SKU的数量）
    expected_qty    INT(18,4)  DEFAULT 0, -- 预期数量（配比箱数 * 配比数量）
    scanned_qty     INT(18,4)  DEFAULT 0, -- 已扫描数量
    received_qty    INT(18,4)  DEFAULT 0, -- 已收货数量
    status          VARCHAR(32)  DEFAULT 'PENDING', -- 状态：PENDING/SCANNING/COMPLETED
    is_full_box     VARCHAR(1)   DEFAULT 'N', -- 是否满箱：Y/N
    full_box_qty    INT(18,4), -- 满箱数量（包装箱标准数量）
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_sorting_detail PRIMARY KEY (id),
    CONSTRAINT uk_wms_sorting_detail UNIQUE (receipt_no, line_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_sorting_detail_receipt ON wms_sorting_receipt_detail(receipt_no);
CREATE INDEX idx_wms_sorting_detail_sku ON wms_sorting_receipt_detail(sku_code);

-- 3. 整理收货箱表
CREATE TABLE wms_sorting_receipt_box (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    box_no          VARCHAR(64)  NOT NULL, -- 箱号
    receipt_no      VARCHAR(64), -- 整理收货单号
    sku_code        VARCHAR(64), -- 商品编码
    sku_name        VARCHAR(256), -- 商品名称
    style_code      VARCHAR(64), -- 款式编码
    color_code      VARCHAR(64), -- 颜色编码
    size_code       VARCHAR(32), -- 尺码
    box_qty         INT(18,4)  DEFAULT 0, -- 箱内数量
    full_box_qty    INT(18,4), -- 满箱数量
    is_full         VARCHAR(1)   DEFAULT 'N', -- 是否满箱：Y/N
    location_code   VARCHAR(64), -- 库位编码
    lpn_no          VARCHAR(64), -- 托盘号/LPN
    status          VARCHAR(32)  DEFAULT 'CREATED', -- 状态：CREATED/RECEIVED/PUTAWAYED
    operator        VARCHAR(64), -- 操作人
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    received_time   TIMESTAMP, -- 收货时间
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_sorting_box PRIMARY KEY (id),
    CONSTRAINT uk_wms_sorting_box_no UNIQUE (box_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_sorting_box_receipt ON wms_sorting_receipt_box(receipt_no);
CREATE INDEX idx_wms_sorting_box_sku ON wms_sorting_receipt_box(sku_code);
CREATE INDEX idx_wms_sorting_box_status ON wms_sorting_receipt_box(status);


-- 注释
ALTER TABLE wms_sorting_receipt COMMENT='整理收货单表（服装配比箱拆箱）';
ALTER TABLE wms_sorting_receipt_detail COMMENT='整理收货明细表';
ALTER TABLE wms_sorting_receipt_box COMMENT='整理收货箱表';
