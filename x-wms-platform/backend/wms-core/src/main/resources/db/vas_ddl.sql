-- ============================================================
-- X WMS VAS增值服务模块 DDL
-- 数据库: MySQL 8.x
-- 包含: VAS服务定义/VAS工单/VAS工单明细/VAS物料消耗
-- ============================================================

-- 1. VAS服务定义表
CREATE TABLE wms_vas_service (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    service_code    VARCHAR(64)  NOT NULL,
    service_name    VARCHAR(128) NOT NULL,
    service_type    VARCHAR(32)  NOT NULL, -- LABELING贴标/REPACK重新包装/KITTING组合套装/SPLIT拆零/ASSEMBLY组装/INSPECTION质检加工/CUSTOM定制/OTHER其他
    category        VARCHAR(32),            -- 服务分类: PACKAGING包装/LABELING标签/PROCESSING加工/QUALITY质检/OTHER
    description     VARCHAR(512),
    unit            VARCHAR(16),            -- 计费单位: PIECE件/ORDER单/LOT批/HOUR小时/KG公斤
    unit_price      DECIMAL(14,4)  DEFAULT 0, -- 单价
    cost_price      DECIMAL(14,4)  DEFAULT 0, -- 成本价
    standard_time   INT(10,2),            -- 标准工时(分钟/单位)
    need_material   TINYINT(1)     DEFAULT 0, -- 是否需要物料
    material_list   TEXT,                    -- 所需物料清单(JSON)
    need_equipment  TINYINT(1)     DEFAULT 0, -- 是否需要设备
    equipment_list  TEXT,                    -- 所需设备清单(JSON)
    skill_required  VARCHAR(128),           -- 所需技能
    status          VARCHAR(16)  DEFAULT 'ENABLED', -- ENABLED/DISABLED
    owner_code      VARCHAR(64),
    warehouse_code  VARCHAR(64),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    deleted         TINYINT(1)     DEFAULT 0,
    CONSTRAINT pk_wms_vas_service PRIMARY KEY (id),
    CONSTRAINT uk_wms_vas_service_code UNIQUE (service_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_vas_svc_type ON wms_vas_service(service_type);

-- 2. VAS工单表
CREATE TABLE wms_vas_order (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    order_no        VARCHAR(64)  NOT NULL,
    order_type      VARCHAR(16)  NOT NULL, -- INBOUND入库VAS/OUTBOUND出库VAS/STOCK库存VAS/STANDALONE独立VAS
    source_order_no VARCHAR(64),            -- 源单号(入库单/出库单)
    source_order_type VARCHAR(16),
    customer_code   VARCHAR(64),
    owner_code      VARCHAR(64),
    warehouse_code  VARCHAR(64)  NOT NULL,
    service_code    VARCHAR(64)  NOT NULL,
    service_name    VARCHAR(128),
    service_type    VARCHAR(32),
    plan_qty        DECIMAL(14,4)  DEFAULT 0, -- 计划数量
    actual_qty      DECIMAL(14,4)  DEFAULT 0, -- 实际完成数量
    unit            VARCHAR(16),
    unit_price      DECIMAL(14,4),
    total_amount    DECIMAL(14,4)  DEFAULT 0, -- 总费用
    status          VARCHAR(32)  NOT NULL,  -- PENDING待处理/ASSIGNED已分配/PROCESSING处理中/PAUSED暂停/COMPLETED完成/CANCELLED取消/EXCEPTION异常
    priority        SMALLINT     DEFAULT 5,
    assignee        VARCHAR(64),
    assign_time     TIMESTAMP,
    start_time      TIMESTAMP,
    finish_time     TIMESTAMP,
    actual_duration INT(10,2),            -- 实际耗时(分钟)
    plan_start_time TIMESTAMP,
    plan_finish_time TIMESTAMP,
    location_code   VARCHAR(64),            -- 作业区/操作台
    batch_no        VARCHAR(64),
    remark          VARCHAR(512),
    exception_reason VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    deleted         TINYINT(1)     DEFAULT 0,
    CONSTRAINT pk_wms_vas_order PRIMARY KEY (id),
    CONSTRAINT uk_wms_vas_order_no UNIQUE (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_vas_order_status ON wms_vas_order(status);
CREATE INDEX idx_wms_vas_order_source ON wms_vas_order(source_order_no);
CREATE INDEX idx_wms_vas_order_service ON wms_vas_order(service_code);

-- 3. VAS工单明细表
CREATE TABLE wms_vas_order_item (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    order_id        BIGINT    NOT NULL,
    order_no        VARCHAR(64)  NOT NULL,
    sku             VARCHAR(64)  NOT NULL,
    barcode         VARCHAR(64),
    product_name    VARCHAR(256),
    batch_no        VARCHAR(64),
    owner_code      VARCHAR(64),
    from_location   VARCHAR(64),            -- 源库位
    to_location     VARCHAR(64),            -- 目标库位
    plan_qty        DECIMAL(14,4) NOT NULL,
    actual_qty      DECIMAL(14,4) DEFAULT 0,
    unit_price      DECIMAL(14,4),
    item_amount     DECIMAL(14,4) DEFAULT 0,
    item_status     VARCHAR(32),  -- PENDING待处理/PROCESSING处理中/COMPLETED完成/EXCEPTION异常
    before_spec     VARCHAR(256),           -- 加工前规格
    after_spec      VARCHAR(256),           -- 加工后规格
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_vas_order_item PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_vas_item_order ON wms_vas_order_item(order_id);
CREATE INDEX idx_wms_vas_item_sku ON wms_vas_order_item(sku);

-- 4. VAS物料消耗表
CREATE TABLE wms_vas_material (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    order_id        BIGINT    NOT NULL,
    order_item_id   BIGINT,
    material_sku    VARCHAR(64)  NOT NULL,
    material_name   VARCHAR(256),
    material_barcode VARCHAR(64),
    plan_qty        DECIMAL(14,4) NOT NULL,   -- 计划用量
    actual_qty      DECIMAL(14,4) DEFAULT 0,  -- 实际用量
    unit            VARCHAR(16),
    unit_cost       DECIMAL(14,4),
    total_cost      DECIMAL(14,4) DEFAULT 0,
    location_code   VARCHAR(64),            -- 物料领用库位
    pick_by         VARCHAR(64),
    pick_time       TIMESTAMP,
    status          VARCHAR(16) DEFAULT 'PENDING', -- PENDING待领用/PICKED已领用/CONSUMED已消耗/RETURNED已退料
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_vas_material PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_vas_material_order ON wms_vas_material(order_id);


-- 注释
ALTER TABLE wms_vas_service COMMENT='VAS服务定义表';
ALTER TABLE wms_vas_order COMMENT='VAS工单表';
ALTER TABLE wms_vas_order_item COMMENT='VAS工单明细表';
ALTER TABLE wms_vas_material COMMENT='VAS物料消耗表';
