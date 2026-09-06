-- ============================================================
-- X WMS 库存导入导出管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 导入任务/导入记录/导出任务/导出记录
-- ============================================================

-- 1. 导入任务表
CREATE TABLE wms_import_task (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    task_id         VARCHAR(64)  NOT NULL, -- 任务ID
    task_name       VARCHAR(128) NOT NULL, -- 任务名称
    import_type     VARCHAR(32)  NOT NULL, -- 导入类型: INVENTORY库存/PRODUCT产品/LOCATION库位/OWNER货主/ORDER订单/BATCH批次
    warehouse_code  VARCHAR(64), -- 仓库
    owner_code      VARCHAR(64), -- 货主
    file_name       VARCHAR(256) NOT NULL, -- 文件名
    file_path       VARCHAR(512), -- 文件路径
    file_size       BIGINT, -- 文件大小(字节)
    file_format     VARCHAR(16) DEFAULT 'EXCEL', -- 文件格式: EXCEL/CSV
    total_count     INT   DEFAULT 0, -- 总记录数
    success_count   INT   DEFAULT 0, -- 成功数
    fail_count      INT   DEFAULT 0, -- 失败数
    skip_count      INT   DEFAULT 0, -- 跳过数
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING待处理/PROCESSING处理中/COMPLETED已完成/FAILED失败/CANCELLED已取消
    error_message   TEXT, -- 错误信息
    operator        VARCHAR(64), -- 操作人
    start_time      TIMESTAMP, -- 开始时间
    end_time        TIMESTAMP, -- 结束时间
    duration_ms     BIGINT, -- 耗时(毫秒)
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_import_task PRIMARY KEY (id),
    CONSTRAINT uk_wms_import_task_id UNIQUE (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_import_task_type ON wms_import_task(import_type);
CREATE INDEX idx_wms_import_task_status ON wms_import_task(status);
CREATE INDEX idx_wms_import_task_wh ON wms_import_task(warehouse_code);

-- 2. 导入记录表
CREATE TABLE wms_import_record (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    record_id       VARCHAR(64)  NOT NULL, -- 记录ID
    task_id         VARCHAR(64)  NOT NULL, -- 关联任务
    row_no          INT   NOT NULL, -- 行号
    row_data        TEXT, -- 行数据(JSON)
    import_status   VARCHAR(32) DEFAULT 'PENDING', -- PENDING待处理/SUCCESS成功/FAILED失败/SKIPPED跳过
    error_code      VARCHAR(64), -- 错误码
    error_message   VARCHAR(1024), -- 错误信息
    biz_key         VARCHAR(256), -- 业务主键
    retry_count     SMALLINT    DEFAULT 0, -- 重试次数
    operator        VARCHAR(64), -- 操作人
    process_time    TIMESTAMP, -- 处理时间
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_import_record PRIMARY KEY (id),
    CONSTRAINT uk_wms_import_record_id UNIQUE (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_import_record_task ON wms_import_record(task_id);
CREATE INDEX idx_wms_import_record_status ON wms_import_record(import_status);

-- 3. 导出任务表
CREATE TABLE wms_export_task (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    task_id         VARCHAR(64)  NOT NULL, -- 任务ID
    task_name       VARCHAR(128) NOT NULL, -- 任务名称
    export_type     VARCHAR(32)  NOT NULL, -- 导出类型: INVENTORY库存/PRODUCT产品/LOCATION库位/OWNER货主/ORDER订单/BATCH批次/REPORT报表
    warehouse_code  VARCHAR(64), -- 仓库
    owner_code      VARCHAR(64), -- 货主
    query_params    TEXT, -- 查询参数(JSON)
    export_columns  TEXT, -- 导出列配置(JSON)
    file_name       VARCHAR(256), -- 文件名
    file_path       VARCHAR(512), -- 文件路径
    file_size       BIGINT, -- 文件大小(字节)
    file_format     VARCHAR(16) DEFAULT 'EXCEL', -- 文件格式: EXCEL/CSV/PDF
    total_count     INT   DEFAULT 0, -- 总记录数
    exported_count  INT   DEFAULT 0, -- 已导出数
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING待处理/PROCESSING处理中/COMPLETED已完成/FAILED失败/CANCELLED已取消
    error_message   TEXT, -- 错误信息
    operator        VARCHAR(64), -- 操作人
    start_time      TIMESTAMP, -- 开始时间
    end_time        TIMESTAMP, -- 结束时间
    duration_ms     BIGINT, -- 耗时(毫秒)
    expire_time     TIMESTAMP, -- 过期时间
    download_count  INT   DEFAULT 0, -- 下载次数
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_export_task PRIMARY KEY (id),
    CONSTRAINT uk_wms_export_task_id UNIQUE (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_export_task_type ON wms_export_task(export_type);
CREATE INDEX idx_wms_export_task_status ON wms_export_task(status);
CREATE INDEX idx_wms_export_task_wh ON wms_export_task(warehouse_code);

-- 4. 导出记录表
CREATE TABLE wms_export_record (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    record_id       VARCHAR(64)  NOT NULL, -- 记录ID
    task_id         VARCHAR(64)  NOT NULL, -- 关联任务
    row_no          INT   NOT NULL, -- 行号
    row_data        TEXT, -- 行数据(JSON)
    export_status   VARCHAR(32) DEFAULT 'PENDING', -- PENDING待处理/EXPORTED已导出/FAILED失败
    error_message   VARCHAR(1024), -- 错误信息
    biz_key         VARCHAR(256), -- 业务主键
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_export_record PRIMARY KEY (id),
    CONSTRAINT uk_wms_export_record_id UNIQUE (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_export_record_task ON wms_export_record(task_id);
CREATE INDEX idx_wms_export_record_status ON wms_export_record(export_status);


-- 注释
ALTER TABLE wms_import_task COMMENT='导入任务表';
ALTER TABLE wms_import_record COMMENT='导入记录表';
ALTER TABLE wms_export_task COMMENT='导出任务表';
ALTER TABLE wms_export_record COMMENT='导出记录表';
