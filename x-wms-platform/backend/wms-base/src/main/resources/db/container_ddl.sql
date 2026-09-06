-- ============================================================
-- X WMS 容器管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 容器档案/容器类型/容器绑定/容器流转记录
-- ============================================================

-- 1. 容器类型表
CREATE TABLE wms_container_type (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    type_code       VARCHAR(64)  NOT NULL, -- 类型编码
    type_name       VARCHAR(128) NOT NULL, -- 类型名称
    category        VARCHAR(32), -- PALLET托盘/BOX箱/BAG袋/TOTE周转箱/CAGE笼车
    length          INT(18,4), -- 长(cm)
    width           INT(18,4), -- 宽(cm)
    height          INT(18,4), -- 高(cm)
    max_weight      INT(18,4), -- 最大承重(kg)
    max_volume      INT(18,4), -- 最大容积(L)
    tare_weight     INT(18,4), -- 皮重(kg)
    reusable        TINYINT(1)     DEFAULT 1, -- 是否可循环使用
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_container_type PRIMARY KEY (id),
    CONSTRAINT uk_wms_container_type_code UNIQUE (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 容器档案表
CREATE TABLE wms_container (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    container_no    VARCHAR(64)  NOT NULL, -- 容器编号(条码)
    type_code       VARCHAR(64)  NOT NULL, -- 容器类型
    warehouse_code  VARCHAR(64), -- 所属仓库
    status          VARCHAR(32) DEFAULT 'EMPTY', -- EMPTY空/OCCUPIED占用/IN_TRANSIT在途/REPAIR维修/DISCARD报废
    current_location VARCHAR(64), -- 当前位置(库位/月台/暂存区)
    current_load    INT(18,4)  DEFAULT 0, -- 当前载重
    current_volume  INT(18,4)  DEFAULT 0, -- 当前容积
    sku_count       INT    DEFAULT 0, -- SKU种类数
    item_count      INT    DEFAULT 0, -- 商品件数
    batch_count     INT    DEFAULT 0, -- 批次数
    last_clean_time TIMESTAMP, -- 最后清洁时间
    last_check_time TIMESTAMP, -- 最后盘点时间
    use_count       INT    DEFAULT 0, -- 使用次数
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_container PRIMARY KEY (id),
    CONSTRAINT uk_wms_container_no UNIQUE (container_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_container_type ON wms_container(type_code);
CREATE INDEX idx_wms_container_status ON wms_container(status);
CREATE INDEX idx_wms_container_location ON wms_container(current_location);

-- 3. 容器绑定表（容器与商品/单据的绑定关系）
CREATE TABLE wms_container_bind (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    bind_no         VARCHAR(64)  NOT NULL, -- 绑定单号
    container_no    VARCHAR(64)  NOT NULL,
    ref_type        VARCHAR(32)  NOT NULL, -- INBOUND入库/OUTBOUND出库/TRANSFER调拨/MOVE移库/STORAGE存储
    ref_no          VARCHAR(64)  NOT NULL, -- 关联单号
    sku_code        VARCHAR(64),
    batch_no        VARCHAR(128),
    quantity        INT(18,4)  NOT NULL, -- 绑定数量
    status          VARCHAR(32) DEFAULT 'BOUND', -- BOUND已绑定/RELEASED已释放
    bind_time       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    release_time    TIMESTAMP,
    operator        VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_container_bind PRIMARY KEY (id),
    CONSTRAINT uk_wms_container_bind_no UNIQUE (bind_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_container_bind_container ON wms_container_bind(container_no);
CREATE INDEX idx_wms_container_bind_ref ON wms_container_bind(ref_type, ref_no);

-- 4. 容器流转记录表
CREATE TABLE wms_container_trace (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    trace_no        VARCHAR(64)  NOT NULL, -- 流水号
    container_no    VARCHAR(64)  NOT NULL,
    action_type     VARCHAR(32)  NOT NULL, -- BIND绑定/RELEASE释放/MOVE移动/CLEAN清洁/REPAIR维修/DISCARD报废
    from_location   VARCHAR(64), -- 来源位置
    to_location     VARCHAR(64), -- 目标位置
    ref_type        VARCHAR(32), -- 关联单据类型
    ref_no          VARCHAR(64), -- 关联单号
    quantity        INT(18,4), -- 数量变化
    operator        VARCHAR(64),
    action_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(512),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wms_container_trace PRIMARY KEY (id),
    CONSTRAINT uk_wms_container_trace_no UNIQUE (trace_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_container_trace_container ON wms_container_trace(container_no);
CREATE INDEX idx_wms_container_trace_time ON wms_container_trace(action_time);


-- 注释
ALTER TABLE wms_container_type COMMENT='容器类型表';
ALTER TABLE wms_container COMMENT='容器档案表';
ALTER TABLE wms_container_bind COMMENT='容器绑定表';
ALTER TABLE wms_container_trace COMMENT='容器流转记录表';
