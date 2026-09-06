-- ============================================================
-- X WMS 库存模拟仿真管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 库存仿真/场景模拟/压力测试/方案评估
-- ============================================================

-- 1. 库存仿真表
CREATE TABLE wms_inventory_simulation (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    simulation_id   VARCHAR(64)  NOT NULL,
    simulation_name VARCHAR(128) NOT NULL,
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    simulation_type VARCHAR(32)  NOT NULL,
    description     VARCHAR(512),
    initial_state   TEXT,
    simulation_config TEXT,
    simulation_params TEXT,
    simulation_result TEXT,
    simulation_metrics TEXT,
    status          VARCHAR(32) DEFAULT 'DRAFT',
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    duration_ms     BIGINT,
    progress        INT(5,2) DEFAULT 0,
    error_message   VARCHAR(1024),
    operator        VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_inventory_simulation PRIMARY KEY (id),
    CONSTRAINT uk_wms_inventory_simulation_id UNIQUE (simulation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inventory_simulation_wh ON wms_inventory_simulation(warehouse_code);
CREATE INDEX idx_wms_inventory_simulation_type ON wms_inventory_simulation(simulation_type);
CREATE INDEX idx_wms_inventory_simulation_status ON wms_inventory_simulation(status);

-- 2. 场景模拟表
CREATE TABLE wms_scenario_simulation (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    scenario_id     VARCHAR(64)  NOT NULL,
    scenario_name   VARCHAR(128) NOT NULL,
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    scenario_type   VARCHAR(32)  NOT NULL,
    description     VARCHAR(512),
    scenario_config TEXT,
    scenario_events TEXT,
    initial_inventory TEXT,
    expected_result TEXT,
    actual_result   TEXT,
    deviation_analysis TEXT,
    status          VARCHAR(32) DEFAULT 'DRAFT',
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    duration_ms     BIGINT,
    operator        VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_scenario_simulation PRIMARY KEY (id),
    CONSTRAINT uk_wms_scenario_simulation_id UNIQUE (scenario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_scenario_simulation_wh ON wms_scenario_simulation(warehouse_code);
CREATE INDEX idx_wms_scenario_simulation_type ON wms_scenario_simulation(scenario_type);
CREATE INDEX idx_wms_scenario_simulation_status ON wms_scenario_simulation(status);

-- 3. 压力测试表
CREATE TABLE wms_stress_test (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    test_id         VARCHAR(64)  NOT NULL,
    test_name       VARCHAR(128) NOT NULL,
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    test_type       VARCHAR(32)  NOT NULL,
    description     VARCHAR(512),
    test_config     TEXT,
    concurrency     INT,
    total_requests  BIGINT,
    success_count   BIGINT,
    failure_count   BIGINT,
    avg_response_time INT(19,4),
    max_response_time INT(19,4),
    min_response_time INT(19,4),
    p50_response_time INT(19,4),
    p95_response_time INT(19,4),
    p99_response_time INT(19,4),
    throughput      INT(19,4),
    error_rate      INT(10,2),
    test_result     TEXT,
    status          VARCHAR(32) DEFAULT 'DRAFT',
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    duration_ms     BIGINT,
    operator        VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_stress_test PRIMARY KEY (id),
    CONSTRAINT uk_wms_stress_test_id UNIQUE (test_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_stress_test_wh ON wms_stress_test(warehouse_code);
CREATE INDEX idx_wms_stress_test_type ON wms_stress_test(test_type);
CREATE INDEX idx_wms_stress_test_status ON wms_stress_test(status);

-- 4. 方案评估表
CREATE TABLE wms_scheme_evaluation (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    evaluation_id   VARCHAR(64)  NOT NULL,
    evaluation_name VARCHAR(128) NOT NULL,
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    scheme_type     VARCHAR(32)  NOT NULL,
    description     VARCHAR(512),
    scheme_a_config TEXT,
    scheme_b_config TEXT,
    scheme_a_result TEXT,
    scheme_b_result TEXT,
    comparison_result TEXT,
    evaluation_metrics TEXT,
    recommendation  VARCHAR(32),
    recommendation_reason VARCHAR(512),
    status          VARCHAR(32) DEFAULT 'DRAFT',
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    duration_ms     BIGINT,
    operator        VARCHAR(64),
    approver        VARCHAR(64),
    approve_time    TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_scheme_evaluation PRIMARY KEY (id),
    CONSTRAINT uk_wms_scheme_evaluation_id UNIQUE (evaluation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_scheme_evaluation_wh ON wms_scheme_evaluation(warehouse_code);
CREATE INDEX idx_wms_scheme_evaluation_type ON wms_scheme_evaluation(scheme_type);
CREATE INDEX idx_wms_scheme_evaluation_status ON wms_scheme_evaluation(status);


-- 注释
ALTER TABLE wms_inventory_simulation COMMENT='库存仿真表';
ALTER TABLE wms_scenario_simulation COMMENT='场景模拟表';
ALTER TABLE wms_stress_test COMMENT='压力测试表';
ALTER TABLE wms_scheme_evaluation COMMENT='方案评估表';
