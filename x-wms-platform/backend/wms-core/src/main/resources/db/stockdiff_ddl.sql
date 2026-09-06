-- ============================================================
-- X WMS 库存盘点差异管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 差异记录/差异处理/差异审批/差异分析
-- ============================================================

-- 1. 盘点差异记录表
CREATE TABLE wms_stock_diff (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    diff_id         VARCHAR(64)  NOT NULL, -- 差异ID
    stocktake_no    VARCHAR(64)  NOT NULL, -- 盘点单号
    stocktake_line  INT, -- 盘点行号
    warehouse_code  VARCHAR(64)  NOT NULL,
    owner_code      VARCHAR(64),
    location_code   VARCHAR(64)  NOT NULL,
    sku_code        VARCHAR(64)  NOT NULL,
    sku_name        VARCHAR(256),
    batch_no        VARCHAR(64),
    serial_no       VARCHAR(64),
    system_qty      INT(18,4)  NOT NULL, -- 系统库存
    counted_qty     INT(18,4)  NOT NULL, -- 实盘数量
    diff_qty        INT(18,4)  NOT NULL, -- 差异数量
    diff_rate       INT(10,4), -- 差异率(%)
    diff_type       VARCHAR(32)  NOT NULL, -- 差异类型: SHORTAGE盘亏/OVERAGE盘盈/DAMAGE损坏/EXPIRED过期/OTHER其他
    diff_level      VARCHAR(16)  NOT NULL, -- 差异级别: MINOR轻微/NORMAL正常/MAJOR重大/CRITICAL严重
    reason_code     VARCHAR(64), -- 原因编码
    reason_desc     VARCHAR(512), -- 原因描述
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING待处理/PROCESSING处理中/APPROVING审批中/RESOLVED已解决/CANCELLED已取消
    adjust_flag     VARCHAR(8)   DEFAULT 'N', -- 是否调整库存: Y/N
    adjust_no       VARCHAR(64), -- 调整单号
    handler         VARCHAR(64), -- 处理人
    handle_time     TIMESTAMP, -- 处理时间
    approver        VARCHAR(64), -- 审批人
    approve_time    TIMESTAMP, -- 审批时间
    approve_note    VARCHAR(512), -- 审批备注
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_stock_diff PRIMARY KEY (id),
    CONSTRAINT uk_wms_stock_diff_id UNIQUE (diff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_stock_diff_stocktake ON wms_stock_diff(stocktake_no);
CREATE INDEX idx_wms_stock_diff_sku ON wms_stock_diff(sku_code);
CREATE INDEX idx_wms_stock_diff_location ON wms_stock_diff(location_code);
CREATE INDEX idx_wms_stock_diff_status ON wms_stock_diff(status);
CREATE INDEX idx_wms_stock_diff_type ON wms_stock_diff(diff_type);

-- 2. 差异处理记录表
CREATE TABLE wms_stock_diff_handle (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    handle_id       VARCHAR(64)  NOT NULL, -- 处理ID
    diff_id         VARCHAR(64)  NOT NULL, -- 关联差异
    handle_type     VARCHAR(32)  NOT NULL, -- 处理类型: ADJUST调整/RECOUNT复盘/TRANSFER转移/DAMAGE报损/OTHER其他
    handle_action   VARCHAR(64), -- 处理动作
    handle_note     VARCHAR(512), -- 处理备注
    before_status   VARCHAR(32), -- 处理前状态
    after_status    VARCHAR(32), -- 处理后状态
    adjust_no       VARCHAR(64), -- 调整单号
    adjust_qty      INT(18,4), -- 调整数量
    operator        VARCHAR(64), -- 操作人
    handle_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_stock_diff_handle PRIMARY KEY (id),
    CONSTRAINT uk_wms_stock_diff_handle_id UNIQUE (handle_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_stock_diff_handle_diff ON wms_stock_diff_handle(diff_id);

-- 3. 差异审批记录表
CREATE TABLE wms_stock_diff_approve (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    approve_id      VARCHAR(64)  NOT NULL, -- 审批ID
    diff_id         VARCHAR(64)  NOT NULL, -- 关联差异
    approve_node    VARCHAR(64)  NOT NULL, -- 审批节点
    approve_role    VARCHAR(64), -- 审批角色
    approver        VARCHAR(64), -- 审批人
    approve_result  VARCHAR(32)  NOT NULL, -- 审批结果: APPROVED通过/REJECTED驳回/RETURNED退回
    approve_note    VARCHAR(512), -- 审批备注
    approve_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_stock_diff_approve PRIMARY KEY (id),
    CONSTRAINT uk_wms_stock_diff_approve_id UNIQUE (approve_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_stock_diff_approve_diff ON wms_stock_diff_approve(diff_id);

-- 4. 差异分析汇总表
CREATE TABLE wms_stock_diff_analysis (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    analysis_id     VARCHAR(64)  NOT NULL, -- 分析ID
    analysis_date   DATE          NOT NULL, -- 分析日期
    analysis_type   VARCHAR(32)  NOT NULL, -- 分析类型: DAILY日报/MONTHLY月报/STOCKTAKE单盘
    warehouse_code  VARCHAR(64),
    owner_code      VARCHAR(64),
    category_code   VARCHAR(64),
    sku_code        VARCHAR(64),
    location_code   VARCHAR(64),
    total_count     INT   DEFAULT 0, -- 总盘点数
    diff_count      INT   DEFAULT 0, -- 差异数
    shortage_count  INT   DEFAULT 0, -- 盘亏数
    overage_count   INT   DEFAULT 0, -- 盘盈数
    total_system_qty INT(18,4) DEFAULT 0, -- 总系统库存
    total_counted_qty INT(18,4) DEFAULT 0, -- 总实盘库存
    total_diff_qty  INT(18,4) DEFAULT 0, -- 总差异数量
    accuracy_rate   INT(10,4), -- 准确率(%)
    diff_rate       INT(10,4), -- 差异率(%)
    major_diff_count INT  DEFAULT 0, -- 重大差异数
    critical_diff_count INT DEFAULT 0, -- 严重差异数
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_stock_diff_analysis PRIMARY KEY (id),
    CONSTRAINT uk_wms_stock_diff_analysis_id UNIQUE (analysis_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_stock_diff_analysis_date ON wms_stock_diff_analysis(analysis_date);
CREATE INDEX idx_wms_stock_diff_analysis_wh ON wms_stock_diff_analysis(warehouse_code);


-- 注释
ALTER TABLE wms_stock_diff COMMENT='盘点差异记录表';
ALTER TABLE wms_stock_diff_handle COMMENT='差异处理记录表';
ALTER TABLE wms_stock_diff_approve COMMENT='差异审批记录表';
ALTER TABLE wms_stock_diff_analysis COMMENT='差异分析汇总表';
