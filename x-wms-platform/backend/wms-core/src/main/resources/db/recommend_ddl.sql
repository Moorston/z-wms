-- ============================================================
-- X WMS 库存智能推荐管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 补货推荐/库位推荐/波次推荐/路径推荐
-- ============================================================

-- 1. 补货推荐表
CREATE TABLE wms_replenish_recommend (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    recommend_id    VARCHAR(64)  NOT NULL, -- 推荐ID
    warehouse_code  VARCHAR(64)  NOT NULL, -- 仓库
    owner_code      VARCHAR(64), -- 货主
    sku_code        VARCHAR(64)  NOT NULL, -- SKU
    sku_name        VARCHAR(128), -- SKU名称
    category_code   VARCHAR(64), -- 品类
    from_location   VARCHAR(64), -- 来源库位(补货源)
    to_location     VARCHAR(64), -- 目标库位(补货目标)
    current_quantity INT(19,4), -- 当前库存
    safety_stock    INT(19,4), -- 安全库存
    reorder_point   INT(19,4), -- 再订货点
    max_stock       INT(19,4), -- 最大库存
    recommend_quantity INT(19,4), -- 推荐补货量
    forecast_demand INT(19,4), -- 预测需求量
    forecast_period VARCHAR(32), -- 预测周期: DAILY/WEEKLY/MONTHLY
    lead_time       INT, -- 提前期(天)
    priority        VARCHAR(16) DEFAULT 'NORMAL', -- 优先级: URGENT紧急/HIGH高/NORMAL正常/LOW低
    confidence      INT(5,2), -- 置信度(%)
    reason          VARCHAR(512), -- 推荐原因
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING待处理/ACCEPTED已接受/REJECTED已拒绝/EXECUTED已执行
    operator        VARCHAR(64), -- 操作人
    operate_time    TIMESTAMP, -- 操作时间
    related_replenish_id VARCHAR(64), -- 关联补货单ID
    recommend_time  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP, -- 推荐时间
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_replenish_recommend PRIMARY KEY (id),
    CONSTRAINT uk_wms_replenish_recommend_id UNIQUE (recommend_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_replenish_recommend_wh ON wms_replenish_recommend(warehouse_code);
CREATE INDEX idx_wms_replenish_recommend_sku ON wms_replenish_recommend(sku_code);
CREATE INDEX idx_wms_replenish_recommend_status ON wms_replenish_recommend(status);
CREATE INDEX idx_wms_replenish_recommend_priority ON wms_replenish_recommend(priority);

-- 2. 库位推荐表
CREATE TABLE wms_location_recommend (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    recommend_id    VARCHAR(64)  NOT NULL, -- 推荐ID
    warehouse_code  VARCHAR(64)  NOT NULL, -- 仓库
    owner_code      VARCHAR(64), -- 货主
    sku_code        VARCHAR(64)  NOT NULL, -- SKU
    sku_name        VARCHAR(128), -- SKU名称
    batch_no        VARCHAR(64), -- 批次
    quantity        INT(19,4), -- 数量
    biz_type        VARCHAR(32)  NOT NULL, -- 业务类型: INBOUND上架/MOVE移库/REPLENISH补货/RETURN退货
    biz_no          VARCHAR(64), -- 业务单号
    recommend_locations TEXT, -- 推荐库位列表(JSON数组, 按优先级排序)
    best_location   VARCHAR(64), -- 最佳推荐库位
    best_score      INT(10,2), -- 最佳得分
    recommend_reason VARCHAR(512), -- 推荐原因
    factors         TEXT, -- 考虑因素(JSON)
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING待处理/ACCEPTED已接受/REJECTED已拒绝/EXECUTED已执行
    operator        VARCHAR(64), -- 操作人
    operate_time    TIMESTAMP, -- 操作时间
    selected_location VARCHAR(64), -- 实际选择库位
    recommend_time  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP, -- 推荐时间
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_location_recommend PRIMARY KEY (id),
    CONSTRAINT uk_wms_location_recommend_id UNIQUE (recommend_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_location_recommend_wh ON wms_location_recommend(warehouse_code);
CREATE INDEX idx_wms_location_recommend_sku ON wms_location_recommend(sku_code);
CREATE INDEX idx_wms_location_recommend_biz ON wms_location_recommend(biz_type, biz_no);
CREATE INDEX idx_wms_location_recommend_status ON wms_location_recommend(status);

-- 3. 波次推荐表
CREATE TABLE wms_wave_recommend (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    recommend_id    VARCHAR(64)  NOT NULL, -- 推荐ID
    warehouse_code  VARCHAR(64)  NOT NULL, -- 仓库
    owner_code      VARCHAR(64), -- 货主
    wave_type       VARCHAR(32)  NOT NULL, -- 波次类型: NORMAL普通/URGENT紧急/BULK大宗/APPOINTMENT预约
    recommend_strategy VARCHAR(32), -- 推荐策略: TIME时间/QUANTITY数量/CARRIER承运商/AREA库区/PRIORITY优先级
    order_count     INT, -- 订单数
    sku_count       INT, -- SKU数
    total_quantity  INT(19,4), -- 总数量
    recommend_orders TEXT, -- 推荐订单列表(JSON数组)
    recommend_wave_name VARCHAR(128), -- 推荐波次名称
    estimate_pick_time BIGINT, -- 预计拣货时间(分钟)
    estimate_pickers SMALLINT, -- 预计拣货人数
    priority        VARCHAR(16) DEFAULT 'NORMAL', -- 优先级: URGENT紧急/HIGH高/NORMAL正常/LOW低
    confidence      INT(5,2), -- 置信度(%)
    reason          VARCHAR(512), -- 推荐原因
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING待处理/ACCEPTED已接受/REJECTED已拒绝/EXECUTED已执行
    operator        VARCHAR(64), -- 操作人
    operate_time    TIMESTAMP, -- 操作时间
    related_wave_id VARCHAR(64), -- 关联波次ID
    recommend_time  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP, -- 推荐时间
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_wave_recommend PRIMARY KEY (id),
    CONSTRAINT uk_wms_wave_recommend_id UNIQUE (recommend_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_wave_recommend_wh ON wms_wave_recommend(warehouse_code);
CREATE INDEX idx_wms_wave_recommend_type ON wms_wave_recommend(wave_type);
CREATE INDEX idx_wms_wave_recommend_status ON wms_wave_recommend(status);
CREATE INDEX idx_wms_wave_recommend_priority ON wms_wave_recommend(priority);

-- 4. 路径推荐表
CREATE TABLE wms_path_recommend (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    recommend_id    VARCHAR(64)  NOT NULL, -- 推荐ID
    warehouse_code  VARCHAR(64)  NOT NULL, -- 仓库
    owner_code      VARCHAR(64), -- 货主
    wave_id         VARCHAR(64), -- 波次ID
    picker_id       VARCHAR(64), -- 拣货员ID
    picker_name     VARCHAR(128), -- 拣货员名称
    pick_mode       VARCHAR(32)  NOT NULL, -- 拣货模式: PICK_TO_CART摘果式/SOW_TO_BIN播种式/RELAY接力式
    location_count  INT, -- 库位数
    sku_count       INT, -- SKU数
    total_quantity  INT(19,4), -- 总数量
    recommend_path  TEXT, -- 推荐路径(JSON数组, 库位序列)
    start_location  VARCHAR(64), -- 起点库位
    end_location    VARCHAR(64), -- 终点库位
    estimate_distance INT(19,4), -- 预计距离(米)
    estimate_time   BIGINT, -- 预计时间(分钟)
    path_algorithm  VARCHAR(32), -- 路径算法: NEAREST_NEIGHBOR最近邻/TSP旅行商问题/GENETIC遗传算法/SHORTEST_PATH最短路径
    congestion_avoidance VARCHAR(8) DEFAULT 'Y', -- 拥堵规避: Y/N
    revisit_avoidance VARCHAR(8) DEFAULT 'Y', -- 避免重走: Y/N
    priority        VARCHAR(16) DEFAULT 'NORMAL', -- 优先级: URGENT紧急/HIGH高/NORMAL正常/LOW低
    confidence      INT(5,2), -- 置信度(%)
    reason          VARCHAR(512), -- 推荐原因
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING待处理/ACCEPTED已接受/REJECTED已拒绝/EXECUTED已执行
    operator        VARCHAR(64), -- 操作人
    operate_time    TIMESTAMP, -- 操作时间
    actual_path     TEXT, -- 实际路径(JSON数组)
    actual_distance INT(19,4), -- 实际距离(米)
    actual_time     BIGINT, -- 实际时间(分钟)
    recommend_time  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP, -- 推荐时间
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_path_recommend PRIMARY KEY (id),
    CONSTRAINT uk_wms_path_recommend_id UNIQUE (recommend_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_path_recommend_wh ON wms_path_recommend(warehouse_code);
CREATE INDEX idx_wms_path_recommend_wave ON wms_path_recommend(wave_id);
CREATE INDEX idx_wms_path_recommend_picker ON wms_path_recommend(picker_id);
CREATE INDEX idx_wms_path_recommend_status ON wms_path_recommend(status);


-- 注释
ALTER TABLE wms_replenish_recommend COMMENT='补货推荐表';
ALTER TABLE wms_location_recommend COMMENT='库位推荐表';
ALTER TABLE wms_wave_recommend COMMENT='波次推荐表';
ALTER TABLE wms_path_recommend COMMENT='路径推荐表';
