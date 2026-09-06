-- ============================================================
-- X WMS 库存标签/条码管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 标签模板/条码规则/标签任务/条码记录
-- ============================================================

-- 1. 标签模板表
CREATE TABLE wms_label_template (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    template_code   VARCHAR(64)  NOT NULL, -- 模板编码
    template_name   VARCHAR(128) NOT NULL, -- 模板名称
    template_type   VARCHAR(32)  NOT NULL, -- 模板类型: SKU商品/LOCATION库位/BATCH批次/CONTAINER容器/ORDER订单/PALLET托盘
    warehouse_code  VARCHAR(64), -- 仓库
    owner_code      VARCHAR(64), -- 货主
    width           INT(10,2), -- 标签宽度(mm)
    height          INT(10,2), -- 标签高度(mm)
    orientation     VARCHAR(16) DEFAULT 'PORTRAIT', -- 方向: PORTRAIT竖版/LANDSCAPE横版
    template_content TEXT, -- 模板内容(JSON/ZPL)
    template_format VARCHAR(16) DEFAULT 'ZPL', -- 模板格式: ZPL/EPL/HTML/PDF
    barcode_type    VARCHAR(32) DEFAULT 'CODE128', -- 条码类型: CODE128/CODE39/QRCODE/EAN13
    barcode_position VARCHAR(32), -- 条码位置: TOP_LEFT/TOP_RIGHT/CENTER/BOTTOM_LEFT/BOTTOM_RIGHT
    print_count     SMALLINT    DEFAULT 1, -- 默认打印份数
    default_printer VARCHAR(64), -- 默认打印机
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_label_template PRIMARY KEY (id),
    CONSTRAINT uk_wms_label_template_code UNIQUE (template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_label_template_type ON wms_label_template(template_type);
CREATE INDEX idx_wms_label_template_wh ON wms_label_template(warehouse_code);

-- 2. 条码规则表
CREATE TABLE wms_barcode_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL, -- 规则编码
    rule_name       VARCHAR(128) NOT NULL, -- 规则名称
    barcode_type    VARCHAR(32)  NOT NULL, -- 条码类型: SKU/LOCATION/BATCH/CONTAINER/ORDER/PALLET/SERIAL
    warehouse_code  VARCHAR(64), -- 仓库
    owner_code      VARCHAR(64), -- 货主
    prefix          VARCHAR(32), -- 前缀
    suffix          VARCHAR(32), -- 后缀
    sequence_length SMALLINT    DEFAULT 8, -- 序列号长度
    sequence_start  BIGINT   DEFAULT 1, -- 序列号起始值
    sequence_current BIGINT  DEFAULT 1, -- 当前序列号
    date_format     VARCHAR(32), -- 日期格式: yyyyMMdd/yyMMdd
    include_date    VARCHAR(8)  DEFAULT 'N', -- 是否包含日期: Y/N
    include_owner   VARCHAR(8)  DEFAULT 'N', -- 是否包含货主: Y/N
    include_warehouse VARCHAR(8) DEFAULT 'N', -- 是否包含仓库: Y/N
    checksum_type   VARCHAR(16), -- 校验码类型: MOD10/MOD11/NONE
    barcode_format  VARCHAR(32) DEFAULT 'CODE128', -- 条码格式: CODE128/CODE39/QRCODE/EAN13
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_barcode_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_barcode_rule_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_barcode_rule_type ON wms_barcode_rule(barcode_type);
CREATE INDEX idx_wms_barcode_rule_wh ON wms_barcode_rule(warehouse_code);

-- 3. 标签任务表
CREATE TABLE wms_label_task (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    task_id         VARCHAR(64)  NOT NULL, -- 任务ID
    task_name       VARCHAR(128), -- 任务名称
    template_code   VARCHAR(64)  NOT NULL, -- 关联模板
    warehouse_code  VARCHAR(64), -- 仓库
    owner_code      VARCHAR(64), -- 货主
    biz_type        VARCHAR(32)  NOT NULL, -- 业务类型: INBOUND入库/OUTBOUND出库/TRANSFER调拨/INVENTORY盘点/ADJUST调整/MOVE移库
    biz_no          VARCHAR(64), -- 业务单号
    total_count     INT   DEFAULT 0, -- 总标签数
    printed_count   INT   DEFAULT 0, -- 已打印数
    failed_count    INT   DEFAULT 0, -- 失败数
    printer         VARCHAR(64), -- 打印机
    print_mode      VARCHAR(16) DEFAULT 'BATCH', -- 打印模式: SINGLE单张/BATCH批量/QUEUE队列
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING待打印/PRINTING打印中/COMPLETED已完成/FAILED失败/CANCELLED已取消
    error_message   TEXT, -- 错误信息
    operator        VARCHAR(64), -- 操作人
    start_time      TIMESTAMP, -- 开始时间
    end_time        TIMESTAMP, -- 结束时间
    duration_ms     BIGINT, -- 耗时(毫秒)
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_label_task PRIMARY KEY (id),
    CONSTRAINT uk_wms_label_task_id UNIQUE (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_label_task_template ON wms_label_task(template_code);
CREATE INDEX idx_wms_label_task_status ON wms_label_task(status);
CREATE INDEX idx_wms_label_task_wh ON wms_label_task(warehouse_code);

-- 4. 条码记录表
CREATE TABLE wms_barcode_record (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    record_id       VARCHAR(64)  NOT NULL, -- 记录ID
    barcode         VARCHAR(128) NOT NULL, -- 条码
    barcode_type    VARCHAR(32)  NOT NULL, -- 条码类型: SKU/LOCATION/BATCH/CONTAINER/ORDER/PALLET/SERIAL
    rule_code       VARCHAR(64), -- 生成规则
    warehouse_code  VARCHAR(64), -- 仓库
    owner_code      VARCHAR(64), -- 货主
    biz_key         VARCHAR(128), -- 业务主键(SKU/库位/批次等)
    biz_type        VARCHAR(32), -- 业务类型
    sequence_no     BIGINT, -- 序列号
    generate_time   TIMESTAMP, -- 生成时间
    print_count     INT   DEFAULT 0, -- 打印次数
    last_print_time TIMESTAMP, -- 最后打印时间
    scan_count      INT   DEFAULT 0, -- 扫描次数
    last_scan_time  TIMESTAMP, -- 最后扫描时间
    status          VARCHAR(32) DEFAULT 'ACTIVE', -- ACTIVE活跃/USED已使用/EXPIRED已过期/DISABLED已禁用
    expire_time     TIMESTAMP, -- 过期时间
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_barcode_record PRIMARY KEY (id),
    CONSTRAINT uk_wms_barcode_record_id UNIQUE (record_id),
    CONSTRAINT uk_wms_barcode UNIQUE (barcode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_barcode_record_type ON wms_barcode_record(barcode_type);
CREATE INDEX idx_wms_barcode_record_biz ON wms_barcode_record(biz_key);
CREATE INDEX idx_wms_barcode_record_wh ON wms_barcode_record(warehouse_code);


-- 注释
ALTER TABLE wms_label_template COMMENT='标签模板表';
ALTER TABLE wms_barcode_rule COMMENT='条码规则表';
ALTER TABLE wms_label_task COMMENT='标签任务表';
ALTER TABLE wms_barcode_record COMMENT='条码记录表';
