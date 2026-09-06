-- ============================================================
-- X WMS 打包管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 打包单/打包明细/包裹表/复核记录表
-- ============================================================

-- 1. 打包单表
CREATE TABLE wms_pack (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    pack_no         VARCHAR(64)  NOT NULL,
    outbound_no     VARCHAR(64)  NOT NULL,
    wave_no         VARCHAR(64),
    warehouse_code  VARCHAR(64),
    owner_code_col  VARCHAR(64),
    status          VARCHAR(32) DEFAULT 'CREATED', -- CREATED/CHECKING/CHECKED/PACKING/PACKED/DONE/CANCELLED
    total_qty       INT(18,4)  DEFAULT 0,
    checked_qty     INT(18,4)  DEFAULT 0,
    packed_qty      INT(18,4)  DEFAULT 0,
    package_count   INT    DEFAULT 0,
    total_weight    INT(18,4)  DEFAULT 0,
    total_volume    INT(18,4)  DEFAULT 0,
    checker         VARCHAR(64), -- 复核员
    packer          VARCHAR(64), -- 打包员
    check_time      TIMESTAMP,
    pack_time       TIMESTAMP,
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_pack PRIMARY KEY (id),
    CONSTRAINT uk_wms_pack_no UNIQUE (pack_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_pack_outbound ON wms_pack(outbound_no);
CREATE INDEX idx_wms_pack_wave ON wms_pack(wave_no);
CREATE INDEX idx_wms_pack_status ON wms_pack(status);

-- 2. 打包明细表
CREATE TABLE wms_pack_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    pack_no         VARCHAR(64)  NOT NULL,
    line_no         INT    NOT NULL,
    sku_code        VARCHAR(64)  NOT NULL,
    batch_no        VARCHAR(128),
    expected_qty    INT(18,4)  DEFAULT 0,
    checked_qty     INT(18,4)  DEFAULT 0,
    packed_qty      INT(18,4)  DEFAULT 0,
    difference_qty  INT(18,4)  DEFAULT 0, -- 差异数量
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING/CHECKED/PACKED
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_pack_detail PRIMARY KEY (id),
    CONSTRAINT uk_wms_pack_detail UNIQUE (pack_no, line_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_pack_detail_pack ON wms_pack_detail(pack_no);
CREATE INDEX idx_wms_pack_detail_sku ON wms_pack_detail(sku_code);

-- 3. 包裹表
CREATE TABLE wms_package (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    package_no      VARCHAR(64)  NOT NULL,
    pack_no         VARCHAR(64)  NOT NULL,
    outbound_no     VARCHAR(64),
    package_type    VARCHAR(32), -- BOX纸箱/BAG袋/PALLET托盘
    package_size    VARCHAR(64), -- 尺寸规格
    weight          INT(18,4), -- 重量
    volume          INT(18,4), -- 体积
    length          INT(18,4), -- 长
    width           INT(18,4), -- 宽
    height          INT(18,4), -- 高
    item_count      INT    DEFAULT 0, -- 商品件数
    sku_count       INT    DEFAULT 0, -- SKU数
    tracking_no     VARCHAR(128), -- 运单号
    carrier         VARCHAR(64), -- 承运商
    status          VARCHAR(32) DEFAULT 'CREATED', -- CREATED/PACKED/WEIGHED/LABELED/SHIPPED
    packer          VARCHAR(64),
    pack_time       TIMESTAMP,
    weigh_time      TIMESTAMP,
    label_time      TIMESTAMP,
    ship_time       TIMESTAMP,
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_package PRIMARY KEY (id),
    CONSTRAINT uk_wms_package_no UNIQUE (package_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_package_pack ON wms_package(pack_no);
CREATE INDEX idx_wms_package_outbound ON wms_package(outbound_no);
CREATE INDEX idx_wms_package_tracking ON wms_package(tracking_no);
CREATE INDEX idx_wms_package_status ON wms_package(status);

-- 4. 复核记录表
CREATE TABLE wms_check_record (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    record_no       VARCHAR(64)  NOT NULL,
    pack_no         VARCHAR(64)  NOT NULL,
    outbound_no     VARCHAR(64),
    sku_code        VARCHAR(64),
    batch_no        VARCHAR(128),
    expected_qty    INT(18,4),
    actual_qty      INT(18,4),
    difference_qty  INT(18,4),
    check_result    VARCHAR(32), -- PASS通过/FAIL失败/DIFFERENCE差异
    checker         VARCHAR(64),
    check_time      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_check_record PRIMARY KEY (id),
    CONSTRAINT uk_wms_check_record_no UNIQUE (record_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_check_pack ON wms_check_record(pack_no);
CREATE INDEX idx_wms_check_outbound ON wms_check_record(outbound_no);
CREATE INDEX idx_wms_check_result ON wms_check_record(check_result);


-- 注释
ALTER TABLE wms_pack COMMENT='打包单表';
ALTER TABLE wms_pack_detail COMMENT='打包明细表';
ALTER TABLE wms_package COMMENT='包裹表';
ALTER TABLE wms_check_record COMMENT='复核记录表';
