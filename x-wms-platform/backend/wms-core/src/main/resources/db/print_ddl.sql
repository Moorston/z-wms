-- ============================================================
-- X WMS 报表打印模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 打印模板/打印任务/打印机/打印队列
-- ============================================================

-- 1. 打印模板表
CREATE TABLE wms_print_template (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    template_code   VARCHAR(64)  NOT NULL,
    template_name   VARCHAR(128) NOT NULL,
    template_type   VARCHAR(32)  NOT NULL, -- PICKING拣货单/OUTBOUND出库单/INBOUND入库单/LABEL标签/RECEIPT收货单/PACKING装箱单/DELIVERY配送单
    paper_size      VARCHAR(16),  -- A4/A5/100x100/80x60等
    orientation     VARCHAR(8),   -- PORTRAIT纵向/LANDSCAPE横向
    template_content TEXT,         -- 模板内容(HTML/XML/Jasper)
    template_engine VARCHAR(16),  -- HTML/JASPER/FREEMARKER
    variables       TEXT,          -- 可用变量(JSON数组)
    default_printer VARCHAR(64),  -- 默认打印机
    copies          SMALLINT      DEFAULT 1, -- 默认份数
    enabled         TINYINT(1)      DEFAULT 1,
    owner_code_col  VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_print_template PRIMARY KEY (id),
    CONSTRAINT uk_wms_print_tpl_code UNIQUE (template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_print_tpl_type ON wms_print_template(template_type);

-- 2. 打印任务表
CREATE TABLE wms_print_task (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    task_no         VARCHAR(64)  NOT NULL,
    template_code   VARCHAR(64)  NOT NULL,
    template_name   VARCHAR(128),
    business_type   VARCHAR(32),  -- 业务类型: PICKING/OUTBOUND/INBOUND等
    business_no     VARCHAR(64),  -- 业务单号
    printer_code    VARCHAR(64),  -- 打印机编码
    printer_name    VARCHAR(128),
    copies          SMALLINT      DEFAULT 1,
    print_data      TEXT,          -- 打印数据(JSON)
    print_content   TEXT,          -- 渲染后的打印内容
    status          VARCHAR(16)   DEFAULT 'PENDING', -- PENDING待打印/PRINTING打印中/SUCCESS成功/FAILED失败/CANCELLED取消
    fail_reason     VARCHAR(512),
    print_time      TIMESTAMP,
    created_by      VARCHAR(64),
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_print_task PRIMARY KEY (id),
    CONSTRAINT uk_wms_print_task_no UNIQUE (task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_print_task_status ON wms_print_task(status);
CREATE INDEX idx_wms_print_task_biz ON wms_print_task(business_type, business_no);
CREATE INDEX idx_wms_print_task_time ON wms_print_task(created_time);

-- 3. 打印机表
CREATE TABLE wms_printer (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    printer_code    VARCHAR(64)  NOT NULL,
    printer_name    VARCHAR(128) NOT NULL,
    printer_type    VARCHAR(32),  -- LASER激光/INKJET喷墨/THERMAL热敏/LABEL标签
    printer_model   VARCHAR(128),
    ip_address      VARCHAR(64),
    port            SMALLINT,
    connection_type VARCHAR(16),  -- NETWORK网络/USB本地/SERIAL串口
    warehouse_code  VARCHAR(64),  -- 所属仓库
    location        VARCHAR(128), -- 位置描述
    paper_sizes     VARCHAR(256), -- 支持纸张大小(逗号分隔)
    status          VARCHAR(16)   DEFAULT 'ONLINE', -- ONLINE在线/OFFLINE离线/ERROR故障/MAINTENANCE维护
    last_heartbeat  TIMESTAMP,
    enabled         TINYINT(1)      DEFAULT 1,
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_printer PRIMARY KEY (id),
    CONSTRAINT uk_wms_printer_code UNIQUE (printer_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_printer_wh ON wms_printer(warehouse_code);
CREATE INDEX idx_wms_printer_status ON wms_printer(status);

-- 4. 打印队列表
CREATE TABLE wms_print_queue (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    queue_no        VARCHAR(64)  NOT NULL,
    printer_code    VARCHAR(64)  NOT NULL,
    task_id         BIGINT    NOT NULL,
    task_no         VARCHAR(64),
    priority        SMALLINT      DEFAULT 5, -- 优先级1-10, 1最高
    status          VARCHAR(16)   DEFAULT 'WAITING', -- WAITING等待/PRINTING打印中/DONE完成/FAILED失败
    retry_count     SMALLINT      DEFAULT 0,
    max_retry       SMALLINT      DEFAULT 3,
    created_time    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    start_time      TIMESTAMP,
    finish_time     TIMESTAMP,
    CONSTRAINT pk_wms_print_queue PRIMARY KEY (id),
    CONSTRAINT uk_wms_print_queue_no UNIQUE (queue_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_print_queue_printer ON wms_print_queue(printer_code, status);
CREATE INDEX idx_wms_print_queue_priority ON wms_print_queue(priority);


-- 注释
ALTER TABLE wms_print_template COMMENT='打印模板表';
ALTER TABLE wms_print_task COMMENT='打印任务表';
ALTER TABLE wms_printer COMMENT='打印机表';
ALTER TABLE wms_print_queue COMMENT='打印队列表';
