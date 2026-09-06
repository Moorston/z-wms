-- ============================================================
-- X WMS 库存组合管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 组合规则/组合明细/组合组装单/组合拆解单
-- ============================================================

-- 1. 组合规则表
CREATE TABLE wms_combo_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    combo_code      VARCHAR(64)  NOT NULL, -- 组合品编码
    combo_name      VARCHAR(256) NOT NULL, -- 组合品名称
    warehouse_code  VARCHAR(64), -- 适用仓库
    owner_code      VARCHAR(64), -- 适用货主
    combo_type      VARCHAR(32)  NOT NULL, -- 组合类型: KIT套装/BUNDLE捆绑/GIFT赠品/PROMO促销
    status          VARCHAR(32) DEFAULT 'ACTIVE', -- ACTIVE/INACTIVE
    auto_assemble   VARCHAR(8)   DEFAULT 'N', -- 是否自动组装
    auto_disassemble VARCHAR(8)  DEFAULT 'N', -- 是否自动拆解
    cost_calc_method VARCHAR(32), -- 成本计算: SUM子品成本/固定成本/加权平均
    fixed_cost      INT(18,4), -- 固定成本
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_combo_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_combo_code UNIQUE (combo_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 组合明细表
CREATE TABLE wms_combo_item (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    combo_code      VARCHAR(64)  NOT NULL, -- 组合品编码
    item_sku_code   VARCHAR(64)  NOT NULL, -- 子品SKU
    item_sku_name   VARCHAR(256), -- 子品名称
    quantity        INT(18,4)  NOT NULL, -- 子品数量
    unit_cost       INT(18,4), -- 子品单位成本
    is_optional     VARCHAR(8)   DEFAULT 'N', -- 是否可选
    sort_no         SMALLINT     DEFAULT 0, -- 排序号
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_combo_item PRIMARY KEY (id),
    CONSTRAINT uk_wms_combo_item UNIQUE (combo_code, item_sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_combo_item_combo ON wms_combo_item(combo_code);
CREATE INDEX idx_wms_combo_item_sku ON wms_combo_item(item_sku_code);

-- 3. 组合组装单表
CREATE TABLE wms_combo_assemble (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    assemble_no     VARCHAR(64)  NOT NULL, -- 组装单号
    combo_code      VARCHAR(64)  NOT NULL, -- 组合品编码
    combo_name      VARCHAR(256),
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    assemble_qty    INT(18,4)  NOT NULL, -- 组装数量
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING待处理/PROCESSING处理中/COMPLETED已完成/CANCELLED已取消
    source_type     VARCHAR(32), -- 来源: MANUAL手动/AUTO自动/ORDER订单
    source_no       VARCHAR(64), -- 来源单号
    operator        VARCHAR(64),
    assemble_time   TIMESTAMP,
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_combo_assemble PRIMARY KEY (id),
    CONSTRAINT uk_wms_combo_assemble_no UNIQUE (assemble_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_combo_assemble_combo ON wms_combo_assemble(combo_code);
CREATE INDEX idx_wms_combo_assemble_status ON wms_combo_assemble(status);

-- 4. 组合拆解单表
CREATE TABLE wms_combo_disassemble (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    disassemble_no  VARCHAR(64)  NOT NULL, -- 拆解单号
    combo_code      VARCHAR(64)  NOT NULL,
    combo_name      VARCHAR(256),
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    disassemble_qty INT(18,4)  NOT NULL, -- 拆解数量
    status          VARCHAR(32) DEFAULT 'PENDING',
    source_type     VARCHAR(32),
    source_no       VARCHAR(64),
    operator        VARCHAR(64),
    disassemble_time TIMESTAMP,
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_combo_disassemble PRIMARY KEY (id),
    CONSTRAINT uk_wms_combo_disassemble_no UNIQUE (disassemble_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_combo_disassemble_combo ON wms_combo_disassemble(combo_code);
CREATE INDEX idx_wms_combo_disassemble_status ON wms_combo_disassemble(status);


-- 注释
ALTER TABLE wms_combo_rule COMMENT='组合规则表';
ALTER TABLE wms_combo_item COMMENT='组合明细表';
ALTER TABLE wms_combo_assemble COMMENT='组合组装单表';
ALTER TABLE wms_combo_disassemble COMMENT='组合拆解单表';
