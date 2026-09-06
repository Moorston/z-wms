-- ============================================================
-- X WMS 库存审批流程管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 审批流程定义/审批节点/审批实例/审批记录
-- ============================================================

-- 1. 审批流程定义表
CREATE TABLE wms_approval_process (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    process_code    VARCHAR(64)  NOT NULL, -- 流程编码
    process_name    VARCHAR(128) NOT NULL, -- 流程名称
    process_type    VARCHAR(32)  NOT NULL, -- 流程类型: INVENTORY_ADJUST库存调整/INVENTORY_MOVE库存移库/INVENTORY_FREEZE库存冻结/INVENTORY_UNFREEZE库存解冻/STOCKTAKE_DIFF盘点差异/RETURN退货/PURCHASE采购
    warehouse_code  VARCHAR(64), -- 仓库
    owner_code      VARCHAR(64), -- 货主
    version         SMALLINT    DEFAULT 1, -- 版本号
    description     VARCHAR(512), -- 描述
    status          VARCHAR(32) DEFAULT 'ACTIVE', -- ACTIVE活跃/INACTIVE停用/DRAFT草稿
    effective_date  DATE, -- 生效日期
    expire_date     DATE, -- 失效日期
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_approval_process PRIMARY KEY (id),
    CONSTRAINT uk_wms_approval_process_code UNIQUE (process_code, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_approval_process_type ON wms_approval_process(process_type);
CREATE INDEX idx_wms_approval_process_wh ON wms_approval_process(warehouse_code);

-- 2. 审批节点表
CREATE TABLE wms_approval_node (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    node_code       VARCHAR(64)  NOT NULL, -- 节点编码
    node_name       VARCHAR(128) NOT NULL, -- 节点名称
    process_code    VARCHAR(64)  NOT NULL, -- 关联流程
    process_version SMALLINT    DEFAULT 1, -- 流程版本
    node_type       VARCHAR(32)  NOT NULL, -- 节点类型: START开始/APPROVAL审批/CC抄送/END结束
    node_order      SMALLINT    NOT NULL, -- 节点顺序
    approve_type    VARCHAR(32), -- 审批类型: ANY任一/ALL全部/MAJORITY多数
    approve_role    VARCHAR(64), -- 审批角色
    approve_users   VARCHAR(512), -- 审批用户列表
    approve_dept    VARCHAR(64), -- 审批部门
    timeout_hours   INT, -- 超时时间(小时)
    timeout_action  VARCHAR(32), -- 超时动作: AUTO_PASS自动通过/AUTO_REJECT自动拒绝/ESCALATE升级/NOTIFY仅通知
    next_node       VARCHAR(64), -- 下一节点
    reject_node     VARCHAR(64), -- 驳回节点
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_approval_node PRIMARY KEY (id),
    CONSTRAINT uk_wms_approval_node UNIQUE (process_code, process_version, node_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_approval_node_process ON wms_approval_node(process_code);

-- 3. 审批实例表
CREATE TABLE wms_approval_instance (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    instance_id     VARCHAR(64)  NOT NULL, -- 实例ID
    process_code    VARCHAR(64)  NOT NULL, -- 关联流程
    process_version SMALLINT    DEFAULT 1, -- 流程版本
    process_name    VARCHAR(128), -- 流程名称
    process_type    VARCHAR(32), -- 流程类型
    warehouse_code  VARCHAR(64),
    owner_code      VARCHAR(64),
    biz_type        VARCHAR(32)  NOT NULL, -- 业务类型
    biz_no          VARCHAR(64)  NOT NULL, -- 业务单号
    biz_data        TEXT, -- 业务数据(JSON)
    title           VARCHAR(256), -- 审批标题
    current_node    VARCHAR(64), -- 当前节点
    current_node_name VARCHAR(128), -- 当前节点名称
    status          VARCHAR(32) DEFAULT 'PENDING', -- PENDING待审批/APPROVING审批中/APPROVED已通过/REJECTED已驳回/CANCELLED已取消/TIMEOUT已超时
    submitter       VARCHAR(64)  NOT NULL, -- 提交人
    submit_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP, -- 提交时间
    approve_time    TIMESTAMP, -- 审批完成时间
    duration_ms     BIGINT, -- 耗时(毫秒)
    current_approvers VARCHAR(512), -- 当前审批人
    approve_count   SMALLINT    DEFAULT 0, -- 已审批人数
    total_approvers SMALLINT    DEFAULT 0, -- 总审批人数
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_approval_instance PRIMARY KEY (id),
    CONSTRAINT uk_wms_approval_instance_id UNIQUE (instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_approval_instance_biz ON wms_approval_instance(biz_type, biz_no);
CREATE INDEX idx_wms_approval_instance_status ON wms_approval_instance(status);
CREATE INDEX idx_wms_approval_instance_submitter ON wms_approval_instance(submitter);
CREATE INDEX idx_wms_approval_instance_wh ON wms_approval_instance(warehouse_code);

-- 4. 审批记录表
CREATE TABLE wms_approval_record (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    record_id       VARCHAR(64)  NOT NULL, -- 记录ID
    instance_id     VARCHAR(64)  NOT NULL, -- 关联实例
    node_code       VARCHAR(64)  NOT NULL, -- 节点编码
    node_name       VARCHAR(128), -- 节点名称
    node_order      SMALLINT, -- 节点顺序
    approver        VARCHAR(64)  NOT NULL, -- 审批人
    approve_role    VARCHAR(64), -- 审批角色
    approve_dept    VARCHAR(64), -- 审批部门
    action          VARCHAR(32)  NOT NULL, -- 操作: APPROVE通过/REJECT驳回/CC抄送/TRANSFER转办/WITHDRAW撤回
    opinion         VARCHAR(1024), -- 审批意见
    approve_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP, -- 审批时间
    duration_ms     BIGINT, -- 耗时(毫秒)
    from_node       VARCHAR(64), -- 来源节点
    to_node         VARCHAR(64), -- 目标节点
    attachment_url  VARCHAR(512), -- 附件URL
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_approval_record PRIMARY KEY (id),
    CONSTRAINT uk_wms_approval_record_id UNIQUE (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_approval_record_instance ON wms_approval_record(instance_id);
CREATE INDEX idx_wms_approval_record_approver ON wms_approval_record(approver);
CREATE INDEX idx_wms_approval_record_node ON wms_approval_record(node_code);


-- 注释
ALTER TABLE wms_approval_process COMMENT='审批流程定义表';
ALTER TABLE wms_approval_node COMMENT='审批节点表';
ALTER TABLE wms_approval_instance COMMENT='审批实例表';
ALTER TABLE wms_approval_record COMMENT='审批记录表';
