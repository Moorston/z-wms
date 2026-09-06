# ============================================================
# X WMS Nacos 配置说明
# ============================================================

## 配置架构

```
Nacos Server (localhost:8848)
├── Group: WMS_GROUP
│   ├── wms-common.yaml      # 共享配置（数据库/Redis/Kafka/日志/MyBatis）
│   ├── wms-core.yaml        # 核心服务配置（端口8081）
│   ├── wms-base.yaml        # 基础服务配置（端口8082）
│   ├── wms-analytics.yaml   # 分析服务配置（端口8083）
│   └── wms-integration.yaml # 集成服务配置（端口8084）
```

## 配置加载顺序

1. **本地 application.yaml** - 最小配置（Nacos连接信息）
2. **Nacos wms-common.yaml** - 共享配置（所有服务继承）
3. **Nacos wms-{service}.yaml** - 服务专属配置（覆盖共享配置）

## 快速启动

### 1. 启动Nacos
```bash
cd deploy/docker
docker-compose up -d nacos
```

### 2. 导入配置
```bash
cd deploy/nacos
chmod +x import-config.sh
./import-config.sh
# Windows PowerShell:
# 手动在Nacos控制台导入，或使用curl命令
```

### 3. 启动服务
```bash
cd backend
mvn spring-boot:run -pl wms-core
mvn spring-boot:run -pl wms-base
mvn spring-boot:run -pl wms-analytics
mvn spring-boot:run -pl wms-integration
```

## 配置热更新

在Nacos控制台修改配置后，支持`@RefreshScope`的Bean会自动刷新：
- 业务规则配置（上架/分配/周转规则）
- 限流/熔断参数
- 行业插件开关

## 服务注册

所有服务启动后自动注册到Nacos：
- wms-core:8081
- wms-base:8082
- wms-analytics:8083
- wms-integration:8084

服务间通过OpenFeign调用，使用服务名而非IP。
