-- ============================================================
-- X WMS 数据归档模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 归档规则/归档任务/归档记录/归档批次
-- ============================================================

-- 1. 归档规则表
CREATE TABLE wms_archive_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL,
    rule_name       VARCHAR(128) NOT NULL,
    table_name      VARCHAR(64)  NOT NULL,    -- 源表名
    archive_table   VARCHAR(64),              -- 归档表名(默认源表_ARCH)
    archive_type    VARCHAR(16)  DEFAULT 'DATE', -- DATE按日期/STATUS按状态/ID按ID
    date_field      VARCHAR(64),              -- 日期字段(archive_type=DATE时)
    status_field    VARCHAR(64),              -- 状态字段(archive_type=STATUS时)
    status_values   VARCHAR(256),             -- 状态值(逗号分隔)
    retain_days     SMALLINT     DEFAULT 365, -- 保留天数
    batch_size      INT    DEFAULT 1000,-- 每批处理条数
    archive_mode    VARCHAR(16)  DEFAULT 'MOVE', -- MOVE移动/COPY复制/DELETE仅删除
    storage_type    VARCHAR(16)  DEFAULT 'DATABASE', -- DATABASE数据库/CSV文件/OSS对象存储
    oss_path        VARCHAR(256),             -- OSS存储路径
    enabled         TINYINT(1)     DEFAULT 1,
    cron_expression VARCHAR(64),              -- 定时执行cron
    last_execute_time TIMESTAMP,
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_archive_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_archive_rule_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_archive_rule_table ON wms_archive_rule(table_name);

-- 2. 归档任务表
CREATE TABLE wms_archive_task (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    task_no         VARCHAR(64)  NOT NULL,
    rule_id         BIGINT,
    rule_code       VARCHAR(64),
    table_name      VARCHAR(64),
    archive_type    VARCHAR(16),
    status          VARCHAR(16)  DEFAULT 'PENDING', -- PENDING待执行/RUNNING执行中/COMPLETED完成/FAILED失败/CANCELLED取消
    total_count     INT(14)    DEFAULT 0,  -- 总记录数
    archived_count  INT(14)    DEFAULT 0,  -- 已归档数
    failed_count    INT(14)    DEFAULT 0,  -- 失败数
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    duration_min    INT,               -- 耗时(分钟)
    error_msg       TEXT,
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_archive_task PRIMARY KEY (id),
    CONSTRAINT uk_wms_archive_task_no UNIQUE (task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_archive_task_status ON wms_archive_task(status);
CREATE INDEX idx_wms_archive_task_rule ON wms_archive_task(rule_id);

-- 3. 归档批次表
CREATE TABLE wms_archive_batch (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    batch_no        VARCHAR(64)  NOT NULL,
    task_id         BIGINT,
    task_no         VARCHAR(64),
    batch_index     INT,               -- 批次序号
    start_id        BIGINT,               -- 起始ID
    end_id          BIGINT,               -- 结束ID
    record_count    INT    DEFAULT 0,  -- 本批记录数
    status          VARCHAR(16)  DEFAULT 'PENDING', -- PENDING/RUNNING/COMPLETED/FAILED
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    error_msg       VARCHAR(1024),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_archive_batch PRIMARY KEY (id),
    CONSTRAINT uk_wms_archive_batch_no UNIQUE (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_archive_batch_task ON wms_archive_batch(task_id);

-- 4. 归档记录表 (归档数据索引, 便于查询归档数据)
CREATE TABLE wms_archive_record (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    batch_id        BIGINT,
    batch_no        VARCHAR(64),
    source_table    VARCHAR(64),
    source_id       BIGINT,               -- 源记录ID
    source_no       VARCHAR(64),             -- 源业务单号
    archive_table   VARCHAR(64),             -- 归档表
    archive_time    TIMESTAMP,
    archive_by      VARCHAR(64),
    data_summary    VARCHAR(512),            -- 数据摘要
    owner_code_col  VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_archive_record PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_archive_rec_source ON wms_archive_record(source_table, source_id);
CREATE INDEX idx_wms_archive_rec_batch ON wms_archive_record(batch_id);


-- 注释
ALTER TABLE wms_archive_rule COMMENT='归档规则表';
ALTER TABLE wms_archive_task COMMENT='归档任务表';
ALTER TABLE wms_archive_batch COMMENT='归档批次表';
ALTER TABLE wms_archive_record COMMENT='归档记录表';
