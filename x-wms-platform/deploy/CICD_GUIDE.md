# X WMS CI/CD 实施指南

## 概述

X WMS 采用 GitHub Actions 作为 CI/CD 引擎，支持多环境（dev/staging/prod）自动化构建、测试、安全扫描、镜像构建和部署。

## 流水线阶段

| 阶段 | 触发 | 内容 | 耗时 |
|------|------|------|------|
| 代码质量 | push/PR | Checkstyle + SpotBugs + ESLint + TS检查 | ~3min |
| 安全扫描 | push/PR | OWASP依赖扫描 + npm audit | ~5min |
| 构建测试 | push/PR | Maven构建+测试 + 前端构建 + JaCoCo | ~8min |
| 镜像构建 | push(main/develop) | 5个Docker镜像 + Trivy扫描 | ~10min |
| 部署Dev | push(develop) | docker-compose自动部署 | ~3min |
| 部署Staging | push(main) | k3s蓝绿部署 + 冒烟测试 | ~5min |
| 部署Prod | release tag | k3s金丝雀发布 + 监控 | ~15min |

## 环境配置

### GitHub Secrets 配置

| Secret名 | 说明 |
|---------|------|
| REGISTRY_USERNAME | 镜像仓库用户名 |
| REGISTRY_PASSWORD | 镜像仓库密码 |
| DEV_SERVER_HOST | Dev服务器地址 |
| DEV_SERVER_USER | Dev服务器SSH用户 |
| DEV_SERVER_SSH_KEY | Dev服务器SSH私钥 |
| STAGING_KUBE_CONFIG | Staging kubeconfig |
| PROD_KUBE_CONFIG | Prod kubeconfig |
| STAGING_API_URL | Staging API地址 |
| PROD_API_URL | Prod API地址 |
| WECHAT_WEBHOOK_URL | 企业微信通知Webhook |
| DINGTALK_WEBHOOK_URL | 钉钉通知Webhook |

### 本地开发环境启动

```bash
# 启动基础设施 (Oracle/Redis/Kafka/Nacos/ClickHouse)
cd deploy/docker
docker-compose up -d

# 导入Nacos配置
bash deploy/nacos/import-config.sh

# 后端启动 (4个服务)
cd backend
mvn spring-boot:run -pl wms-common
mvn spring-boot:run -pl wms-base
mvn spring-boot:run -pl wms-core
mvn spring-boot:run -pl wms-analytics
mvn spring-boot:run -pl wms-integration

# 前端启动
cd frontend
pnpm install
pnpm dev
```

## 质量门禁

| 检查项 | 阈值 | 工具 |
|--------|------|------|
| 代码规范 | 0违规 | Checkstyle |
| 静态缺陷 | 0中高危 | SpotBugs |
| 单元测试覆盖率 | ≥80%行 | JaCoCo |
| 依赖漏洞 | 0高危(CVSS≥7) | OWASP DC |
| 镜像漏洞 | 0高危 | Trivy |
| 前端Lint | 0error | ESLint |
| 前端类型 | 0error | tsc |

## 部署策略

### Dev环境
- 触发: push到develop分支
- 方式: docker-compose直接替换
- 数据: 测试数据，可重置

### Staging环境
- 触发: push到main分支，手动审批
- 方式: k3s蓝绿部署
  1. 部署新版本到green
  2. 健康检查通过
  3. Service切换到green
  4. blue保留30分钟可回滚

### Prod环境
- 触发: release tag发布，手动审批
- 方式: k3s金丝雀发布
  1. 部署1个canary实例(10%流量)
  2. 监控5分钟(错误率/延迟)
  3. 扩大到50%
  4. 全量发布，canary缩容到0
  5. 异常自动回滚

## 回滚操作

```bash
# 应用回滚
kubectl rollout undo deployment/wms-core -n xwms-prod

# 指定版本回滚
bash deploy/scripts/rollback.sh prod wms-core v1.0.0

# 配置回滚 (Nacos)
# Nacos控制台 → 配置管理 → 历史版本 → 回滚

# 数据库回滚
# Flyway: mvn flyway:undo -Dflyway.target=previous_version
# 或: 从备份恢复
```

## 数据库迁移

使用 Flyway 进行版本化迁移：

```bash
# 新增迁移脚本
# backend/wms-core/src/main/resources/db/migration/V1.0.1__add_inbound_tables.sql

# 执行迁移
mvn flyway:migrate -pl wms-core

# 查看迁移状态
mvn flyway:info -pl wms-core
```

## 监控与告警

部署后自动监控：
- 健康检查: actuator/health
- 指标采集: actuator/prometheus → Prometheus
- 日志采集: ELK/Loki
- 链路追踪: Micrometer Tracing → Zipkin/Jaeger
- 告警: 企业微信/钉钉通知

## 常用命令

```bash
# 本地运行完整CI检查
cd backend
mvn clean verify

# 本地构建镜像
docker build -f deploy/docker/Dockerfile.backend \
  --build-arg JAR_FILE=wms-core \
  --build-arg PORT=8081 \
  -t xwms/wms-core:local .

# k3s部署
kubectl apply -k deploy/k3s/overlays/dev

# 查看部署状态
kubectl get pods -n xwms-dev
kubectl rollout status deployment/wms-core -n xwms-dev

# 查看日志
kubectl logs -f deployment/wms-core -n xwms-dev
```
