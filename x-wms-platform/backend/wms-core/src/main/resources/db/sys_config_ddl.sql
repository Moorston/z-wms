-- ============================================================
-- 系统参数配置表
-- 支持按模块/分类管理系统参数，支持仓库级/货主级参数覆盖
-- ============================================================

-- 1. 系统参数配置表
CREATE TABLE wms_sys_config (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    config_code     VARCHAR(128) NOT NULL, -- 参数编码（唯一）
    config_name     VARCHAR(256) NOT NULL, -- 参数名称
    config_value    VARCHAR(2000), -- 参数值
    default_value   VARCHAR(2000), -- 参数默认值
    config_type     VARCHAR(32)  DEFAULT 'STRING', -- 参数类型: STRING/INT/BOOLEAN/JSON/DATE
    category        VARCHAR(32), -- 参数分类: INBOUND/OUTBOUND/INVENTORY/QC/SYSTEM/INTERFACE/PRINT/WAVE/LOCATION/BATCH
    module_code     VARCHAR(64), -- 模块编码
    warehouse_code  VARCHAR(64), -- 仓库编码（为空表示全局参数）
    owner_code      VARCHAR(64), -- 货主编码（为空表示全局参数）
    enabled         VARCHAR(1)   DEFAULT 'Y', -- 是否启用: Y/N
    is_system       VARCHAR(1)   DEFAULT 'N', -- 是否系统内置: Y/N（内置参数不允许删除）
    allow_modify    VARCHAR(1)   DEFAULT 'Y', -- 是否允许修改: Y/N
    validate_rule   VARCHAR(512), -- 校验规则（正则表达式或枚举值）
    description     VARCHAR(1024), -- 参数描述
    remark          VARCHAR(512), -- 备注
    sort_order      INT    DEFAULT 0, -- 排序号
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_sys_config PRIMARY KEY (id),
    CONSTRAINT uk_wms_sys_config_code UNIQUE (config_code, warehouse_code, owner_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_sys_config_category ON wms_sys_config(category);
CREATE INDEX idx_wms_sys_config_module ON wms_sys_config(module_code);
CREATE INDEX idx_wms_sys_config_warehouse ON wms_sys_config(warehouse_code);
CREATE INDEX idx_wms_sys_config_owner ON wms_sys_config(owner_code);


-- 注释
ALTER TABLE wms_sys_config COMMENT='系统参数配置表';

-- ============================================================
-- 23个入库关键参数初始化数据
-- ============================================================

-- ASN_RLS_CTL: ASN默认释放状态
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'ASN_RLS_CTL', 'ASN默认释放状态', 'Y', 'Y', 'BOOLEAN', 'INBOUND', 'asn', 'Y', 'Y', 'Y', 'Y=已释放可收货/N=未释放需手动释放', 1, 'system');

-- RCV_TIM_CTL: 预期到货时间范围控制
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'RCV_TIM_CTL', '预期到货时间范围控制', 'N', 'N', 'BOOLEAN', 'INBOUND', 'receipt', 'Y', 'Y', 'Y', 'Y=仅在预期时间范围内可收货/N=不限制', 2, 'system');

-- PO_RLS_CTL: PO默认释放状态
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'PO_RLS_CTL', 'PO默认释放状态', 'Y', 'Y', 'BOOLEAN', 'INBOUND', 'po', 'Y', 'Y', 'Y', 'Y=已释放可提取/N=未释放需审核', 3, 'system');

-- SN#_CTL: 序列号管理模式
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'SN#_CTL', '序列号管理模式', '0', '0', 'INT', 'INBOUND', 'serial', 'Y', 'Y', 'Y', '0=不管理/1=1级单品/2=2级箱+单品', 4, 'system');

-- SN#_RCV_VAL: 入库序列号验证方式
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'SN#_RCV_VAL', '入库序列号验证方式', '1', '1', 'INT', 'INBOUND', 'serial', 'Y', 'Y', 'Y', '1=当前仓库/2=当前ASN/3=扫描队列', 5, 'system');

-- QC_RCV_CTL: 收货前质检控制
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'QC_RCV_CTL', '收货前质检控制', 'N', 'N', 'STRING', 'QC', 'qc', 'Y', 'Y', 'Y', 'Y=收货前质检/C=收货后质检/N=不质检', 6, 'system');

-- QC_PTA_CTL: 上架质检控制
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'QC_PTA_CTL', '上架质检控制', 'N', 'N', 'BOOLEAN', 'QC', 'qc', 'Y', 'Y', 'Y', 'Y=质检合格才能上架/N=不限制', 7, 'system');

-- QC_FRM_TRN: 收货后质检属性转移
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'QC_FRM_TRN', '收货后质检属性转移', 'N', 'N', 'BOOLEAN', 'QC', 'qc', 'Y', 'Y', 'Y', 'Y=质检后同步转移批次属性/N=不转移', 8, 'system');

-- MDT_EDT_CAL: 根据生产日期自动计算失效日期
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'MDT_EDT_CAL', '生产日期自动计算失效日期', 'N', 'N', 'BOOLEAN', 'INVENTORY', 'batch', 'Y', 'Y', 'Y', 'Y=根据生产日期+保质期自动计算/N=手动录入', 9, 'system');

-- RF_BRC_MOD: 盲收模式
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'RF_BRC_MOD', '盲收模式', 'NORMAL', 'NORMAL', 'STRING', 'INBOUND', 'receipt', 'Y', 'Y', 'Y', 'NORMAL=普通模式/SIMPLE=简化模式', 10, 'system');

-- RCV_AND_SRT: 整理收货模式
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'RCV_AND_SRT', '整理收货模式', 'N', 'N', 'BOOLEAN', 'INBOUND', 'receipt', 'Y', 'Y', 'Y', 'Y=开启整理收货(服装配比箱)/N=关闭', 11, 'system');

-- RCV_SHW_PIC: 快捷收货显示产品图片
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'RCV_SHW_PIC', '快捷收货显示产品图片', 'N', 'N', 'BOOLEAN', 'INBOUND', 'receipt', 'Y', 'Y', 'Y', 'Y=显示产品图片/N=不显示', 12, 'system');

-- RF_PTA_CFM: 上架库位校验模式
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'RF_PTA_CFM', '上架库位校验模式', 'WARN', 'WARN', 'STRING', 'INBOUND', 'putaway', 'Y', 'Y', 'Y', 'NONE=不校验/WARN=仅提示/FORCE=强制校验', 13, 'system');

-- RF_PTA_CHG: 上架是否允许修改数量
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'RF_PTA_CHG', '上架是否允许修改数量', 'N', 'N', 'BOOLEAN', 'INBOUND', 'putaway', 'Y', 'Y', 'Y', 'Y=允许修改数量(分多次上架)/N=不允许', 14, 'system');

-- CRS_LOC_CTL: 超大产品占用多库位控制
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'CRS_LOC_CTL', '超大产品占用多库位控制', 'N', 'N', 'BOOLEAN', 'LOCATION', 'location', 'Y', 'Y', 'Y', 'Y=开启封存库位(超大产品)/N=关闭', 15, 'system');

-- PTA_PCS_SCN: 上架逐件扫描模式
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'PTA_PCS_SCN', '上架逐件扫描模式', 'N', 'N', 'BOOLEAN', 'INBOUND', 'putaway', 'Y', 'Y', 'Y', 'Y=逐件扫描上架/N=按数量上架', 16, 'system');

-- PTA_SKU_CTL: 扫描跟踪号+SKU获取上架任务
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'PTA_SKU_CTL', '扫描跟踪号+SKU获取上架任务', 'N', 'N', 'BOOLEAN', 'INBOUND', 'putaway', 'Y', 'Y', 'Y', 'Y=需同时扫描LPN和SKU/N=仅扫描LPN', 17, 'system');

-- PAC_CTL: 在库包装处理方法
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'PAC_CTL', '在库包装处理方法', 'NONE', 'NONE', 'STRING', 'INVENTORY', 'consumable', 'Y', 'Y', 'Y', 'NONE=不处理/DEDUCT=扣减耗材/RECORD=仅记录', 18, 'system');

-- MOV_BTW_WH: 是否允许跨仓移库
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'MOV_BTW_WH', '是否允许跨仓移库', 'N', 'N', 'BOOLEAN', 'INVENTORY', 'inventory', 'Y', 'Y', 'Y', 'Y=允许跨仓移库/N=不允许', 19, 'system');

-- CUS_ORD_LNK: 订单类型与货主绑定
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'CUS_ORD_LNK', '订单类型与货主绑定', 'N', 'N', 'BOOLEAN', 'SYSTEM', 'system', 'Y', 'Y', 'Y', 'Y=订单类型绑定货主/N=不绑定', 20, 'system');

-- RCV_MIX_BOX: 收货是否允许混箱
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'RCV_MIX_BOX', '收货是否允许混箱', 'Y', 'Y', 'BOOLEAN', 'INBOUND', 'receipt', 'Y', 'Y', 'Y', 'Y=允许混箱/N=不允许', 21, 'system');

-- RCV_MIX_ASN: 收货是否允许混ASN
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'RCV_MIX_ASN', '收货是否允许混ASN', 'Y', 'Y', 'BOOLEAN', 'INBOUND', 'receipt', 'Y', 'Y', 'Y', 'Y=允许混ASN扫描/N=不允许', 22, 'system');

-- PTA_LOC_OVR: 上架是否允许覆盖库位
INSERT INTO wms_sys_config (id, config_code, config_name, config_value, default_value, config_type, category, module_code, enabled, is_system, allow_modify, description, sort_order, created_by)
VALUES (seq_wms_sys_config.NEXTVAL, 'PTA_LOC_OVR', '上架是否允许覆盖库位', 'N', 'N', 'BOOLEAN', 'INBOUND', 'putaway', 'Y', 'Y', 'Y', 'Y=允许覆盖推荐库位(需原因)/N=不允许', 23, 'system');

COMMIT;
