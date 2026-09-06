-- ============================================================
-- X WMS RF作业管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: RF任务/RF作业记录/RF用户会话/RF菜单配置
-- ============================================================

-- 1. RF任务表
CREATE TABLE wms_rf_task (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    task_no         VARCHAR(64)  NOT NULL,
    task_type       VARCHAR(32)  NOT NULL, -- RECEIVING收货/PUTAWAY上架/PICKING拣货/CHECKING复核/PACKING打包/MOVING移库/COUNTING盘点/REPLENISH补货/VAS增值/RETURN退货/SORTING分拣
    priority        VARCHAR(8)   DEFAULT 'NORMAL', -- LOW/NORMAL/HIGH/URGENT
    status          VARCHAR(16)  DEFAULT 'PENDING', -- PENDING待领取/ASSIGNED已分配/IN_PROGRESS进行中/PAUSED已暂停/COMPLETED已完成/CANCELLED已取消
    warehouse_code  VARCHAR(64),
    area_code       VARCHAR(64),
    source_no       VARCHAR(64),            -- 关联业务单号
    source_type     VARCHAR(32),            -- 关联业务类型
    sku             VARCHAR(64),
    product_name    VARCHAR(256),
    batch_no        VARCHAR(64),
    location_from   VARCHAR(64),
    location_to     VARCHAR(64),
    quantity        DECIMAL(14,4)  DEFAULT 0,
    quantity_done   DECIMAL(14,4)  DEFAULT 0,
    assigned_to     VARCHAR(64),            -- 分配给(RF用户ID)
    assigned_name   VARCHAR(64),
    pda_device_id   VARCHAR(64),
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    duration_min    INT,
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_rf_task PRIMARY KEY (id),
    CONSTRAINT uk_wms_rf_task_no UNIQUE (task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_rf_task_type ON wms_rf_task(task_type);
CREATE INDEX idx_wms_rf_task_status ON wms_rf_task(status);
CREATE INDEX idx_wms_rf_task_assignee ON wms_rf_task(assigned_to);

-- 2. RF作业记录表
CREATE TABLE wms_rf_work_log (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    log_no          VARCHAR(64)  NOT NULL,
    task_id         BIGINT,
    task_no         VARCHAR(64),
    task_type       VARCHAR(32),
    user_id         VARCHAR(64),
    user_name       VARCHAR(64),
    pda_device_id   VARCHAR(64),
    action          VARCHAR(32)  NOT NULL, -- LOGIN登录/LOGOUT登登/ACCEPT领取/START开始/SCAN扫码/CONFIRM确认/PAUSE暂停/RESUME继续/COMPLETE完成/CANCEL取消/EXCEPTION异常
    location_from   VARCHAR(64),
    location_to     VARCHAR(64),
    sku             VARCHAR(64),
    batch_no        VARCHAR(64),
    barcode         VARCHAR(128),
    quantity        DECIMAL(14,4),
    before_status   VARCHAR(16),
    after_status    VARCHAR(16),
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_rf_work_log PRIMARY KEY (id),
    CONSTRAINT uk_wms_rf_log_no UNIQUE (log_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_rf_log_task ON wms_rf_work_log(task_id);
CREATE INDEX idx_wms_rf_log_user ON wms_rf_work_log(user_id);
CREATE INDEX idx_wms_rf_log_time ON wms_rf_work_log(created_time);

-- 3. RF用户会话表
CREATE TABLE wms_rf_session (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    session_id      VARCHAR(128) NOT NULL,
    user_id         VARCHAR(64)  NOT NULL,
    user_name       VARCHAR(64),
    pda_device_id   VARCHAR(64),
    warehouse_code  VARCHAR(64),
    login_time      TIMESTAMP,
    last_active_time TIMESTAMP,
    logout_time     TIMESTAMP,
    status          VARCHAR(16)  DEFAULT 'ACTIVE', -- ACTIVE活跃/IDLE空闲/EXPIRED过期/LOGOUT登出
    current_task_id BIGINT,
    current_menu    VARCHAR(64),
    ip_address      VARCHAR(64),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_rf_session PRIMARY KEY (id),
    CONSTRAINT uk_wms_rf_session_id UNIQUE (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_rf_session_user ON wms_rf_session(user_id);
CREATE INDEX idx_wms_rf_session_status ON wms_rf_session(status);

-- 4. RF菜单配置表
CREATE TABLE wms_rf_menu (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    menu_code       VARCHAR(64)  NOT NULL,
    menu_name       VARCHAR(128) NOT NULL,
    parent_code     VARCHAR(64),
    menu_type       VARCHAR(16),  -- MENU菜单/FUNCTION功能
    task_type       VARCHAR(32),  -- 关联任务类型
    icon            VARCHAR(64),
    sort_order      SMALLINT      DEFAULT 0,
    enabled         TINYINT(1)      DEFAULT 1,
    permission_code VARCHAR(64),  -- 权限编码
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_rf_menu PRIMARY KEY (id),
    CONSTRAINT uk_wms_rf_menu_code UNIQUE (menu_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_rf_menu_parent ON wms_rf_menu(parent_code);


-- 注释
ALTER TABLE wms_rf_task COMMENT='RF任务表';
ALTER TABLE wms_rf_work_log COMMENT='RF作业记录表';
ALTER TABLE wms_rf_session COMMENT='RF用户会话表';
ALTER TABLE wms_rf_menu COMMENT='RF菜单配置表';
