-- ============================================================
-- X WMS 设备管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 设备档案/设备状态/维护记录/使用记录
-- ============================================================

-- 1. 设备档案表
CREATE TABLE wms_equipment (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    equipment_code  VARCHAR(64)  NOT NULL,
    equipment_name  VARCHAR(128) NOT NULL,
    equipment_type  VARCHAR(32)  NOT NULL, -- FORKLIFT叉车/PALLET_JACK托盘车/CONVEYOR输送线/SORTER分拣机/AGV_AGV小车/RFID_READER RFID读写器/BARCODE_SCANNER扫码枪/PRINTER打印机/WEIGHING_SCALE电子秤/PDA手持终端/OTHER其他
    brand           VARCHAR(64),
    model           VARCHAR(64),
    serial_no       VARCHAR(64),
    warehouse_code  VARCHAR(64),
    area_code       VARCHAR(64),
    location_code   VARCHAR(64),            -- 当前位置
    status          VARCHAR(16)  DEFAULT 'IDLE', -- IDLE空闲/IN_USE使用中/MAINTENANCE维护中/FAULT故障/RETIRED报废
    run_status      VARCHAR(16),            -- RUNNING运行/STANDBY待机/STOPPED停止/ERROR错误
    purchase_date   DATE,
    warranty_end    DATE,
    last_maintain   DATE,
    next_maintain   DATE,
    maintain_cycle_days SMALLINT,           -- 维护周期(天)
    total_run_hours DECIMAL(14,2) DEFAULT 0,  -- 累计运行小时
    total_operations INT(14) DEFAULT 0,   -- 累计作业次数
    max_load        INT(10,2),            -- 最大载重(吨)
    max_speed       INT(10,2),            -- 最大速度
    battery_level   INT(5,2),             -- 电量(%)
    ip_address      VARCHAR(64),            -- IP地址(联网设备)
    wcs_device_id   VARCHAR(64),            -- WCS设备ID
    protocol        VARCHAR(32),            -- 通信协议: TCP/MQTT/OPC_UA/MODBUS
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    deleted         TINYINT(1)     DEFAULT 0,
    CONSTRAINT pk_wms_equipment PRIMARY KEY (id),
    CONSTRAINT uk_wms_equipment_code UNIQUE (equipment_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_equip_type ON wms_equipment(equipment_type);
CREATE INDEX idx_wms_equip_status ON wms_equipment(status);
CREATE INDEX idx_wms_equip_wh ON wms_equipment(warehouse_code);

-- 2. 设备状态实时表
CREATE TABLE wms_equipment_status (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    equipment_id    BIGINT    NOT NULL,
    equipment_code  VARCHAR(64)  NOT NULL,
    run_status      VARCHAR(16),            -- RUNNING/STANDBY/STOPPED/ERROR
    work_status     VARCHAR(16),            -- IDLE/WORKING/CHARGING/MAINTENANCE
    current_task_no VARCHAR(64),            -- 当前任务号
    current_location VARCHAR(64),           -- 当前位置
    target_location VARCHAR(64),            -- 目标位置
    battery_level   INT(5,2),             -- 电量
    speed           INT(10,2),            -- 当前速度
    load_weight     INT(10,2),            -- 当前载重
    error_code      VARCHAR(32),            -- 故障代码
    error_msg       VARCHAR(512),           -- 故障信息
    signal_strength INT(5,2),             -- 信号强度
    last_heartbeat  TIMESTAMP,               -- 最后心跳时间
    updated_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_equip_status PRIMARY KEY (id),
    CONSTRAINT uk_wms_equip_status_id UNIQUE (equipment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_equip_status_code ON wms_equipment_status(equipment_code);

-- 3. 设备维护记录表
CREATE TABLE wms_equipment_maintain (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    maintain_no     VARCHAR(64)  NOT NULL,
    equipment_id    BIGINT    NOT NULL,
    equipment_code  VARCHAR(64)  NOT NULL,
    maintain_type   VARCHAR(16)  NOT NULL, -- DAILY日常/PREVENTIVE预防性/CORRECTIVE故障修复/INSPECTION检查/UPGRADE升级
    maintain_status VARCHAR(16)  DEFAULT 'PENDING', -- PENDING待处理/PROCESSING处理中/COMPLETED完成/CANCELLED取消
    fault_desc      VARCHAR(512),           -- 故障描述
    maintain_desc   VARCHAR(1024),          -- 维护内容
    maintain_by     VARCHAR(64),            -- 维护人
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    duration_min    INT,              -- 维护时长(分钟)
    cost_amount     DECIMAL(14,4) DEFAULT 0,  -- 维护费用
    parts_replaced  TEXT,                    -- 更换配件(JSON)
    result          VARCHAR(16),            -- SUCCESS/FAILED/PARTIAL
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_equip_maintain PRIMARY KEY (id),
    CONSTRAINT uk_wms_maintain_no UNIQUE (maintain_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_maintain_equip ON wms_equipment_maintain(equipment_id);
CREATE INDEX idx_wms_maintain_status ON wms_equipment_maintain(maintain_status);

-- 4. 设备使用记录表
CREATE TABLE wms_equipment_usage (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    equipment_id    BIGINT  NOT NULL,
    equipment_code  VARCHAR(64) NOT NULL,
    task_type       VARCHAR(32),            -- INBOUND/OUTBOUND/MOVE/COUNT/VAS
    task_no         VARCHAR(64),            -- 关联任务号
    operator        VARCHAR(64),            -- 操作人
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    duration_min    INT,              -- 使用时长(分钟)
    operation_count INT DEFAULT 0,    -- 作业次数
    start_location  VARCHAR(64),
    end_location    VARCHAR(64),
    start_battery   INT(5,2),
    end_battery     INT(5,2),
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_equip_usage PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_usage_equip ON wms_equipment_usage(equipment_id);
CREATE INDEX idx_wms_usage_time ON wms_equipment_usage(start_time);


-- 注释
ALTER TABLE wms_equipment COMMENT='设备档案表';
ALTER TABLE wms_equipment_status COMMENT='设备状态实时表';
ALTER TABLE wms_equipment_maintain COMMENT='设备维护记录表';
ALTER TABLE wms_equipment_usage COMMENT='设备使用记录表';
