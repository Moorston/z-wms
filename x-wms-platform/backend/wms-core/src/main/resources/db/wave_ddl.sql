-- ============================================================
-- X WMS 波次管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 波次单/波次明细/波次拣货任务/波次路径
-- ============================================================

-- 1. 波次单表
CREATE TABLE wms_wave (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    wave_no         VARCHAR(64)  NOT NULL,
    wave_type       VARCHAR(32)  NOT NULL, -- NORMAL普通/URGENT紧急/BULK大宗/COLD冷链
    warehouse_code  VARCHAR(64),
    owner_code_col  VARCHAR(64),
    pick_mode       VARCHAR(32), -- PICK_BY_ORDER摘果式/PICK_BY_SKU播种式/PICK_BY_WAVE波次拣货
    strategy_code   VARCHAR(64), -- 波次策略编码
    order_count     INT    DEFAULT 0, -- 订单数
    sku_count       INT    DEFAULT 0, -- SKU数
    total_qty       INT(18,4)  DEFAULT 0, -- 总数量
    picked_qty      INT(18,4)  DEFAULT 0, -- 已拣数量
    status          VARCHAR(32) DEFAULT 'CREATED', -- CREATED/ALLOCATED/PICKING/PICKED/PACKING/DONE/CANCELLED
    priority        SMALLINT     DEFAULT 5, -- 优先级 1-10
    assign_picker   VARCHAR(64), -- 指定拣货员
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_wave PRIMARY KEY (id),
    CONSTRAINT uk_wms_wave_no UNIQUE (wave_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_wave_status ON wms_wave(status);
CREATE INDEX idx_wms_wave_warehouse ON wms_wave(warehouse_code);

-- 2. 波次明细表（波次关联的出库单）
CREATE TABLE wms_wave_detail (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    wave_no         VARCHAR(64)  NOT NULL,
    outbound_no     VARCHAR(64)  NOT NULL,
    customer_code   VARCHAR(64),
    carrier         VARCHAR(64),
    priority        SMALLINT,
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING/PICKING/PICKED/PACKED/SHIPPED
    picked_qty      INT(18,4)  DEFAULT 0,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_wave_detail PRIMARY KEY (id),
    CONSTRAINT uk_wms_wave_detail UNIQUE (wave_no, outbound_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_wave_detail_wave ON wms_wave_detail(wave_no);
CREATE INDEX idx_wms_wave_detail_outbound ON wms_wave_detail(outbound_no);

-- 3. 波次拣货任务表（按SKU+库位拆分的拣货任务）
CREATE TABLE wms_wave_pick_task (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    task_no         VARCHAR(64)  NOT NULL,
    wave_no         VARCHAR(64)  NOT NULL,
    sku_code        VARCHAR(64)  NOT NULL,
    batch_no        VARCHAR(128),
    from_location   VARCHAR(64)  NOT NULL,
    pick_qty        INT(18,4)  NOT NULL,
    picked_qty      INT(18,4)  DEFAULT 0,
    difference_qty  INT(18,4)  DEFAULT 0,
    picker          VARCHAR(64),
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING/PICKING/DONE/EXCEPTION
    path_order      INT, -- 路径顺序
    pick_time       TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_wave_pick PRIMARY KEY (id),
    CONSTRAINT uk_wms_wave_pick_no UNIQUE (task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_wave_pick_wave ON wms_wave_pick_task(wave_no);
CREATE INDEX idx_wms_wave_pick_sku ON wms_wave_pick_task(sku_code);
CREATE INDEX idx_wms_wave_pick_location ON wms_wave_pick_task(from_location);
CREATE INDEX idx_wms_wave_pick_status ON wms_wave_pick_task(status);

-- 4. 波次路径表（拣货路径规划）
CREATE TABLE wms_wave_path (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    wave_no         VARCHAR(64)  NOT NULL,
    path_order      INT    NOT NULL, -- 路径顺序
    location_code   VARCHAR(64)  NOT NULL,
    location_x      INT(10,4), -- 库位X坐标
    location_y      INT(10,4), -- 库位Y坐标
    location_z      INT(10,4), -- 库位Z坐标
    distance        INT(10,4), -- 到下一库位距离
    sku_count       INT    DEFAULT 0, -- 该库位待拣SKU数
    total_qty       INT(18,4)  DEFAULT 0, -- 该库位待拣总数量
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_wave_path PRIMARY KEY (id),
    CONSTRAINT uk_wms_wave_path UNIQUE (wave_no, path_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_wave_path_wave ON wms_wave_path(wave_no);


-- 注释
ALTER TABLE wms_wave COMMENT='波次单表';
ALTER TABLE wms_wave_detail COMMENT='波次明细表（关联出库单）';
ALTER TABLE wms_wave_pick_task COMMENT='波次拣货任务表（按SKU+库位拆分）';
ALTER TABLE wms_wave_path COMMENT='波次路径表（拣货路径规划）';
