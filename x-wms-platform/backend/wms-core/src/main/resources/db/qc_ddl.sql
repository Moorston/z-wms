-- ============================================================
-- X WMS 质检管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 质检规则/质检单/质检明细/抽样方案/不合格品/让步接收
-- ============================================================

-- 1. 质检规则表
CREATE TABLE wms_qc_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL,
    rule_name       VARCHAR(128) NOT NULL,
    sku             VARCHAR(64),
    supplier_code   VARCHAR(64),
    owner_code      VARCHAR(64),
    warehouse_code  VARCHAR(64),
    qc_type         VARCHAR(16)  NOT NULL,  -- FULL全检/SAMPLE抽检/NONE免检
    aql_level       VARCHAR(16),            -- AQL水平: 0.065/0.10/0.15/0.25/0.40/0.65/1.0/1.5/2.5/4.0/6.5
    inspection_level VARCHAR(8),            -- 检验水平: S-1/S-2/S-3/S-4/I/II/III
    strictness      VARCHAR(16)  DEFAULT 'NORMAL', -- NORMAL正常/REDUCED放宽/TIGHTENED加严
    first_batch_full TINYINT(1)   DEFAULT 0,  -- 新供应商前N批全检
    full_check_count SMALLINT   DEFAULT 3,
    status          VARCHAR(16)  DEFAULT 'ENABLED',
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    deleted         TINYINT(1)     DEFAULT 0,
    CONSTRAINT pk_wms_qc_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_qc_rule_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_qc_rule_sku ON wms_qc_rule(sku);
CREATE INDEX idx_wms_qc_rule_supplier ON wms_qc_rule(supplier_code);

-- 2. 抽样方案表 (GB/T 2828.1 标准)
CREATE TABLE wms_qc_sampling_plan (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    plan_code       VARCHAR(64)  NOT NULL,
    inspection_level VARCHAR(8)  NOT NULL,
    lot_size_from   INT    NOT NULL,
    lot_size_to     INT    NOT NULL,
    sample_size_code VARCHAR(8)  NOT NULL,
    sample_size     INT    NOT NULL,
    aql_0_065_accept SMALLINT,
    aql_0_065_reject SMALLINT,
    aql_0_10_accept SMALLINT,
    aql_0_10_reject SMALLINT,
    aql_0_15_accept SMALLINT,
    aql_0_15_reject SMALLINT,
    aql_0_25_accept SMALLINT,
    aql_0_25_reject SMALLINT,
    aql_0_40_accept SMALLINT,
    aql_0_40_reject SMALLINT,
    aql_0_65_accept SMALLINT,
    aql_0_65_reject SMALLINT,
    aql_1_0_accept  SMALLINT,
    aql_1_0_reject  SMALLINT,
    aql_1_5_accept  SMALLINT,
    aql_1_5_reject  SMALLINT,
    aql_2_5_accept  SMALLINT,
    aql_2_5_reject  SMALLINT,
    aql_4_0_accept  SMALLINT,
    aql_4_0_reject  SMALLINT,
    aql_6_5_accept  SMALLINT,
    aql_6_5_reject  SMALLINT,
    strictness      VARCHAR(16)  DEFAULT 'NORMAL',
    CONSTRAINT pk_wms_qc_sampling PRIMARY KEY (id),
    CONSTRAINT uk_wms_qc_sampling_plan UNIQUE (plan_code, inspection_level, strictness)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 质检单表
CREATE TABLE wms_qc_order (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    qc_no           VARCHAR(64)  NOT NULL,
    ref_type        VARCHAR(32)  NOT NULL,  -- INBOUND入库/RETURN退货/INSTOCK在库/OUTBOUND出库
    ref_no          VARCHAR(64)  NOT NULL,
    ref_item_id     BIGINT,
    sku             VARCHAR(64)  NOT NULL,
    barcode         VARCHAR(64),
    product_name    VARCHAR(256),
    supplier_code   VARCHAR(64),
    owner_code      VARCHAR(64),
    warehouse_code  VARCHAR(64),
    location_code   VARCHAR(64),
    batch_no        VARCHAR(64),
    qc_type         VARCHAR(16)  NOT NULL,  -- FULL/SAMPLE/NONE
    lot_qty         DECIMAL(14,4) NOT NULL,   -- 批量
    sample_qty      DECIMAL(14,4),            -- 抽样数量
    inspected_qty   DECIMAL(14,4) DEFAULT 0,  -- 已检数量
    qualified_qty   DECIMAL(14,4) DEFAULT 0,  -- 合格数量
    unqualified_qty DECIMAL(14,4) DEFAULT 0,  -- 不合格数量
    aql_level       VARCHAR(16),
    accept_number   SMALLINT,               -- 接收数
    reject_number   SMALLINT,               -- 拒收数
    defect_count    INT    DEFAULT 0, -- 不良数
    result          VARCHAR(16),            -- PASSED合格/FAILED不合格/CONCESSION让步接收
    status          VARCHAR(32)  NOT NULL,  -- PENDING待检/INSPECTING检验中/PASSED合格/FAILED不合格/CONCESSION_PENDING待审批/CONCESSION_APPROVED审批通过/DISPOSED已处理
    inspector       VARCHAR(64),
    inspector2      VARCHAR(64),            -- 双人复核(GSP)
    inspect_time    TIMESTAMP,
    start_time      TIMESTAMP,
    finish_time     TIMESTAMP,
    remark          VARCHAR(1024),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    deleted         TINYINT(1)     DEFAULT 0,
    CONSTRAINT pk_wms_qc_order PRIMARY KEY (id),
    CONSTRAINT uk_wms_qc_order_no UNIQUE (qc_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_qc_order_ref ON wms_qc_order(ref_type, ref_no);
CREATE INDEX idx_wms_qc_order_sku ON wms_qc_order(sku);
CREATE INDEX idx_wms_qc_order_status ON wms_qc_order(status);
CREATE INDEX idx_wms_qc_order_owner ON wms_qc_order(owner_code_col);

-- 4. 质检明细表
CREATE TABLE wms_qc_item (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    qc_id           BIGINT    NOT NULL,
    item_code       VARCHAR(64)  NOT NULL,  -- 检验项编码
    item_name       VARCHAR(128) NOT NULL,  -- 检验项名称: 外观/数量/规格/效期/功能/包装/温湿度
    category        VARCHAR(32),            -- 分类: APPEARANCE外观/QUANTITY数量/SPEC规格/EXPIRY效期/FUNCTION功能/PACKAGING包装/TEMPERATURE温湿度/OTHER其他
    standard        VARCHAR(512),           -- 标准值
    actual_value    VARCHAR(512),           -- 实际值
    unit            VARCHAR(32),
    result          VARCHAR(16),            -- PASS合格/FAIL不合格/NA不适用
    severity        VARCHAR(16),            -- CRITICAL严重/MAJOR主要/MINOR次要
    remark          VARCHAR(512),
    sort_order      SMALLINT     DEFAULT 0,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_qc_item PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_qc_item_qc ON wms_qc_item(qc_id);

-- 5. 不合格品表
CREATE TABLE wms_qc_unqualified (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    qc_id           BIGINT    NOT NULL,
    sku             VARCHAR(64)  NOT NULL,
    barcode         VARCHAR(64),
    batch_no        VARCHAR(64),
    qty             DECIMAL(14,4) NOT NULL,
    unqualified_type VARCHAR(32),           -- APPEARANCE外观/QUANTITY数量/SPEC规格/EXPIRY效期/FUNCTION功能/PACKAGING包装/OTHER其他
    defect_desc     VARCHAR(1024),
    handle_method   VARCHAR(32),            -- RETURN_SUPPLIER退供/DESTROY销毁/REWORK返工/DOWNGRADE降级/PENDING待处理
    handle_status   VARCHAR(32)  DEFAULT 'PENDING', -- PENDING待处理/PROCESSING处理中/DISPOSED已处理
    location_code   VARCHAR(64),            -- 不合格品区库位
    handler         VARCHAR(64),
    handle_time     TIMESTAMP,
    dispose_cert    VARCHAR(256),           -- 销毁证明文件
    remark          VARCHAR(512),
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    deleted         TINYINT(1)     DEFAULT 0,
    CONSTRAINT pk_wms_qc_unqualified PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_qc_unq_qc ON wms_qc_unqualified(qc_id);
CREATE INDEX idx_wms_qc_unq_sku ON wms_qc_unqualified(sku);

-- 6. 让步接收表
CREATE TABLE wms_qc_concession (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    qc_id           BIGINT    NOT NULL,
    sku             VARCHAR(64)  NOT NULL,
    batch_no        VARCHAR(64),
    qty             DECIMAL(14,4) NOT NULL,
    reason          VARCHAR(1024) NOT NULL, -- 让步原因
    defect_desc     VARCHAR(1024),          -- 缺陷描述
    impact_level    VARCHAR(16),            -- 影响程度: LOW低/MEDIUM中/HIGH高
    applicant       VARCHAR(64)  NOT NULL,
    apply_time      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    approver        VARCHAR(64),
    approve_time    TIMESTAMP,
    approve_opinion VARCHAR(1024),
    status          VARCHAR(32)  NOT NULL,  -- PENDING待审批/APPROVED通过/REJECTED拒绝
    owner_code_col  VARCHAR(64),
    warehouse_code_col VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_qc_concession PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_qc_conc_qc ON wms_qc_concession(qc_id);

-- 7. 供应商质量评分表
CREATE TABLE wms_supplier_quality (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    supplier_code   VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    stat_period     VARCHAR(16)  NOT NULL,  -- 统计周期: 2026-08
    total_batches   INT    DEFAULT 0, -- 总批次
    passed_batches  INT    DEFAULT 0, -- 合格批次
    total_qty       DECIMAL(14,4)  DEFAULT 0,
    defect_qty      DECIMAL(14,4)  DEFAULT 0,
    pass_rate       INT(5,2),             -- 批次合格率 %
    defect_rate     INT(5,4),             -- 不良率 %
    on_time_rate    INT(5,2),             -- 交货及时率 %
    score           INT(5,2),             -- 综合评分
    rank_level      VARCHAR(16),            -- A优秀/B良好/C合格/D不合格
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_supplier_quality PRIMARY KEY (id),
    CONSTRAINT uk_wms_supplier_quality UNIQUE (supplier_code, stat_period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 注释
ALTER TABLE wms_qc_rule COMMENT='质检规则表';
ALTER TABLE wms_qc_sampling_plan COMMENT='抽样方案表(GB/T 2828.1)';
ALTER TABLE wms_qc_order COMMENT='质检单表';
ALTER TABLE wms_qc_item COMMENT='质检明细表';
ALTER TABLE wms_qc_unqualified COMMENT='不合格品表';
ALTER TABLE wms_qc_concession COMMENT='让步接收表';
ALTER TABLE wms_supplier_quality COMMENT='供应商质量评分表';

-- 8. 质检任务表（关联收货/上架任务，控制收货/上架权限）
CREATE TABLE wms_qc_task (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    task_no         VARCHAR(64)  NOT NULL, -- 质检任务号
    qc_no           VARCHAR(64), -- 关联质检单号
    qc_timing       VARCHAR(32), -- 质检时机: BEFORE_RECEIVE收货前/AFTER_RECEIVE收货后/BEFORE_PUTAWAY上架前
    qc_type         VARCHAR(16), -- 质检类型: FULL全检/SAMPLE抽检/NONE免检
    asn_no          VARCHAR(64), -- 关联ASN号
    inbound_no      VARCHAR(64), -- 关联入库单号
    inbound_detail_no VARCHAR(64), -- 关联入库明细号
    receipt_task_no VARCHAR(64), -- 关联收货任务号
    receipt_detail_no VARCHAR(64), -- 关联收货任务明细号
    putaway_task_no VARCHAR(64), -- 关联上架任务号
    putaway_detail_no VARCHAR(64), -- 关联上架任务明细号
    sku_code        VARCHAR(64), -- 商品编码
    sku_name        VARCHAR(256), -- 商品名称
    barcode         VARCHAR(64), -- 商品条码
    supplier_code   VARCHAR(64), -- 供应商编码
    owner_code      VARCHAR(64), -- 货主编码
    warehouse_code  VARCHAR(64), -- 仓库编码
    batch_no        VARCHAR(64), -- 批次号
    lot_qty         INT(18,4)  DEFAULT 0, -- 质检数量
    sample_qty      INT(18,4)  DEFAULT 0, -- 抽样数量
    inspected_qty   INT(18,4)  DEFAULT 0, -- 已检验数量
    qualified_qty   INT(18,4)  DEFAULT 0, -- 合格数量
    unqualified_qty INT(18,4)  DEFAULT 0, -- 不合格数量
    qc_result       VARCHAR(32), -- 质检结果: PASSED/FAILED/CONCESSION
    status          VARCHAR(32)  DEFAULT 'PENDING', -- 状态: PENDING/INSPECTING/PASSED/FAILED/CONCESSION/DISPOSED/CANCELLED
    block_receive   VARCHAR(1)   DEFAULT 'N', -- 是否阻塞收货
    block_putaway   VARCHAR(1)   DEFAULT 'N', -- 是否阻塞上架
    inspector       VARCHAR(64), -- 质检员
    start_time      TIMESTAMP, -- 开始时间
    finish_time     TIMESTAMP, -- 完成时间
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_qc_task PRIMARY KEY (id),
    CONSTRAINT uk_wms_qc_task_no UNIQUE (task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_qc_task_asn ON wms_qc_task(asn_no);
CREATE INDEX idx_wms_qc_task_inbound ON wms_qc_task(inbound_no);
CREATE INDEX idx_wms_qc_task_receipt ON wms_qc_task(receipt_task_no);
CREATE INDEX idx_wms_qc_task_putaway ON wms_qc_task(putaway_task_no);
CREATE INDEX idx_wms_qc_task_status ON wms_qc_task(status);
CREATE INDEX idx_wms_qc_task_timing ON wms_qc_task(qc_timing);
CREATE INDEX idx_wms_qc_task_sku ON wms_qc_task(sku_code);

-- 9. 质检样本表
CREATE TABLE wms_qc_sample (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    sample_no       VARCHAR(64)  NOT NULL, -- 样本号
    task_no         VARCHAR(64), -- 关联质检任务号
    qc_no           VARCHAR(64), -- 关联质检单号
    sku_code        VARCHAR(64), -- 商品编码
    sku_name        VARCHAR(256), -- 商品名称
    batch_no        VARCHAR(64), -- 批次号
    serial_no       VARCHAR(64), -- 序列号
    sample_qty      INT(18,4)  DEFAULT 0, -- 样本数量
    status          VARCHAR(32)  DEFAULT 'DRAWN', -- 状态: DRAWN/TESTING/PASSED/FAILED/RETURNED/DESTROYED
    defect_type     VARCHAR(32), -- 缺陷类型: CRITICAL/MAJOR/MINOR
    defect_desc     VARCHAR(512), -- 缺陷描述
    test_item       VARCHAR(128), -- 检测项目
    test_standard   VARCHAR(512), -- 检测标准
    test_method     VARCHAR(256), -- 检测方法
    test_device     VARCHAR(128), -- 检测设备
    test_value      VARCHAR(256), -- 检测值
    upper_limit     VARCHAR(64), -- 标准值上限
    lower_limit     VARCHAR(64), -- 标准值下限
    is_qualified    VARCHAR(1), -- 是否合格: Y/N
    drawn_by        VARCHAR(64), -- 抽样人
    drawn_time      TIMESTAMP, -- 抽样时间
    tested_by       VARCHAR(64), -- 检测人
    tested_time     TIMESTAMP, -- 检测时间
    returned_by     VARCHAR(64), -- 归还人
    returned_time   TIMESTAMP, -- 归还时间
    remark          VARCHAR(512), -- 备注
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_qc_sample PRIMARY KEY (id),
    CONSTRAINT uk_wms_qc_sample_no UNIQUE (sample_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_qc_sample_task ON wms_qc_sample(task_no);
CREATE INDEX idx_wms_qc_sample_qc ON wms_qc_sample(qc_no);
CREATE INDEX idx_wms_qc_sample_sku ON wms_qc_sample(sku_code);
CREATE INDEX idx_wms_qc_sample_status ON wms_qc_sample(status);

-- 新增序列

-- 新增注释
ALTER TABLE wms_qc_task COMMENT='质检任务表（关联收货/上架任务）';
ALTER TABLE wms_qc_sample COMMENT='质检样本表';
