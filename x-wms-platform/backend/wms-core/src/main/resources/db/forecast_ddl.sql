-- ============================================================
-- X WMS 库存分析预测管理模块 DDL
-- 数据库: MySQL 8.x
-- 包含: 库存分析/需求预测/库存优化/预测模型
-- ============================================================

-- 1. 库存分析表
CREATE TABLE wms_inventory_analysis (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    analysis_id     VARCHAR(64)  NOT NULL, -- 分析ID
    analysis_name   VARCHAR(128), -- 分析名称
    analysis_type   VARCHAR(32)  NOT NULL, -- 分析类型: TURNOVER周转率/ABC分类/SAFETY_STOCK安全库存/AGING库龄/VALUE价值分析/SPACE空间分析
    warehouse_code  VARCHAR(64), -- 仓库
    owner_code      VARCHAR(64), -- 货主
    period_start    DATE, -- 期间开始
    period_end      DATE, -- 期间结束
    sku_code        VARCHAR(64), -- SKU
    category_code   VARCHAR(64), -- 品类
    total_quantity  INT(19,4), -- 总数量
    total_amount    INT(19,4), -- 总金额
    avg_quantity    INT(19,4), -- 平均数量
    avg_amount      INT(19,4), -- 平均金额
    turnover_rate   INT(10,4), -- 周转率
    turnover_days   INT(10,2), -- 周转天数
    abc_class       VARCHAR(8), -- ABC分类: A/B/C
    safety_stock    INT(19,4), -- 安全库存
    reorder_point   INT(19,4), -- 再订货点
    max_stock       INT(19,4), -- 最大库存
    min_stock       INT(19,4), -- 最小库存
    aging_days      INT, -- 库龄天数
    aging_bucket    VARCHAR(32), -- 库龄区间: 0-30/31-60/61-90/91-180/180+
    space_utilization INT(10,2), -- 空间利用率
    analysis_result TEXT, -- 分析结果(JSON)
    suggestions     TEXT, -- 建议(JSON)
    status          VARCHAR(32) DEFAULT 'COMPLETED', -- PENDING处理中/COMPLETED已完成/FAILED失败
    error_message   TEXT, -- 错误信息
    operator        VARCHAR(64), -- 操作人
    analysis_time   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP, -- 分析时间
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_inventory_analysis PRIMARY KEY (id),
    CONSTRAINT uk_wms_inventory_analysis_id UNIQUE (analysis_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inventory_analysis_wh ON wms_inventory_analysis(warehouse_code);
CREATE INDEX idx_wms_inventory_analysis_type ON wms_inventory_analysis(analysis_type);
CREATE INDEX idx_wms_inventory_analysis_sku ON wms_inventory_analysis(sku_code);
CREATE INDEX idx_wms_inventory_analysis_period ON wms_inventory_analysis(period_start, period_end);

-- 2. 需求预测表
CREATE TABLE wms_demand_forecast (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    forecast_id     VARCHAR(64)  NOT NULL, -- 预测ID
    forecast_name   VARCHAR(128), -- 预测名称
    model_code      VARCHAR(64), -- 预测模型
    warehouse_code  VARCHAR(64), -- 仓库
    owner_code      VARCHAR(64), -- 货主
    sku_code        VARCHAR(64)  NOT NULL, -- SKU
    category_code   VARCHAR(64), -- 品类
    forecast_period VARCHAR(32)  NOT NULL, -- 预测周期: DAILY日/WEEKLY周/MONTHLY月/QUARTERLY季
    forecast_horizon SMALLINT    NOT NULL, -- 预测期数
    history_start   DATE, -- 历史数据开始
    history_end     DATE, -- 历史数据结束
    forecast_start  DATE NOT NULL, -- 预测开始
    forecast_end    DATE NOT NULL, -- 预测结束
    actual_quantity INT(19,4), -- 实际数量(历史)
    forecast_quantity INT(19,4), -- 预测数量
    lower_bound     INT(19,4), -- 下限
    upper_bound     INT(19,4), -- 上限
    confidence      INT(5,2), -- 置信度(%)
    mae             INT(19,4), -- 平均绝对误差
    rmse            INT(19,4), -- 均方根误差
    mape            INT(10,4), -- 平均绝对百分比误差
    forecast_data   TEXT, -- 预测数据(JSON数组)
    actual_data     TEXT, -- 实际数据(JSON数组)
    status          VARCHAR(32) DEFAULT 'COMPLETED', -- PENDING处理中/COMPLETED已完成/FAILED失败
    error_message   TEXT, -- 错误信息
    operator        VARCHAR(64), -- 操作人
    forecast_time   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP, -- 预测时间
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_demand_forecast PRIMARY KEY (id),
    CONSTRAINT uk_wms_demand_forecast_id UNIQUE (forecast_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_demand_forecast_wh ON wms_demand_forecast(warehouse_code);
CREATE INDEX idx_wms_demand_forecast_sku ON wms_demand_forecast(sku_code);
CREATE INDEX idx_wms_demand_forecast_model ON wms_demand_forecast(model_code);
CREATE INDEX idx_wms_demand_forecast_period ON wms_demand_forecast(forecast_start, forecast_end);

-- 3. 库存优化表
CREATE TABLE wms_inventory_optimization (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    optimization_id VARCHAR(64)  NOT NULL, -- 优化ID
    optimization_name VARCHAR(128), -- 优化名称
    optimization_type VARCHAR(32) NOT NULL, -- 优化类型: SAFETY_STOCK安全库存优化/REORDER_POINT再订货点优化/ABC分类优化/SPACE空间优化/COST成本优化
    warehouse_code  VARCHAR(64), -- 仓库
    owner_code      VARCHAR(64), -- 货主
    sku_code        VARCHAR(64), -- SKU
    category_code   VARCHAR(64), -- 品类
    current_value   INT(19,4), -- 当前值
    optimized_value INT(19,4), -- 优化值
    improvement_rate INT(10,2), -- 改善率(%)
    cost_saving     INT(19,4), -- 成本节约
    space_saving    INT(19,4), -- 空间节约
    optimization_result TEXT, -- 优化结果(JSON)
    suggestions     TEXT, -- 建议(JSON)
    status          VARCHAR(32) DEFAULT 'COMPLETED', -- PENDING处理中/COMPLETED已完成/FAILED失败/IMPLEMENTED已实施
    error_message   TEXT, -- 错误信息
    operator        VARCHAR(64), -- 操作人
    implementer     VARCHAR(64), -- 实施人
    implement_time  TIMESTAMP, -- 实施时间
    optimization_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP, -- 优化时间
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_inventory_optimization PRIMARY KEY (id),
    CONSTRAINT uk_wms_inventory_optimization_id UNIQUE (optimization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_inventory_optimization_wh ON wms_inventory_optimization(warehouse_code);
CREATE INDEX idx_wms_inventory_optimization_type ON wms_inventory_optimization(optimization_type);
CREATE INDEX idx_wms_inventory_optimization_sku ON wms_inventory_optimization(sku_code);
CREATE INDEX idx_wms_inventory_optimization_status ON wms_inventory_optimization(status);

-- 4. 预测模型表
CREATE TABLE wms_forecast_model (
    id              BIGINT    NOT NULL AUTO_INCREMENT,
    model_code      VARCHAR(64)  NOT NULL, -- 模型编码
    model_name      VARCHAR(128) NOT NULL, -- 模型名称
    model_type      VARCHAR(32)  NOT NULL, -- 模型类型: MOVING_AVG移动平均/EXPONENTIAL_SMOOTHING指数平滑/ARIMA时间序列/LSTM神经网络/XGBOOST机器学习/PROPHET预测
    algorithm       VARCHAR(64), -- 算法
    version         VARCHAR(32) DEFAULT '1.0', -- 版本
    description     VARCHAR(512), -- 描述
    parameters      TEXT, -- 模型参数(JSON)
    hyperparameters TEXT, -- 超参数(JSON)
    feature_list    TEXT, -- 特征列表(JSON)
    training_data_start DATE, -- 训练数据开始
    training_data_end   DATE, -- 训练数据结束
    training_samples INT, -- 训练样本数
    accuracy        INT(10,2), -- 准确率(%)
    mae             INT(19,4), -- 平均绝对误差
    rmse            INT(19,4), -- 均方根误差
    mape            INT(10,4), -- 平均绝对百分比误差
    training_time   TIMESTAMP, -- 训练时间
    training_duration BIGINT, -- 训练耗时(毫秒)
    model_path      VARCHAR(512), -- 模型文件路径
    model_size      BIGINT, -- 模型大小(字节)
    framework       VARCHAR(64), -- 框架: TensorFlow/PyTorch/Scikit-learn/Statsmodels
    status          VARCHAR(32) DEFAULT 'ACTIVE', -- DRAFT草稿/TRAINING训练中/ACTIVE活跃/INACTIVE停用/RETIRED退役
    is_default      VARCHAR(8)  DEFAULT 'N', -- 是否默认: Y/N
    created_by      VARCHAR(64),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP,
    CONSTRAINT pk_wms_forecast_model PRIMARY KEY (id),
    CONSTRAINT uk_wms_forecast_model_code UNIQUE (model_code, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wms_forecast_model_type ON wms_forecast_model(model_type);
CREATE INDEX idx_wms_forecast_model_status ON wms_forecast_model(status);


-- 注释
ALTER TABLE wms_inventory_analysis COMMENT='库存分析表';
ALTER TABLE wms_demand_forecast COMMENT='需求预测表';
ALTER TABLE wms_inventory_optimization COMMENT='库存优化表';
ALTER TABLE wms_forecast_model COMMENT='预测模型表';
