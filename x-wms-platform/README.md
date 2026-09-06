# X WMS Platform

> 企业级仓储管理系统 - 3PL多货主多仓库
> 技术架构 V8.1 | Java 25 LTS + Spring Boot 3.5 + K3s 1.32 + Oracle 23ai + Kafka 4.0 KRaft + Redis 8.0

## 项目结构

```
x-wms-platform/
├── docs/                    # 文档
│   ├── architecture/        # 架构文档（含SVG架构图）
│   ├── business/            # 业务文档
│   └── api/                 # API文档
├── backend/                 # Java后端
│   ├── pom.xml              # 父POM
│   ├── wms-common/          # 公共模块（工具/异常/插件框架）
│   ├── wms-core/            # 核心服务 :8081（入库/出库/库存/作业/批次/质检）
│   ├── wms-base/            # 基础服务 :8082（库位/规则/基础数据/月台费收）
│   ├── wms-analytics/       # 分析服务 :8083（KPI/报表/看板，ClickHouse）
│   └── wms-integration/     # 集成服务 :8084（ERP/TMS/快递适配器+API平台）
├── ai-platform/             # AI平台（Python 3.13+FastAPI，独立部署）
├── deploy/                  # 部署配置
│   ├── k3s/                 # K3s部署YAML
│   ├── docker/              # Dockerfile
│   └── scripts/             # 运维脚本
├── sql/                     # 数据库脚本
│   ├── init/                # 初始化DDL
│   └── migration/           # Flyway迁移脚本
└── README.md
```

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 25 LTS |
| 框架 | Spring Boot | 3.5 |
| 微服务 | Spring Cloud | 2025.0 |
| 网关 | APISIX | 3.12 |
| 注册配置 | Nacos | 2.4 |
| 数据库 | Oracle | 23ai |
| OLAP | ClickHouse | 25.8 |
| 缓存 | Redis | 8.0 |
| 消息队列 | Kafka (KRaft) | 4.0 |
| 容器编排 | K3s | 1.32 |
| 监控 | Prometheus + Grafana | 3.0 + 12 |
| 链路追踪 | SkyWalking | 10 |
| AI平台 | Python + FastAPI | 3.13 |

## 核心架构决策

1. **库存扣减**：Oracle原子SQL+乐观锁，强一致，Redis只做检查和预占
2. **4微服务**：core/base/analytics/integration，DDD限界上下文
3. **不分库分表**：分区表+归档+OLAP分离
4. **消息队列**：Kafka(核心事件流) + Redis Stream(轻量任务)
5. **插件化**：SPI+事件+策略+独立类加载器，5大扩展点
6. **AI独立**：Python平台独立部署，Kafka事件+API集成

## 快速开始

### 环境要求
- JDK 25+
- Maven 3.9+
- Docker Desktop（本地开发）
- Oracle 23ai / Redis 8 / Kafka 4 / Nacos 2.4

### 本地启动
```bash
# 1. 启动中间件（Docker Compose）
cd deploy/docker && docker-compose up -d

# 2. 构建
cd backend && mvn clean package -DskipTests

# 3. 启动服务（按顺序）
java -jar wms-base/target/wms-base.jar
java -jar wms-core/target/wms-core.jar
java -jar wms-analytics/target/wms-analytics.jar
java -jar wms-integration/target/wms-integration.jar
```

### 端口分配
| 服务 | 端口 | 说明 |
|------|------|------|
| wms-core | 8081 | 核心作业 |
| wms-base | 8082 | 基础支撑 |
| wms-analytics | 8083 | 分析报表 |
| wms-integration | 8084 | 系统集成 |
| Nacos | 8848 | 注册配置 |
| APISIX | 9080 | 网关 |
| Oracle | 1521 | 数据库 |
| Redis | 6379 | 缓存 |
| Kafka | 9092 | 消息队列 |
| ClickHouse | 8123 | OLAP |

## 模块说明

### wms-common 公共模块
- `core/`：统一响应Result、基础实体BaseEntity
- `exception/`：业务异常BizException、错误码ErrorCode
- `plugin/`：插件框架WmsPlugin、PluginManager
- `utils/`：工具类
- `config/`：通用配置

### wms-core 核心服务
- `inbound/`：入库管理（ASN/收货/质检/上架）
- `outbound/`：出库管理（订单/波次/拣货/复核/发运）
- `inventory/`：库存管理（扣减/预占/调拨/盘点/批次）
- `operation/`：作业执行（任务调度/PDA交互）
- `batch/`：批次全链路追踪
- `qc/`：质检管理

### wms-base 基础服务
- `location/`：库位管理（仓库/库区/库位组/库位）
- `rule/`：规则引擎（13类规则插件）
- `master/`：基础数据（商品/货主/客户/供应商）
- `yard/`：月台费收管理
- `system/`：系统管理（用户/角色/权限/字典）

### wms-analytics 分析服务
- `kpi/`：KPI指标计算
- `report/`：报表生成

### wms-integration 集成服务
- `adapter/`：外部系统适配器（ERP/TMS/WCS/电商/快递）
- `api/`：开放API平台
- `express/`：快递单批量获取（Kafka+虚拟线程）

## 架构图

详见 `docs/architecture/技术架构图V8.1.svg`

## License

MIT
