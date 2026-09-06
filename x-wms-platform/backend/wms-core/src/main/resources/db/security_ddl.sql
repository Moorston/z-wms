-- ============================================================
-- X WMS 库存安全管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 数据权限/操作审计/安全策略/访问日志
-- ============================================================

-- 1. 数据权限表
CREATE TABLE wms_data_permission (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    permission_code VARCHAR(64)  NOT NULL, -- 权限编码
    permission_name VARCHAR(128) NOT NULL, -- 权限名称
    permission_type VARCHAR(32)  NOT NULL, -- 权限类型: WAREHOUSE仓库/OWNER货主/AREA库区/LOCATION库位/PRODUCT商品/DEPARTMENT部门
    resource_type   VARCHAR(32)  NOT NULL, -- 资源类型: INVENTORY库存/INBOUND入库/OUTBOUND出库/TRANSFER调拨/STOCKTAKE盘点/REPORT报表
    scope_type      VARCHAR(32)  NOT NULL, -- 范围类型: ALL全部/ASSIGNED指定/SELF自己/DEPARTMENT部门
    scope_values    TEXT, -- 范围值(JSON数组)
    role_code       VARCHAR(64), -- 关联角色
    user_code       VARCHAR(64), -- 关联用户
    warehouse_code  VARCHAR(64), -- 仓库
    owner_code      VARCHAR(64), -- 货主
    can_view        VARCHAR(8)  DEFAULT 'Y', -- 可查看: Y/N
    can_create      VARCHAR(8)  DEFAULT 'N', -- 可创建: Y/N
    can_update      VARCHAR(8)  DEFAULT 'N', -- 可修改: Y/N
    can_delete      VARCHAR(8)  DEFAULT 'N', -- 可删除: Y/N
    can_export      VARCHAR(8)  DEFAULT 'N', -- 可导出: Y/N
    can_approve     VARCHAR(8)  DEFAULT 'N', -- 可审批: Y/N
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    remark          VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_data_permission PRIMARY KEY (id),
    CONSTRAINT uk_wms_data_permission_code UNIQUE (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_data_permission_role ON wms_data_permission(role_code);
CREATE INDEX idx_wms_data_permission_user ON wms_data_permission(user_code);
CREATE INDEX idx_wms_data_permission_type ON wms_data_permission(permission_type);

-- 2. 操作审计表
CREATE TABLE wms_operation_audit (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    audit_id        VARCHAR(64)  NOT NULL, -- 审计ID
    trace_id        VARCHAR(64), -- 链路ID
    user_code       VARCHAR(64)  NOT NULL, -- 操作人
    user_name       VARCHAR(128), -- 操作人名称
    role_code       VARCHAR(64), -- 角色
    warehouse_code  VARCHAR(64), -- 仓库
    owner_code      VARCHAR(64), -- 货主
    module          VARCHAR(64)  NOT NULL, -- 模块: INVENTORY/INBOUND/OUTBOUND等
    operation       VARCHAR(64)  NOT NULL, -- 操作: CREATE/UPDATE/DELETE/APPROVE/EXPORT等
    operation_type  VARCHAR(32)  NOT NULL, -- 操作类型: QUERY查询/ADD新增/EDIT修改/DELETE删除/APPROVE审批/EXPORT导出/LOGIN登录/LOGOUT登出
    biz_type        VARCHAR(32), -- 业务类型
    biz_no          VARCHAR(64), -- 业务单号
    request_url     VARCHAR(512), -- 请求URL
    request_method  VARCHAR(16), -- 请求方法
    request_params  TEXT, -- 请求参数
    response_data   TEXT, -- 响应数据
    before_data     TEXT, -- 修改前数据
    after_data      TEXT, -- 修改后数据
    ip_address      VARCHAR(64), -- IP地址
    user_agent      VARCHAR(512), -- 客户端
    device_type     VARCHAR(32), -- 设备类型: PC/PDA/PACK_STATION
    status          VARCHAR(32) DEFAULT 'SUCCESS', -- SUCCESS成功/FAILED失败
    error_message   TEXT, -- 错误信息
    duration_ms     BIGINT, -- 耗时(毫秒)
    operation_time  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP, -- 操作时间
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_operation_audit PRIMARY KEY (id),
    CONSTRAINT uk_wms_operation_audit_id UNIQUE (audit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_operation_audit_user ON wms_operation_audit(user_code);
CREATE INDEX idx_wms_operation_audit_module ON wms_operation_audit(module);
CREATE INDEX idx_wms_operation_audit_time ON wms_operation_audit(operation_time);
CREATE INDEX idx_wms_operation_audit_biz ON wms_operation_audit(biz_type, biz_no);
CREATE INDEX idx_wms_operation_audit_wh ON wms_operation_audit(warehouse_code);

-- 3. 安全策略表
CREATE TABLE wms_security_policy (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    policy_code     VARCHAR(64)  NOT NULL, -- 策略编码
    policy_name     VARCHAR(128) NOT NULL, -- 策略名称
    policy_type     VARCHAR(32)  NOT NULL, -- 策略类型: PASSWORD密码/LOGIN登录/SESSION会话/ACCESS访问/DATA数据/API接口
    policy_scope    VARCHAR(32) DEFAULT 'GLOBAL', -- 策略范围: GLOBAL全局/WAREHOUSE仓库/ROLE角色/USER用户
    scope_value     VARCHAR(64), -- 范围值
    policy_config   TEXT, -- 策略配置(JSON)
    priority        SMALLINT    DEFAULT 100, -- 优先级
    is_enabled      VARCHAR(8)  DEFAULT 'Y', -- 是否启用: Y/N
    effective_date  DATE, -- 生效日期
    expire_date     DATE, -- 失效日期
    description     VARCHAR(512),
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_security_policy PRIMARY KEY (id),
    CONSTRAINT uk_wms_security_policy_code UNIQUE (policy_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_security_policy_type ON wms_security_policy(policy_type);
CREATE INDEX idx_wms_security_policy_scope ON wms_security_policy(policy_scope);

-- 4. 访问日志表
CREATE TABLE wms_access_log (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    log_id          VARCHAR(64)  NOT NULL, -- 日志ID
    trace_id        VARCHAR(64), -- 链路ID
    user_code       VARCHAR(64), -- 用户
    user_name       VARCHAR(128), -- 用户名称
    warehouse_code  VARCHAR(64), -- 仓库
    access_type     VARCHAR(32)  NOT NULL, -- 访问类型: LOGIN登录/LOGOUT登out/API访问/PAGE页面/DOWNLOAD下载/UPLOAD上传
    access_url      VARCHAR(512), -- 访问URL
    access_method   VARCHAR(16), -- 访问方法
    access_params   TEXT, -- 访问参数
    ip_address      VARCHAR(64), -- IP地址
    user_agent      VARCHAR(512), -- 客户端
    device_type     VARCHAR(32), -- 设备类型
    status_code     INT, -- 状态码
    response_time   BIGINT, -- 响应时间(毫秒)
    is_success      VARCHAR(8)  DEFAULT 'Y', -- 是否成功: Y/N
    error_message   VARCHAR(1024), -- 错误信息
    session_id      VARCHAR(128), -- 会话ID
    token_id        VARCHAR(128), -- Token ID
    access_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP, -- 访问时间
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_access_log PRIMARY KEY (id),
    CONSTRAINT uk_wms_access_log_id UNIQUE (log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_access_log_user ON wms_access_log(user_code);
CREATE INDEX idx_wms_access_log_type ON wms_access_log(access_type);
CREATE INDEX idx_wms_access_log_time ON wms_access_log(access_time);
CREATE INDEX idx_wms_access_log_ip ON wms_access_log(ip_address);
CREATE INDEX idx_wms_access_log_wh ON wms_access_log(warehouse_code);


-- 注释
ALTER TABLE wms_data_permission COMMENT='数据权限表';
ALTER TABLE wms_operation_audit COMMENT='操作审计表';
ALTER TABLE wms_security_policy COMMENT='安全策略表';
ALTER TABLE wms_access_log COMMENT='访问日志表';
