-- ============================================================
-- X WMS 上架任务管理 Sprint 3 DDL
-- 包含：上架原因代码表、上架推荐日志表、上架例外日志表
--       上架任务表字段扩展（派发/领取/工作区/优先级/异常）
-- ============================================================

-- 1. 上架原因代码表
CREATE TABLE wms_putaway_reason_code (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    reason_code     VARCHAR(32)    NOT NULL,
    reason_name     VARCHAR(128)   NOT NULL,
    reason_type     VARCHAR(32)    NOT NULL,
    sort_order      INT      DEFAULT 0,
    status          VARCHAR(16)    DEFAULT 'ACTIVE',
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64),
    updated_time    TIMESTAMP,
    CONSTRAINT pk_putaway_reason_code PRIMARY KEY (id),
    CONSTRAINT uk_putaway_reason_code UNIQUE (reason_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE wms_putaway_reason_code COMMENT='上架原因代码表';

CREATE INDEX idx_putaway_reason_type ON wms_putaway_reason_code(reason_type);


-- 初始化原因代码数据
INSERT INTO wms_putaway_reason_code (id, reason_code, reason_name, reason_type, sort_order, status) VALUES (seq_putaway_reason_code.NEXTVAL, 'LOC_FULL', '推荐库位已满', 'OVERRIDE', 1, 'ACTIVE');
INSERT INTO wms_putaway_reason_code (id, reason_code, reason_name, reason_type, sort_order, status) VALUES (seq_putaway_reason_code.NEXTVAL, 'LOC_LOCKED', '推荐库位被锁定', 'OVERRIDE', 2, 'ACTIVE');
INSERT INTO wms_putaway_reason_code (id, reason_code, reason_name, reason_type, sort_order, status) VALUES (seq_putaway_reason_code.NEXTVAL, 'LOC_WRONG', '推荐库位错误', 'OVERRIDE', 3, 'ACTIVE');
INSERT INTO wms_putaway_reason_code (id, reason_code, reason_name, reason_type, sort_order, status) VALUES (seq_putaway_reason_code.NEXTVAL, 'MANUAL', '人工指定库位', 'OVERRIDE', 4, 'ACTIVE');
INSERT INTO wms_putaway_reason_code (id, reason_code, reason_name, reason_type, sort_order, status) VALUES (seq_putaway_reason_code.NEXTVAL, 'NO_LOCATION', '无可用库位', 'EXCEPTION', 1, 'ACTIVE');
INSERT INTO wms_putaway_reason_code (id, reason_code, reason_name, reason_type, sort_order, status) VALUES (seq_putaway_reason_code.NEXTVAL, 'QTY_DIFF', '数量差异', 'EXCEPTION', 2, 'ACTIVE');
INSERT INTO wms_putaway_reason_code (id, reason_code, reason_name, reason_type, sort_order, status) VALUES (seq_putaway_reason_code.NEXTVAL, 'BATCH_MISMATCH', '批次不匹配', 'EXCEPTION', 3, 'ACTIVE');
INSERT INTO wms_putaway_reason_code (id, reason_code, reason_name, reason_type, sort_order, status) VALUES (seq_putaway_reason_code.NEXTVAL, 'EQUIPMENT_FAIL', '设备故障', 'EXCEPTION', 4, 'ACTIVE');
INSERT INTO wms_putaway_reason_code (id, reason_code, reason_name, reason_type, sort_order, status) VALUES (seq_putaway_reason_code.NEXTVAL, 'OVER_QTY', '上架数量超收', 'DIFFERENCE', 1, 'ACTIVE');
INSERT INTO wms_putaway_reason_code (id, reason_code, reason_name, reason_type, sort_order, status) VALUES (seq_putaway_reason_code.NEXTVAL, 'SHORT_QTY', '上架数量短少', 'DIFFERENCE', 2, 'ACTIVE');

-- ============================================================

-- 2. 上架推荐日志表
CREATE TABLE wms_putaway_recommend_log (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    log_no              VARCHAR(32)    NOT NULL,
    task_no             VARCHAR(32),
    detail_no           VARCHAR(32),
    warehouse_code      VARCHAR(32),
    owner_code          VARCHAR(32),
    sku_code            VARCHAR(64),
    batch_no            VARCHAR(64),
    putaway_qty         INT(18,4),
    rule_id             BIGINT,
    rule_code           VARCHAR(32),
    hit_line_no         INT,
    candidate_count     INT,
    recommended_location VARCHAR(64),
    recommended_area    VARCHAR(32),
    recommend_score     INT(10,2),
    distance_score      INT(10,2),
    capacity_score      INT(10,2),
    turnover_score      INT(10,2),
    mix_score           INT(10,2),
    is_override         CHAR(1)         DEFAULT 'N',
    override_reason_code VARCHAR(32),
    actual_location     VARCHAR(64),
    response_time_ms    BIGINT,
    recommend_time      TIMESTAMP,
    remark              VARCHAR(512),
    CONSTRAINT pk_putaway_recommend_log PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE wms_putaway_recommend_log COMMENT='上架推荐日志表';

CREATE INDEX idx_putaway_rec_log_task ON wms_putaway_recommend_log(task_no);
CREATE INDEX idx_putaway_rec_log_sku ON wms_putaway_recommend_log(sku_code, warehouse_code);
CREATE INDEX idx_putaway_rec_log_time ON wms_putaway_recommend_log(recommend_time);


-- ============================================================

-- 3. 上架例外日志表
CREATE TABLE wms_putaway_exception_log (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    log_no              VARCHAR(32)    NOT NULL,
    task_no             VARCHAR(32),
    detail_no           VARCHAR(32),
    warehouse_code      VARCHAR(32),
    sku_code            VARCHAR(64),
    exception_type      VARCHAR(32)    NOT NULL,
    reason_code         VARCHAR(32),
    reason_desc         VARCHAR(512),
    original_location   VARCHAR(64),
    actual_location     VARCHAR(64),
    operator            VARCHAR(64),
    operate_time        TIMESTAMP,
    handle_status       VARCHAR(16)    DEFAULT 'PENDING',
    handler             VARCHAR(64),
    handle_time         TIMESTAMP,
    handle_result       VARCHAR(1024),
    remark              VARCHAR(512),
    CONSTRAINT pk_putaway_exception_log PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE wms_putaway_exception_log COMMENT='上架例外日志表';

CREATE INDEX idx_putaway_exc_log_task ON wms_putaway_exception_log(task_no);
CREATE INDEX idx_putaway_exc_log_status ON wms_putaway_exception_log(handle_status);
CREATE INDEX idx_putaway_exc_log_type ON wms_putaway_exception_log(exception_type);
CREATE INDEX idx_putaway_exc_log_time ON wms_putaway_exception_log(operate_time);


-- ============================================================

-- 4. 上架任务表字段扩展
ALTER TABLE wms_putaway_task ADD (
    work_zone               VARCHAR(32),
    aisle_no                VARCHAR(32),
    priority                INT      DEFAULT 3,
    assigner                VARCHAR(64),
    assign_time             TIMESTAMP,
    assignee                VARCHAR(64),
    claim_time              TIMESTAMP,
    exception_reason_code   VARCHAR(32),
    exception_remark        VARCHAR(512)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE INDEX idx_putaway_task_status ON wms_putaway_task(status);
CREATE INDEX idx_putaway_task_assignee ON wms_putaway_task(assignee);
CREATE INDEX idx_putaway_task_workzone ON wms_putaway_task(work_zone, status);
CREATE INDEX idx_putaway_task_priority ON wms_putaway_task(priority, status);

-- ============================================================
-- 提交
-- ============================================================
COMMIT;
