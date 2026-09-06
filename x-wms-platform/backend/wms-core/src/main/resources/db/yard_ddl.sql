-- ============================================================
-- X WMS 预约与月台管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 月台定义/预约单/车辆登记/月台使用记录
-- ============================================================

-- 1. 月台定义表
CREATE TABLE wms_dock (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    dock_code       VARCHAR(32)  NOT NULL,
    dock_name       VARCHAR(128),
    warehouse_code  VARCHAR(64)  NOT NULL,
    area_code       VARCHAR(64),            -- 所属库区
    dock_type       VARCHAR(16)  NOT NULL, -- INBOUND入库/OUTBOUND出库/BOTH两用/REVERSE退货
    status          VARCHAR(16)  DEFAULT 'IDLE', -- IDLE空闲/OCCUPIED占用/RESERVED已预约/MAINTENANCE维护/DISABLED禁用
    capacity        INT    DEFAULT 1, -- 同时容纳车辆数
    has_dock_leveler TINYINT(1)    DEFAULT 1, -- 是否有登车桥
    has_dock_shelter TINYINT(1)    DEFAULT 1, -- 是否有门封
    has_forklift    TINYINT(1)    DEFAULT 0, -- 是否配叉车
    max_vehicle_length INT(10,2),         -- 最大车长(米)
    max_vehicle_weight INT(10,2),         -- 最大载重(吨)
    sort_order      SMALLINT     DEFAULT 0,
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    deleted         TINYINT(1)     DEFAULT 0,
    CONSTRAINT pk_wms_dock PRIMARY KEY (id),
    CONSTRAINT uk_wms_dock_code UNIQUE (dock_code, warehouse_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_dock_wh ON wms_dock(warehouse_code);
CREATE INDEX idx_wms_dock_status ON wms_dock(status);

-- 2. 预约单表
CREATE TABLE wms_appointment (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    appoint_no      VARCHAR(64)  NOT NULL,
    appoint_type    VARCHAR(16)  NOT NULL, -- INBOUND入库/OUTBOUND出库/RETURN退货/TRANSFER调拨
    warehouse_code  VARCHAR(64)  NOT NULL,
    dock_id         BIGINT,              -- 分配的月台
    dock_code       VARCHAR(32),
    carrier_code    VARCHAR(64),            -- 承运商
    carrier_name    VARCHAR(256),
    driver_name     VARCHAR(64),
    driver_phone    VARCHAR(32),
    plate_no        VARCHAR(32),            -- 车牌号
    vehicle_type    VARCHAR(32),            -- 车型: 4.2米/6.8米/9.6米/13米/17.5米
    vehicle_length  INT(10,2),
    vehicle_weight  INT(10,2),
    contact_name    VARCHAR(64),
    contact_phone   VARCHAR(32),
    plan_arrive_time TIMESTAMP NOT NULL,     -- 预约到达时间
    plan_leave_time TIMESTAMP,               -- 预计离开时间
    actual_arrive_time TIMESTAMP,            -- 实际到达时间
    actual_leave_time TIMESTAMP,             -- 实际离开时间
    check_in_time   TIMESTAMP,               -- 签到时间
    check_out_time  TIMESTAMP,               -- 签退时间
    status          VARCHAR(32)  NOT NULL, -- PENDING待确认/CONFIRMED已确认/CANCELLED已取消/ARRIVED已到达/CHECKED_IN已签到/LOADING装卸中/COMPLETED已完成/NO_SHOW未到/OVERDUE逾期
    source_order_no VARCHAR(64),            -- 关联业务单号
    source_order_type VARCHAR(16),
    pallet_count    INT    DEFAULT 0, -- 托盘数
    package_count   INT    DEFAULT 0, -- 件数
    weight          DECIMAL(14,4),            -- 重量(吨)
    volume          DECIMAL(14,4),            -- 体积(方)
    priority        SMALLINT     DEFAULT 5,
    appoint_by      VARCHAR(64),
    confirm_by      VARCHAR(64),
    confirm_time    TIMESTAMP,
    cancel_reason   VARCHAR(512),
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    deleted         TINYINT(1)     DEFAULT 0,
    CONSTRAINT pk_wms_appointment PRIMARY KEY (id),
    CONSTRAINT uk_wms_appoint_no UNIQUE (appoint_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_appoint_status ON wms_appointment(status);
CREATE INDEX idx_wms_appoint_dock ON wms_appointment(dock_id);
CREATE INDEX idx_wms_appoint_arrive ON wms_appointment(plan_arrive_time);
CREATE INDEX idx_wms_appoint_wh ON wms_appointment(warehouse_code);

-- 3. 车辆登记表
CREATE TABLE wms_vehicle (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    plate_no        VARCHAR(32)  NOT NULL,
    vehicle_type    VARCHAR(32),
    vehicle_length  INT(10,2),
    vehicle_weight  INT(10,2),
    carrier_code    VARCHAR(64),
    carrier_name    VARCHAR(256),
    driver_name     VARCHAR(64),
    driver_phone    VARCHAR(32),
    driver_license  VARCHAR(64),            -- 驾驶证号
    status          VARCHAR(16)  DEFAULT 'ACTIVE', -- ACTIVE/DISABLED
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_vehicle PRIMARY KEY (id),
    CONSTRAINT uk_wms_vehicle_plate UNIQUE (plate_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_vehicle_carrier ON wms_vehicle(carrier_code);

-- 4. 月台使用记录表
CREATE TABLE wms_dock_usage (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    dock_id         BIGINT    NOT NULL,
    dock_code       VARCHAR(32)  NOT NULL,
    appointment_id  BIGINT,
    appoint_no      VARCHAR(64),
    warehouse_code  VARCHAR(64),
    plate_no        VARCHAR(32),
    carrier_name    VARCHAR(256),
    usage_type      VARCHAR(16),  -- INBOUND/OUTBOUND/RETURN
    occupy_start    TIMESTAMP,     -- 占用开始
    occupy_end      TIMESTAMP,     -- 占用结束
    duration_min    INT,    -- 占用时长(分钟)
    status          VARCHAR(16)  DEFAULT 'ACTIVE', -- ACTIVE/COMPLETED
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_dock_usage PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_dock_usage_dock ON wms_dock_usage(dock_id);
CREATE INDEX idx_wms_dock_usage_time ON wms_dock_usage(occupy_start);


-- 注释
ALTER TABLE wms_dock COMMENT='月台定义表';
ALTER TABLE wms_appointment COMMENT='预约单表';
ALTER TABLE wms_vehicle COMMENT='车辆登记表';
ALTER TABLE wms_dock_usage COMMENT='月台使用记录表';
