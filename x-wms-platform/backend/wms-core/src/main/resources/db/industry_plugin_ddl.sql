-- ============================================================
-- X WMS 行业插件模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 冷链行业/电商行业/GSP医药行业
-- ============================================================

-- ============================================================
-- 一、冷链行业 (Cold Chain)
-- ============================================================

-- 1. 温度监控记录表
CREATE TABLE wms_coldchain_temp_record (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    record_no       VARCHAR(64)  NOT NULL,
    warehouse_code  VARCHAR(64),
    area_code       VARCHAR(64),
    location_code   VARCHAR(64),
    equipment_code  VARCHAR(64),
    temperature     INT(10,4),
    humidity        INT(10,4),
    temperature_type VARCHAR(32),
    collect_time    TIMESTAMP,
    collect_type    VARCHAR(16),
    collector       VARCHAR(64),
    status          VARCHAR(16)  DEFAULT 'NORMAL',
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_coldchain_temp_record PRIMARY KEY (id),
    CONSTRAINT uk_wms_coldchain_temp_record_no UNIQUE (record_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_cctr_location ON wms_coldchain_temp_record(location_code);
CREATE INDEX idx_wms_cctr_equipment ON wms_coldchain_temp_record(equipment_code);
CREATE INDEX idx_wms_cctr_collect_time ON wms_coldchain_temp_record(collect_time);

-- 2. 温度异常报警表
CREATE TABLE wms_coldchain_temp_alert (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    alert_no        VARCHAR(64)  NOT NULL,
    record_no       VARCHAR(64),
    warehouse_code  VARCHAR(64),
    area_code       VARCHAR(64),
    location_code   VARCHAR(64),
    equipment_code  VARCHAR(64),
    alert_type      VARCHAR(32),
    alert_level     VARCHAR(16),
    current_temp    INT(10,4),
    temp_upper_limit INT(10,4),
    temp_lower_limit INT(10,4),
    current_humidity INT(10,4),
    duration_minutes INT,
    alert_time      TIMESTAMP,
    status          VARCHAR(16)  DEFAULT 'PENDING',
    handled_by      VARCHAR(64),
    handled_time    TIMESTAMP,
    handle_result   VARCHAR(512),
    handle_remark   VARCHAR(512),
    notify_status   VARCHAR(16)  DEFAULT 'NOT_NOTIFIED',
    notify_time     TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_coldchain_temp_alert PRIMARY KEY (id),
    CONSTRAINT uk_wms_coldchain_temp_alert_no UNIQUE (alert_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_ccta_status ON wms_coldchain_temp_alert(status);
CREATE INDEX idx_wms_ccta_location ON wms_coldchain_temp_alert(location_code);
CREATE INDEX idx_wms_ccta_alert_time ON wms_coldchain_temp_alert(alert_time);

-- 3. 冷链设备表
CREATE TABLE wms_coldchain_equipment (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    equipment_code  VARCHAR(64)  NOT NULL,
    equipment_name  VARCHAR(128) NOT NULL,
    equipment_type  VARCHAR(32),
    brand           VARCHAR(64),
    model           VARCHAR(64),
    serial_number   VARCHAR(128),
    warehouse_code  VARCHAR(64),
    area_code       VARCHAR(64),
    location_code   VARCHAR(64),
    temperature_type VARCHAR(32),
    temp_upper_limit INT(10,4),
    temp_lower_limit INT(10,4),
    humidity_upper_limit INT(10,4),
    humidity_lower_limit INT(10,4),
    current_temp    INT(10,4),
    current_humidity INT(10,4),
    run_status      VARCHAR(16)  DEFAULT 'RUNNING',
    status          VARCHAR(16)  DEFAULT 'ACTIVE',
    install_date    TIMESTAMP,
    last_maintain_date TIMESTAMP,
    next_maintain_date TIMESTAMP,
    responsible_person VARCHAR(64),
    contact_phone   VARCHAR(32),
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_coldchain_equipment PRIMARY KEY (id),
    CONSTRAINT uk_wms_coldchain_equipment_code UNIQUE (equipment_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_cce_warehouse ON wms_coldchain_equipment(warehouse_code);
CREATE INDEX idx_wms_cce_type ON wms_coldchain_equipment(equipment_type);
CREATE INDEX idx_wms_cce_run_status ON wms_coldchain_equipment(run_status);

-- 4. 温区管理表
CREATE TABLE wms_coldchain_temp_zone (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    zone_code       VARCHAR(64)  NOT NULL,
    zone_name       VARCHAR(128) NOT NULL,
    warehouse_code  VARCHAR(64),
    area_codes      VARCHAR(512),
    temperature_type VARCHAR(32),
    target_temp     INT(10,4),
    temp_upper_limit INT(10,4),
    temp_lower_limit INT(10,4),
    target_humidity INT(10,4),
    humidity_upper_limit INT(10,4),
    humidity_lower_limit INT(10,4),
    temp_tolerance  INT(10,4),
    alert_delay_minutes INT,
    equipment_codes VARCHAR(512),
    status          VARCHAR(16)  DEFAULT 'ACTIVE',
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_coldchain_temp_zone PRIMARY KEY (id),
    CONSTRAINT uk_wms_coldchain_temp_zone_code UNIQUE (zone_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_ctz_warehouse ON wms_coldchain_temp_zone(warehouse_code);
CREATE INDEX idx_wms_ctz_type ON wms_coldchain_temp_zone(temperature_type);

-- ============================================================
-- 二、电商行业 (E-commerce)
-- ============================================================

-- 5. 预售订单表
CREATE TABLE wms_ecommerce_pre_sale_order (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    pre_sale_no     VARCHAR(64)  NOT NULL,
    order_no        VARCHAR(64),
    shop_code       VARCHAR(64),
    platform_code   VARCHAR(32),
    sku_code        VARCHAR(64),
    sku_name        VARCHAR(256),
    spec            VARCHAR(128),
    quantity        INT(18,4),
    deposit_amount  INT(18,4),
    balance_amount  INT(18,4),
    total_amount    INT(18,4),
    pre_sale_start_time TIMESTAMP,
    pre_sale_end_time TIMESTAMP,
    balance_pay_start_time TIMESTAMP,
    balance_pay_end_time TIMESTAMP,
    expected_ship_time TIMESTAMP,
    actual_ship_time TIMESTAMP,
    status          VARCHAR(16)  DEFAULT 'PENDING',
    deposit_status  VARCHAR(16)  DEFAULT 'UNPAID',
    balance_status  VARCHAR(16)  DEFAULT 'UNPAID',
    ship_status     VARCHAR(16)  DEFAULT 'PENDING',
    shipped_qty     INT(18,4)  DEFAULT 0,
    receiver_name   VARCHAR(64),
    receiver_phone  VARCHAR(32),
    receiver_address VARCHAR(512),
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_ecommerce_pre_sale PRIMARY KEY (id),
    CONSTRAINT uk_wms_ecommerce_pre_sale_no UNIQUE (pre_sale_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_eps_order_no ON wms_ecommerce_pre_sale_order(order_no);
CREATE INDEX idx_wms_eps_shop ON wms_ecommerce_pre_sale_order(shop_code);
CREATE INDEX idx_wms_eps_status ON wms_ecommerce_pre_sale_order(status);

-- 6. 秒杀活动表
CREATE TABLE wms_ecommerce_flash_sale (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    activity_no     VARCHAR(64)  NOT NULL,
    activity_name   VARCHAR(128) NOT NULL,
    shop_code       VARCHAR(64),
    platform_code   VARCHAR(32),
    activity_type   VARCHAR(32),
    sku_code        VARCHAR(64),
    sku_name        VARCHAR(256),
    activity_stock  INT(18,4),
    sold_qty        INT(18,4)  DEFAULT 0,
    remaining_qty   INT(18,4),
    original_price  INT(18,4),
    activity_price  INT(18,4),
    discount_rate   INT(10,4),
    limit_per_person INT(18,4),
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    preheat_start_time TIMESTAMP,
    status          VARCHAR(16)  DEFAULT 'DRAFT',
    stock_lock_status VARCHAR(16) DEFAULT 'NOT_LOCKED',
    lock_time       TIMESTAMP,
    wave_no         VARCHAR(64),
    priority        SMALLINT     DEFAULT 5,
    auto_ship       VARCHAR(1)   DEFAULT 'N',
    ship_deadline   TIMESTAMP,
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_ecommerce_flash_sale PRIMARY KEY (id),
    CONSTRAINT uk_wms_ecommerce_flash_sale_no UNIQUE (activity_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_efs_shop ON wms_ecommerce_flash_sale(shop_code);
CREATE INDEX idx_wms_efs_status ON wms_ecommerce_flash_sale(status);
CREATE INDEX idx_wms_efs_start_time ON wms_ecommerce_flash_sale(start_time);

-- 7. 电商波次配置表
CREATE TABLE wms_ecommerce_wave_config (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    config_code     VARCHAR(64)  NOT NULL,
    config_name     VARCHAR(128) NOT NULL,
    shop_code       VARCHAR(64),
    platform_code   VARCHAR(32),
    activity_type   VARCHAR(32),
    wave_type       VARCHAR(32),
    time_interval   INT,
    max_order_count INT,
    max_sku_count   INT,
    max_item_count  INT,
    wave_start_time TIMESTAMP,
    wave_end_time   TIMESTAMP,
    pick_mode       VARCHAR(32),
    allow_merge     VARCHAR(1)   DEFAULT 'N',
    priority        SMALLINT     DEFAULT 5,
    auto_release    VARCHAR(1)   DEFAULT 'N',
    timeout_minutes INT,
    enabled         VARCHAR(1)   DEFAULT 'Y',
    effective_start_date TIMESTAMP,
    effective_end_date TIMESTAMP,
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_ecommerce_wave_config PRIMARY KEY (id),
    CONSTRAINT uk_wms_ecommerce_wave_config_code UNIQUE (config_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_ewc_shop ON wms_ecommerce_wave_config(shop_code);
CREATE INDEX idx_wms_ewc_activity_type ON wms_ecommerce_wave_config(activity_type);
CREATE INDEX idx_wms_ewc_enabled ON wms_ecommerce_wave_config(enabled);

-- ============================================================
-- 三、GSP医药行业 (Good Supply Practice)
-- ============================================================

-- 8. GSP批次记录表
CREATE TABLE wms_gsp_batch_record (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    record_no       VARCHAR(64)  NOT NULL,
    batch_no        VARCHAR(128),
    sku_code        VARCHAR(64),
    sku_name        VARCHAR(256),
    spec            VARCHAR(128),
    dosage_form     VARCHAR(64),
    manufacturer    VARCHAR(128),
    approval_number VARCHAR(64),
    production_date DATE,
    expiry_date     DATE,
    inbound_qty     INT(18,4),
    stock_qty       INT(18,4),
    outbound_qty    INT(18,4)  DEFAULT 0,
    warehouse_code  VARCHAR(64),
    area_code       VARCHAR(64),
    location_code   VARCHAR(64),
    owner_code      VARCHAR(64),
    supplier_code   VARCHAR(64),
    supplier_name   VARCHAR(128),
    purchase_order_no VARCHAR(64),
    inbound_no      VARCHAR(64),
    receive_no      VARCHAR(64),
    acceptance_no   VARCHAR(64),
    quality_status  VARCHAR(16)  DEFAULT 'WAITING',
    maintenance_status VARCHAR(16) DEFAULT 'NORMAL',
    storage_condition VARCHAR(32),
    temp_requirement INT(10,4),
    humidity_requirement INT(10,4),
    is_first_variety VARCHAR(1)  DEFAULT 'N',
    is_imported     VARCHAR(1)   DEFAULT 'N',
    is_special_managed VARCHAR(1) DEFAULT 'N',
    special_drug_type VARCHAR(32),
    trace_code      VARCHAR(128),
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_gsp_batch_record PRIMARY KEY (id),
    CONSTRAINT uk_wms_gsp_batch_record_no UNIQUE (record_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_gbr_batch_no ON wms_gsp_batch_record(batch_no);
CREATE INDEX idx_wms_gbr_sku ON wms_gsp_batch_record(sku_code);
CREATE INDEX idx_wms_gbr_expiry_date ON wms_gsp_batch_record(expiry_date);
CREATE INDEX idx_wms_gbr_quality_status ON wms_gsp_batch_record(quality_status);

-- 9. GSP质检记录表
CREATE TABLE wms_gsp_quality_check (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    check_no        VARCHAR(64)  NOT NULL,
    check_type      VARCHAR(32),
    ref_no          VARCHAR(64),
    batch_no        VARCHAR(128),
    sku_code        VARCHAR(64),
    sku_name        VARCHAR(256),
    spec            VARCHAR(128),
    manufacturer    VARCHAR(128),
    approval_number VARCHAR(64),
    check_qty       INT(18,4),
    sample_qty      INT(18,4),
    qualified_qty   INT(18,4),
    unqualified_qty INT(18,4),
    appearance_check VARCHAR(16),
    packaging_check VARCHAR(16),
    label_check     VARCHAR(16),
    expiry_check    VARCHAR(16),
    batch_check     VARCHAR(16),
    temp_record     VARCHAR(16),
    check_result    VARCHAR(16),
    unqualified_reason VARCHAR(512),
    handle_method   VARCHAR(32),
    handle_opinion  VARCHAR(512),
    status          VARCHAR(16)  DEFAULT 'PENDING',
    checker         VARCHAR(64),
    check_time      TIMESTAMP,
    reviewer        VARCHAR(64),
    review_time     TIMESTAMP,
    approver        VARCHAR(64),
    approve_time    TIMESTAMP,
    warehouse_code  VARCHAR(64),
    area_code       VARCHAR(64),
    location_code   VARCHAR(64),
    owner_code      VARCHAR(64),
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_gsp_quality_check PRIMARY KEY (id),
    CONSTRAINT uk_wms_gsp_quality_check_no UNIQUE (check_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_gqc_ref_no ON wms_gsp_quality_check(ref_no);
CREATE INDEX idx_wms_gqc_batch_no ON wms_gsp_quality_check(batch_no);
CREATE INDEX idx_wms_gqc_type ON wms_gsp_quality_check(check_type);
CREATE INDEX idx_wms_gqc_status ON wms_gsp_quality_check(status);

-- 10. GSP温度日志表
CREATE TABLE wms_gsp_temp_log (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    log_no          VARCHAR(64)  NOT NULL,
    warehouse_code  VARCHAR(64),
    area_code       VARCHAR(64),
    location_code   VARCHAR(64),
    equipment_code  VARCHAR(64),
    monitor_point_name VARCHAR(128),
    temperature     INT(10,4),
    humidity        INT(10,4),
    temp_upper_limit INT(10,4),
    temp_lower_limit INT(10,4),
    humidity_upper_limit INT(10,4),
    humidity_lower_limit INT(10,4),
    temp_status     VARCHAR(16)  DEFAULT 'NORMAL',
    humidity_status VARCHAR(16)  DEFAULT 'NORMAL',
    exceed_duration INT,
    collect_time    TIMESTAMP,
    collect_type    VARCHAR(16),
    collector       VARCHAR(64),
    is_alert        VARCHAR(1)   DEFAULT 'N',
    alert_no        VARCHAR(64),
    alert_level     VARCHAR(16),
    handle_status   VARCHAR(16)  DEFAULT 'PENDING',
    handled_by      VARCHAR(64),
    handled_time    TIMESTAMP,
    handle_measure  VARCHAR(512),
    handle_result   VARCHAR(512),
    storage_condition VARCHAR(32),
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_gsp_temp_log PRIMARY KEY (id),
    CONSTRAINT uk_wms_gsp_temp_log_no UNIQUE (log_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_gtl_area ON wms_gsp_temp_log(area_code);
CREATE INDEX idx_wms_gtl_collect_time ON wms_gsp_temp_log(collect_time);
CREATE INDEX idx_wms_gtl_temp_status ON wms_gsp_temp_log(temp_status);
CREATE INDEX idx_wms_gtl_handle_status ON wms_gsp_temp_log(handle_status);

-- 11. GSP证照管理表
CREATE TABLE wms_gsp_license (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    license_no      VARCHAR(64)  NOT NULL,
    license_name    VARCHAR(128) NOT NULL,
    license_type    VARCHAR(32),
    certificate_no  VARCHAR(128),
    holder_name     VARCHAR(128),
    legal_representative VARCHAR(64),
    enterprise_principal VARCHAR(64),
    quality_principal VARCHAR(64),
    registered_address VARCHAR(512),
    business_address VARCHAR(512),
    warehouse_address VARCHAR(512),
    business_scope  TEXT,
    business_mode   VARCHAR(32),
    issuing_authority VARCHAR(128),
    issue_date      DATE,
    expiry_date     DATE,
    renewal_date    DATE,
    status          VARCHAR(16)  DEFAULT 'ACTIVE',
    warning_days    SMALLINT     DEFAULT 30,
    is_warned       VARCHAR(1)   DEFAULT 'N',
    warn_time       TIMESTAMP,
    warehouse_code  VARCHAR(64),
    owner_code      VARCHAR(64),
    supplier_code   VARCHAR(64),
    scan_file_url   VARCHAR(512),
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_gsp_license PRIMARY KEY (id),
    CONSTRAINT uk_wms_gsp_license_no UNIQUE (license_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_gl_type ON wms_gsp_license(license_type);
CREATE INDEX idx_wms_gl_expiry_date ON wms_gsp_license(expiry_date);
CREATE INDEX idx_wms_gl_status ON wms_gsp_license(status);
CREATE INDEX idx_wms_gl_warehouse ON wms_gsp_license(warehouse_code);

-- ============================================================
-- ============================================================

-- ============================================================
-- 注释
-- ============================================================
ALTER TABLE wms_coldchain_temp_record COMMENT='冷链温度监控记录表';
ALTER TABLE wms_coldchain_temp_alert COMMENT='冷链温度异常报警表';
ALTER TABLE wms_coldchain_equipment COMMENT='冷链设备管理表';
ALTER TABLE wms_coldchain_temp_zone COMMENT='冷链温区管理表';
ALTER TABLE wms_ecommerce_pre_sale_order COMMENT='电商预售订单表';
ALTER TABLE wms_ecommerce_flash_sale COMMENT='电商秒杀活动表';
ALTER TABLE wms_ecommerce_wave_config COMMENT='电商波次配置表';
ALTER TABLE wms_gsp_batch_record COMMENT='GSP批次记录表';
ALTER TABLE wms_gsp_quality_check COMMENT='GSP质检记录表';
ALTER TABLE wms_gsp_temp_log COMMENT='GSP温度日志表';
ALTER TABLE wms_gsp_license COMMENT='GSP证照管理表';
