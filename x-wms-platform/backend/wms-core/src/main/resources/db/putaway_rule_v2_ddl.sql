-- ============================================================
-- X WMS 上架策略模块 DDL (Sprint 1)
-- 数据库: MySQL 8.x
-- 包含: 更新wms_putaway_rule表 + 新增wms_putaway_rule_line表
-- ============================================================

-- 1. 更新 wms_putaway_rule 表（添加新字段）
ALTER TABLE wms_putaway_rule ADD (
    warehouse_codes VARCHAR(1024),      -- 适用仓库列表（JSON数组，支持多仓库）
    version INT DEFAULT 1,         -- 版本号
    effective_date DATE,                   -- 生效日期
    expire_date DATE                       -- 失效日期
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 添加注释

-- 2. 新增 wms_putaway_rule_line 表（上架规则行表）
CREATE TABLE wms_putaway_rule_line (
    id                      BIGINT    NOT NULL AUTO_INCREMENT,
    rule_id                 BIGINT    NOT NULL,       -- 规则头ID
    line_no                 SMALLINT     NOT NULL,       -- 行号（1-100，规则内唯一）
    description             VARCHAR(512),                 -- 行描述
    condition_json          TEXT,                          -- 行条件JSON：{"orderType":"NORMAL","packageLevel":"CASE","cycleLevel":"A","batchAttr":{"key":"origin","value":"广东"}}
    rule_code               VARCHAR(8)     NOT NULL,     -- 规则代码（01-31）
    target_zone             VARCHAR(32),                  -- 目标库区（规则代码02/03/21时必填）
    target_location         VARCHAR(32),                  -- 目标库位（规则代码01/04/07/22时必填）
    location_limits_json    TEXT,                          -- 库位限制JSON数组：[{"type":"NO_MIX_LOT"},{"type":"SAME_PRODUCT_GROUP","value":"GROUP001"}]
    space_limits_json       TEXT,                          -- 空间限制JSON数组：[{"type":"VOLUME","threshold":2.5},{"type":"WEIGHT","threshold":1000}]
    extended_constraints_json TEXT,                        -- 扩展约束JSON数组：[{"type":"CYCLE_ZONE","value":"A"},{"type":"SKU_LOCATION_LIMIT","maxLocations":5}]
    success_jump_line       SMALLINT,                     -- 成功跳转行号（为空则规则结束）
    fail_jump_line          SMALLINT,                     -- 失败跳转行号（为空则下一行）
    created_by              VARCHAR(64),
    created_time            TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by              VARCHAR(64),
    updated_time            TIMESTAMP,
    CONSTRAINT pk_wms_putaway_rule_line PRIMARY KEY (id),
    CONSTRAINT uk_wms_rule_line_rule_lineno UNIQUE (rule_id, line_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 索引
CREATE INDEX idx_wms_rule_line_rule ON wms_putaway_rule_line(rule_id);
CREATE INDEX idx_wms_rule_line_code ON wms_putaway_rule_line(rule_code);


-- 注释
ALTER TABLE wms_putaway_rule_line COMMENT='上架规则行表（规则链，按行号顺序执行）';

-- ============================================================
-- 3. 初始化示例数据（电商3PL推荐配置）
-- ============================================================

-- 示例规则头
INSERT INTO wms_putaway_rule (id, rule_code, rule_name, rule_type, warehouse_code, strategy, priority, status, version, created_by, created_time)
VALUES (seq_wms_putaway_rule.NEXTVAL, 'PAR-SAMPLE-001', '电商3PL标准上架规则', 'WAREHOUSE', 'WH001', 'ZONE', 10, 'ACTIVE', 1, 'system', CURRENT_TIMESTAMP);

-- 示例规则行（需先获取规则ID，这里用变量示意）
-- 实际执行时需根据上面插入的rule_id来插入规则行
-- 规则行1: 整托→存储区
-- 规则行2: A类→拣货位
-- 规则行3: B类→拣货区
-- 规则行4: C类→存储区
-- 规则行5: 退货→QC区
-- 规则行6: 兜底→TEMP

COMMIT;
