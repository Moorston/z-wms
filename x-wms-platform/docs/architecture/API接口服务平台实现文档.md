# X WMS API接口服务平台实现文档

> 基于integration-service的统一API平台，对接ERP/TMS/WCS/电商/快递等外部系统

## 一、平台架构

### 1.1 核心流程

```
外部系统 → APISIX网关 → ApiGatewayController → ApiPlatformService
                                              ↓
                    ┌─────────────────────────┼─────────────────────────┐
                    ↓                         ↓                         ↓
              1.注册检查               2.鉴权(4种)             3.限流(2种)
                    ↓                         ↓                         ↓
              4.幂等(Redis)          5.熔断(3态)            6.数据转换
                    ↓                         ↓                         ↓
              7.异步(Kafka)或同步 → 8.适配器调用(带重试) → 9.监控统计
```

### 1.2 9大核心组件

| 组件 | 类 | 职责 |
|------|-----|------|
| API注册中心 | `ApiRegistry` | API定义管理，动态注册/注销 |
| 鉴权服务 | `ApiAuthService` | 4种鉴权：NONE/API_KEY/HMAC/OAUTH2 |
| 限流服务 | `ApiRateLimiter` | 2种算法：令牌桶/滑动窗口 |
| 幂等服务 | `ApiIdempotentService` | Redis SETNX去重，24小时有效 |
| 熔断服务 | `CircuitBreakerService` | 3态：CLOSED/OPEN/HALF_OPEN |
| 数据转换 | `DataTransformEngine` | JSONPath字段映射+类型转换 |
| 重试执行器 | `RetryExecutor` | 指数退避，只重试可恢复异常 |
| 监控服务 | `ApiMonitorService` | 调用量/成功率/耗时/告警 |
| 编排服务 | `ApiPlatformService` | 统一调度9大组件 |

---

## 二、鉴权机制

### 2.1 4种鉴权方式

| 方式 | 适用场景 | 安全性 | 实现 |
|------|---------|--------|------|
| NONE | 内部调用 | 低 | 直接放行 |
| API_KEY | 简单对接 | 中 | X-API-Key头校验 |
| HMAC | 高安全对接 | 高 | HMAC-SHA256签名+时间戳+nonce防重放 |
| OAUTH2 | 第三方平台 | 高 | Bearer Token验证 |

### 2.2 HMAC签名计算

```
签名 = Base64(HMAC-SHA256(appSecret, method + path + timestamp + nonce + body))
防重放：timestamp在5分钟内 + nonce唯一（5分钟缓存）
```

---

## 三、限流机制

### 3.1 2种算法

| 算法 | 实现 | 特点 | 适用 |
|------|------|------|------|
| 令牌桶 | 本地AtomicLong | 高性能，允许突发 | 大部分API |
| 滑动窗口 | Redis ZSet | 分布式精确控制 | 高价值API |

### 3.2 限流配置

```yaml
rateLimit:
  algorithm: TOKEN_BUCKET  # 或 SLIDING_WINDOW
  qps: 100
  burst: 200
```

---

## 四、幂等机制

### 4.1 实现原理

```
幂等键 = appId + apiId + bodyHash
1. SETNX key=PROCESSING (24h TTL)
   - 成功：首次请求，继续处理
   - 失败：重复请求
2. 重复请求时：
   - 状态=SUCCESS：返回缓存结果
   - 状态=PROCESSING：返回409处理中
3. 处理成功：key=SUCCESS:result
4. 处理失败：删除key，允许重试
```

---

## 五、熔断机制

### 5.1 三态模型

```
CLOSED(正常) → 连续失败5次 → OPEN(打开)
OPEN(打开) → 等待30秒 → HALF_OPEN(半开)
HALF_OPEN(半开) → 放行3个探测请求
  ├─ 全部成功 → CLOSED(恢复)
  └─ 任一失败 → OPEN(重新打开)
```

---

## 六、重试机制

### 6.1 指数退避

```
重试间隔：1s → 2s → 4s → 8s（上限30s）
最大重试：3次
可重试异常：timeout/connection/5xx
不可重试：4xx/validation/duplicate
```

---

## 七、适配器框架

### 7.1 适配器清单

| 类型 | 适配器 | 系统 | 核心API |
|------|--------|------|---------|
| ERP | `SapErpAdapter` | SAP S/4HANA | OData API，PO/SO/库存同步 |
| ERP | (预留) | Oracle EBS | REST API |
| ERP | (预留) | 用友/金蝶 | 开放平台API |
| TMS | `TmsAdapter` | 运输管理 | 运输计划/轨迹/回单 |
| WCS | `WcsAdapter` | 仓储控制 | 入库/出库/移库任务 |
| 电商 | `TaobaoAdapter` | 淘宝/天猫 | TOP API，订单/发货/库存 |
| 电商 | (预留) | 京东/拼多多 | 开放平台API |
| 快递 | `SfExpressAdapter` | 顺丰 | 取号/轨迹/运费 |
| 快递 | (预留) | 京东/菜鸟/中通 | 开放平台API |

### 7.2 适配器基类

`AbstractIntegrationAdapter` 提供：
- 通用HTTP POST/GET调用
- 鉴权头构建（子类实现）
- 健康检查
- 统一日志

### 7.3 扩展新适配器

1. 继承 `AbstractIntegrationAdapter`
2. 实现 `getBaseUrl()` 和 `buildAuthHeaders()`
3. 实现 `pushOrder()` 和 `pullOrder()`
4. 添加 `@Component` 注解，Spring自动注册
5. 在ApiRegistry中注册对应API定义

---

## 八、统一API入口

### 8.1 通配符路由

```
POST /api/external/**  → 通用POST入口
GET  /api/external/**  → 通用GET入口
PUT  /api/external/**  → 通用PUT入口
```

### 8.2 管理接口

```
GET  /api/external/admin/apis       → 查看所有注册API
GET  /api/external/admin/monitor    → 查看API监控统计
POST /api/external/admin/apis/register → 动态注册API
```

---

## 九、异步API

对于耗时操作（如批量导入、报表生成），支持异步模式：
1. 请求直接返回 `{requestId, status: ACCEPTED}`
2. 实际处理走Kafka `wms-api-async` topic
3. 处理完成后通过回调或查询接口获取结果

---

## 十、监控与告警

### 10.1 统计指标

- 总调用量、成功量、失败量
- 成功率（低于90%告警）
- 平均耗时、最大耗时
- 最近错误信息

### 10.2 告警规则

- 成功率 < 90%（连续100次调用）
- 平均耗时 > 5s
- 熔断器打开
- 限流触发频率 > 10次/分钟

---

## 十一、文件清单

| 文件 | 说明 |
|------|------|
| `ApiDefinition.java` | API定义模型 |
| `ApiRequest.java` | API请求模型 |
| `ApiResponse.java` | API响应模型 |
| `ApiRegistry.java` | API注册中心 |
| `ApiAuthService.java` | 鉴权服务（4种方式） |
| `ApiRateLimiter.java` | 限流服务（2种算法） |
| `ApiIdempotentService.java` | 幂等服务 |
| `DataTransformEngine.java` | 数据转换引擎 |
| `RetryExecutor.java` | 重试执行器 |
| `CircuitBreakerService.java` | 熔断服务 |
| `ApiMonitorService.java` | 监控服务 |
| `ApiPlatformService.java` | 编排服务（核心大脑） |
| `ApiGatewayController.java` | 统一入口控制器 |
| `AbstractIntegrationAdapter.java` | 适配器基类 |
| `SapErpAdapter.java` | SAP ERP适配器 |
| `TmsAdapter.java` | TMS适配器 |
| `WcsAdapter.java` | WCS适配器 |
| `TaobaoAdapter.java` | 淘宝适配器 |
| `SfExpressAdapter.java` | 顺丰适配器 |
