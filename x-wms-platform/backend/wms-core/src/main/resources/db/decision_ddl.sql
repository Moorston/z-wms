-- ============================================================
-- X WMS 库存决策支持管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 库存决策/库存优化/库存策略/库存诊断
-- ============================================================

-- 1. 库存决策表
CREATE TABLE wms_inventory_decision (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    decision_id     VARCHAR(64)  NOT NULL,
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64),
    sku_name        VARCHAR(128),
    category_code   VARCHAR(64),
    decision_type   VARCHAR(32)  NOT NULL,
    decision_name   VARCHAR(128),
    decision_content TEXT,
    decision_reason VARCHAR(512),
    decision_basis  TEXT,
    expected_impact TEXT,
    actual_impact   TEXT,
    priority        VARCHAR(16) DEFAULT 'NORMAL',
    confidence      INT(5,2),
    status          VARCHAR(32) DEFAULT 'DRAFT',
    approver        VARCHAR(64),
    approve_time    TIMESTAMP,
    approve_comment VARCHAR(512),
    operator        VARCHAR(64),
    operate_time    TIMESTAMP,
    related_biz_no  VARCHAR(64),
    decision_time   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_inventory_decision PRIMARY KEY (id),
    CONSTRAINT uk_wms_inventory_decision_id UNIQUE (decision_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inventory_decision_wh ON wms_inventory_decision(warehouse_code);
CREATE INDEX idx_wms_inventory_decision_sku ON wms_inventory_decision(sku_code);
CREATE INDEX idx_wms_inventory_decision_type ON wms_inventory_decision(decision_type);
CREATE INDEX idx_wms_inventory_decision_status ON wms_inventory_decision(status);

-- 2. 库存优化表
CREATE TABLE wms_inventory_optimization (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    optimization_id VARCHAR(64)  NOT NULL,
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    optimization_type VARCHAR(32) NOT NULL,
    optimization_name VARCHAR(128),
    current_state   TEXT,
    target_state    TEXT,
    optimization_plan TEXT,
    expected_benefit TEXT,
    actual_benefit  TEXT,
    implementation_plan TEXT,
    implementation_status VARCHAR(32) DEFAULT 'PENDING',
    implementation_start TIMESTAMP,
    implementation_end TIMESTAMP,
    priority        VARCHAR(16) DEFAULT 'NORMAL',
    status          VARCHAR(32) DEFAULT 'DRAFT',
    approver        VARCHAR(64),
    approve_time    TIMESTAMP,
    operator        VARCHAR(64),
    operate_time    TIMESTAMP,
    optimization_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_inventory_optimization PRIMARY KEY (id),
    CONSTRAINT uk_wms_inventory_optimization_id UNIQUE (optimization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inventory_optimization_wh ON wms_inventory_optimization(warehouse_code);
CREATE INDEX idx_wms_inventory_optimization_type ON wms_inventory_optimization(optimization_type);
CREATE INDEX idx_wms_inventory_optimization_status ON wms_inventory_optimization(status);
CREATE INDEX idx_wms_inventory_optimization_impl ON wms_inventory_optimization(implementation_status);

-- 3. 库存策略表
CREATE TABLE wms_inventory_strategy (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    strategy_id     VARCHAR(64)  NOT NULL,
    strategy_code   VARCHAR(64)  NOT NULL,
    strategy_name   VARCHAR(128) NOT NULL,
    strategy_type   VARCHAR(32)  NOT NULL,
    warehouse_code  VARCHAR(64),
    owner_code      VARCHAR(64),
    category_code   VARCHAR(64),
    sku_code        VARCHAR(64),
    strategy_config TEXT,
    strategy_rules  TEXT,
    priority        INT DEFAULT 100,
    is_active       VARCHAR(8) DEFAULT 'Y',
    effective_start TIMESTAMP,
    effective_end   TIMESTAMP,
    version         INT DEFAULT 1,
    description     VARCHAR(512),
    created_by      VARCHAR(64),
    updated_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_inventory_strategy PRIMARY KEY (id),
    CONSTRAINT uk_wms_inventory_strategy_id UNIQUE (strategy_id),
    CONSTRAINT uk_wms_inventory_strategy_code UNIQUE (strategy_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inventory_strategy_type ON wms_inventory_strategy(strategy_type);
CREATE INDEX idx_wms_inventory_strategy_wh ON wms_inventory_strategy(warehouse_code);
CREATE INDEX idx_wms_inventory_strategy_active ON wms_inventory_strategy(is_active);

-- 4. 库存诊断表
CREATE TABLE wms_inventory_diagnosis (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    diagnosis_id    VARCHAR(64)  NOT NULL,
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    diagnosis_type  VARCHAR(32)  NOT NULL,
    diagnosis_name  VARCHAR(128),
    diagnosis_scope VARCHAR(32) DEFAULT 'WAREHOUSE',
    scope_code      VARCHAR(64),
    current_value   INT(19,4),
    benchmark_value INT(19,4),
    target_value    INT(19,4),
    deviation       INT(19,4),
    deviation_rate  INT(10,2),
    severity        VARCHAR(16) DEFAULT 'NORMAL',
    root_cause      TEXT,
    diagnosis_result TEXT,
    recommendations TEXT,
    action_plan     TEXT,
    priority        VARCHAR(16) DEFAULT 'NORMAL',
    status          VARCHAR(32) DEFAULT 'PENDING',
    handler         VARCHAR(64),
    handle_time     TIMESTAMP,
    handle_result   VARCHAR(512),
    operator        VARCHAR(64),
    operate_time    TIMESTAMP,
    diagnosis_time  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_inventory_diagnosis PRIMARY KEY (id),
    CONSTRAINT uk_wms_inventory_diagnosis_id UNIQUE (diagnosis_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inventory_diagnosis_wh ON wms_inventory_diagnosis(warehouse_code);
CREATE INDEX idx_wms_inventory_diagnosis_type ON wms_inventory_diagnosis(diagnosis_type);
CREATE INDEX idx_wms_inventory_diagnosis_severity ON wms_inventory_diagnosis(severity);
CREATE INDEX idx_wms_inventory_diagnosis_status ON wms_inventory_diagnosis(status);


-- 注释
ALTER TABLE wms_inventory_decision COMMENT='库存决策表';
ALTER TABLE wms_inventory_optimization COMMENT='库存优化表';
ALTER TABLE wms_inventory_strategy COMMENT='库存策略表';
ALTER TABLE wms_inventory_diagnosis COMMENT='库存诊断表';
