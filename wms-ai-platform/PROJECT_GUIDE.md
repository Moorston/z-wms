# WMS AI Platform - 项目代码骨架说明

## 项目概览

基于 FastAPI 的 WMS 智能化平台，整合8个AI功能模块，采用"统一基础设施+插件化AI模块"架构。

## 代码文件清单（共26个文件）

### 基础配置（4个）
| 文件 | 说明 |
|------|------|
| `README.md` | 项目说明、快速开始、端口分配 |
| `requirements.txt` | Python依赖清单 |
| `.env.example` | 环境变量模板 |
| `Dockerfile` | 通用Docker镜像（所有服务共用） |

### 公共模块 common/（5个）
| 文件 | 说明 |
|------|------|
| `common/__init__.py` | 公共模块导出 |
| `common/config.py` | 全局配置（pydantic-settings，环境变量覆盖） |
| `common/ai_client.py` | AI模型统一调用客户端（LLM/OCR/CV/语音/预测/异常检测） |
| `common/data_client.py` | 数据服务客户端（Redis/Kafka/MinIO/Oracle/ClickHouse） |
| `common/utils.py` | 工具类（ID生成/耗时统计/统一返回/业务异常） |

### 基础设施服务（4个）
| 文件 | 端口 | 说明 |
|------|------|------|
| `gateway/main.py` | 8000 | API网关（鉴权/限流/路由/日志） |
| `model_service/main.py` | 8001 | 统一模型服务（LLM/OCR/CV/语音/预测/异常检测，支持Mock模式） |
| `data_service/main.py` | 8002 | 数据服务（Oracle只读/ClickHouse OLAP） |
| `scheduler/main.py` | 8003 | 调度服务（定时任务APScheduler/Kafka事件消费） |

### 8个AI业务模块 modules/（8个）
| 文件 | 端口 | 说明 | 完整度 |
|------|------|------|--------|
| `modules/ocr/main.py` | 8101 | 单据OCR（5种单据类型/模板+LLM抽取/差异校验/人工确认） | 完整 |
| `modules/forecast/main.py` | 8102 | 需求预测（Prophet训练/预测/补货建议/批量训练） | 完整 |
| `modules/report/main.py` | 8103 | 自动化报表（Text-to-SQL/LLM分析/图表生成/推送） | 框架 |
| `modules/rag/main.py` | 8104 | 智能问答RAG（向量检索/重排序/LLM生成/知识库管理） | 框架 |
| `modules/aiops/main.py` | 8105 | AIOps（告警聚合/根因分析/自动修复/异常检测） | 框架 |
| `modules/voice/main.py` | 8106 | 语音拣货（WebSocket/ASR/TTS/指令解析/任务管理） | 框架 |
| `modules/drone/main.py` | 8107 | 无人机盘点（航线规划/图像识别/库存比对/差异处理） | 框架 |
| `modules/video/main.py` | 8108 | 打包视频分析（RTSP流/目标检测/事件检测/效率统计） | 框架 |

### 部署配置 deploy/（5个）
| 文件 | 说明 |
|------|------|
| `docker-compose.yml` | 开发测试环境（中间件+服务） |
| `deploy/k3s/deployment.yaml` | K3s生产部署（Namespace/ConfigMap/12个Deployment+Service/Ingress/HPA/PVC） |
| `deploy/scripts/init_clickhouse.sql` | ClickHouse初始化（11张核心表） |
| `deploy/scripts/start-dev.sh` | 开发环境一键启动脚本 |
| `deploy/scripts/init-kafka-topics.sh` | Kafka Topic初始化（12个Topic） |

## 快速开始

### 方式一：Docker Compose（推荐）
```bash
# 1. 配置环境
cp .env.example .env

# 2. 启动中间件
docker-compose up -d redis kafka minio clickhouse

# 3. 初始化Kafka Topics
bash deploy/scripts/init-kafka-topics.sh

# 4. 初始化ClickHouse
clickhouse-client --host localhost < deploy/scripts/init_clickhouse.sql

# 5. 启动所有服务（Mock模式，不加载真实AI模型）
MOCK_MODE=true docker-compose up -d
```

### 方式二：本地开发
```bash
# 1. 创建虚拟环境
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate

# 2. 安装依赖
pip install -r requirements.txt

# 3. 启动模型服务（Mock模式）
MOCK_MODE=true python -m uvicorn model_service.main:app --port 8001

# 4. 启动OCR模块（新终端）
python -m uvicorn modules.ocr.main:app --port 8101

# 5. 启动预测模块（新终端）
python -m uvicorn modules.forecast.main:app --port 8102

# 6. 启动网关（新终端）
python -m uvicorn gateway.main:app --port 8000
```

### 方式三：一键脚本
```bash
bash deploy/scripts/start-dev.sh all
```

## API测试示例

### 1. 单据OCR识别
```bash
curl -X POST http://localhost:8000/api/ocr/ocr/upload \
  -F "image=@test_delivery_note.jpg" \
  -F "doc_type=delivery" \
  -F "po_no=PO20240101001"
```

### 2. 需求预测
```bash
curl -X POST http://localhost:8000/api/forecast/forecast/predict \
  -H "Content-Type: application/json" \
  -d '{"sku":"SKU001","warehouse":"WH001","periods":30}'
```

### 3. 补货建议
```bash
curl -X POST http://localhost:8000/api/forecast/forecast/replenishment \
  -H "Content-Type: application/json" \
  -d '{"sku":"SKU001","warehouse":"WH001","current_stock":50,"lead_time_days":3}'
```

### 4. 智能问答
```bash
curl -X POST http://localhost:8000/api/rag/rag/chat \
  -H "Content-Type: application/json" \
  -d '{"question":"波次拣货怎么操作？"}'
```

### 5. 自然语言查数
```bash
curl -X POST http://localhost:8000/api/report/report/query \
  -H "Content-Type: application/json" \
  -d '{"question":"昨天出库量最多的前5个商品是什么？"}'
```

## Mock模式说明

开发环境默认 `MOCK_MODE=true`，模型服务返回模拟数据，不加载真实AI模型：
- LLM：返回预设回复
- OCR：返回模拟识别文字
- 目标检测：返回模拟检测结果
- 预测：简单趋势外推
- 异常检测：3σ简单检测
- TTS：返回静音WAV

生产环境设置 `MOCK_MODE=false`，加载真实模型（需要GPU服务器）。

## 生产部署（K3s）

```bash
# 1. 构建镜像
docker build -t wms-ai-platform:latest .

# 2. 推送到镜像仓库
docker tag wms-ai-platform:latest registry.local/wms-ai-platform:latest
docker push registry.local/wms-ai-platform:latest

# 3. 部署到K3s
kubectl apply -f deploy/k3s/deployment.yaml

# 4. 查看部署状态
kubectl get pods -n wms-ai
kubectl get svc -n wms-ai

# 5. 查看日志
kubectl logs -f deployment/module-ocr -n wms-ai
```

## 架构要点

1. **统一模型服务**：所有AI能力通过 `model_service` 统一调用，业务模块不直接依赖模型框架
2. **插件化模块**：8个业务模块独立部署，可独立启停，通过Kafka事件解耦
3. **中心-边缘协同**：重模型(GPU)中心部署，轻模型(PDA/无人机/摄像头)边缘部署
4. **与WMS解耦**：通过API/Kafka与WMS核心交互，AI平台故障不影响仓库作业
5. **Mock模式**：开发环境无需GPU即可运行全部模块，降低开发门槛

## 后续扩展

- [ ] 管理后台前端（Vue3 + ECharts）
- [ ] PDA端应用（Flutter）
- [ ] MLflow模型管理集成
- [ ] Prometheus监控指标埋点
- [ ] 各模块单元测试
- [ ] CI/CD流水线配置
