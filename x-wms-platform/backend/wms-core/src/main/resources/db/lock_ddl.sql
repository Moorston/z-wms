-- ============================================================
-- X WMS 库存锁管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 库存锁规则/库存锁记录/锁等待队列/死锁检测日志
-- ============================================================

-- 1. 库存锁规则表
CREATE TABLE wms_lock_rule (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    rule_code       VARCHAR(64)  NOT NULL, -- 规则编码
    rule_name       VARCHAR(128) NOT NULL, -- 规则名称
    lock_scope      VARCHAR(32)  NOT NULL, -- 锁范围: SKU/LOCATION/BATCH/WAREHOUSE/OWNER
    lock_type       VARCHAR(32)  NOT NULL, -- 锁类型: SHARED共享/EXCLUSIVE排他
    max_wait_time   INT    DEFAULT 30, -- 最大等待时间(秒)
    timeout_time    INT    DEFAULT 300, -- 超时时间(秒)
    retry_count     SMALLINT     DEFAULT 3, -- 重试次数
    retry_interval  INT    DEFAULT 1000, -- 重试间隔(毫秒)
    deadlock_detect VARCHAR(8)   DEFAULT 'Y', -- 是否死锁检测
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_lock_rule PRIMARY KEY (id),
    CONSTRAINT uk_wms_lock_rule_code UNIQUE (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 库存锁记录表
CREATE TABLE wms_inventory_lock (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    lock_id         VARCHAR(64)  NOT NULL, -- 锁ID
    lock_key        VARCHAR(256) NOT NULL, -- 锁键(如 SKU:SKU001:LOC:A01)
    lock_scope      VARCHAR(32)  NOT NULL, -- 锁范围
    lock_type       VARCHAR(32)  NOT NULL, -- 锁类型: SHARED/EXCLUSIVE
    warehouse_code  VARCHAR(64),
    owner_code      VARCHAR(64),
    sku_code        VARCHAR(64),
    location_code   VARCHAR(64),
    batch_no        VARCHAR(64),
    lock_quantity   INT(18,4), -- 锁定数量
    business_type   VARCHAR(64), -- 业务类型: OUTBOUND/INBOUND/TRANSFER/ADJUST/REPLENISH
    business_no     VARCHAR(64), -- 业务单号
    holder          VARCHAR(64)  NOT NULL, -- 持有者(线程/服务)
    holder_ip       VARCHAR(64), -- 持有者IP
    status          VARCHAR(32) DEFAULT 'HELD', -- HELD持有/WAITING等待/RELEASED已释放/TIMEOUT超时/DEADLOCK死锁
    acquire_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP, -- 获取时间
    expire_time     TIMESTAMP, -- 过期时间
    release_time    TIMESTAMP, -- 释放时间
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_inv_lock PRIMARY KEY (id),
    CONSTRAINT uk_wms_inv_lock_id UNIQUE (lock_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inv_lock_key ON wms_inventory_lock(lock_key);
CREATE INDEX idx_wms_inv_lock_status ON wms_inventory_lock(status);
CREATE INDEX idx_wms_inv_lock_biz ON wms_inventory_lock(business_no);
CREATE INDEX idx_wms_inv_lock_holder ON wms_inventory_lock(holder);

-- 3. 锁等待队列表
CREATE TABLE wms_lock_wait_queue (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    wait_id         VARCHAR(64)  NOT NULL, -- 等待ID
    lock_key        VARCHAR(256) NOT NULL, -- 等待的锁键
    lock_type       VARCHAR(32)  NOT NULL, -- 请求锁类型
    requester       VARCHAR(64)  NOT NULL, -- 请求者
    requester_ip    VARCHAR(64),
    business_no     VARCHAR(64),
    wait_start_time TIMESTAMP     DEFAULT CURRENT_TIMESTAMP, -- 等待开始时间
    wait_timeout    TIMESTAMP, -- 等待超时时间
    status          VARCHAR(32) DEFAULT 'WAITING', -- WAITING等待中/ACQUIRED已获取/TIMEOUT超时/CANCELLED已取消
    acquire_time    TIMESTAMP, -- 获取时间
    priority        SMALLINT     DEFAULT 5, -- 优先级
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_lock_wait PRIMARY KEY (id),
    CONSTRAINT uk_wms_lock_wait_id UNIQUE (wait_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_lock_wait_key ON wms_lock_wait_queue(lock_key);
CREATE INDEX idx_wms_lock_wait_status ON wms_lock_wait_queue(status);

-- 4. 死锁检测日志表
CREATE TABLE wms_deadlock_log (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    log_id          VARCHAR(64)  NOT NULL, -- 日志ID
    detect_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP, -- 检测时间
    lock_key        VARCHAR(256), -- 涉及锁键
    involved_locks  TEXT, -- 涉及的锁(JSON)
    involved_holders TEXT, -- 涉及持有者(JSON)
    victim_lock_id  VARCHAR(64), -- 被牺牲的锁ID
    victim_holder   VARCHAR(64), -- 被牺牲的持有者
    resolve_action  VARCHAR(32), -- 解决方式: KILL_VICTIM牺牲/ROLLBACK回滚/WAIT等待
    resolve_result  VARCHAR(32), -- 解决结果: SUCCESS/FAILED
    resolve_time    TIMESTAMP, -- 解决时间
    detail          TEXT, -- 详细信息
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_deadlock_log PRIMARY KEY (id),
    CONSTRAINT uk_wms_deadlock_log_id UNIQUE (log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_deadlock_time ON wms_deadlock_log(detect_time);


-- 注释
ALTER TABLE wms_lock_rule COMMENT='库存锁规则表';
ALTER TABLE wms_inventory_lock COMMENT='库存锁记录表';
ALTER TABLE wms_lock_wait_queue COMMENT='锁等待队列表';
ALTER TABLE wms_deadlock_log COMMENT='死锁检测日志表';
