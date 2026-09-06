# WMS AI 统一平台

> 基于 FastAPI 的 WMS 智能化平台，整合8个AI功能模块：单据OCR、需求预测、自动化报表、智能问答(RAG)、AIOps、语音拣货、无人机盘点、打包视频分析

## 技术栈

- **后端**: Python 3.11 + FastAPI
- **AI模型**: DeepSeek-V4-Pro(LLM) / DeepSeek-OCR-2(OCR) / YOLOv10 / XingChenASR-V3.2-Ultra(ASR) / CosyVoice / Prophet
- **数据**: Oracle / ClickHouse / Redis / Kafka / Milvus / MinIO
- **部署**: Docker Compose(开发) / K3s(生产)

## 快速开始

```bash
# 1. 安装依赖
pip install -r requirements.txt

# 2. 配置环境变量
cp .env.example .env

# 3. 启动中间件（开发环境）
docker-compose up -d redis kafka minio clickhouse

# 4. 启动模型服务
cd model_service && uvicorn main:app --host 0.0.0.0 --port 8001

# 5. 启动OCR模块
cd modules/ocr && uvicorn main:app --host 0.0.0.0 --port 8101

# 6. 启动预测模块
cd modules/forecast && uvicorn main:app --host 0.0.0.0 --port 8102

# 7. 启动网关
cd gateway && uvicorn main:app --host 0.0.0.0 --port 8000
```

## 模块端口分配

| 服务 | 端口 | 说明 |
|------|------|------|
| gateway | 8000 | API网关 |
| model_service | 8001 | 统一模型服务 |
| data_service | 8002 | 数据服务 |
| scheduler | 8003 | 调度服务 |
| module-ocr | 8101 | 单据OCR |
| module-forecast | 8102 | 需求预测 |
| module-report | 8103 | 自动化报表 |
| module-rag | 8104 | 智能问答 |
| module-aiops | 8105 | AIOps |
| module-voice | 8106 | 语音拣货 |
| module-drone | 8107 | 无人机盘点 |
| module-video | 8108 | 打包视频分析 |

## 项目结构

```
wms-ai-platform/
├── common/              # 公共模块（配置/AI客户端/数据客户端/工具）
├── gateway/             # API网关
├── model_service/       # 统一模型服务（LLM/CV/语音/预测/异常检测）
├── data_service/        # 数据服务（Oracle/ClickHouse/Kafka/MinIO）
├── scheduler/           # 调度服务（定时/事件/流式）
├── modules/             # 8个AI业务模块
│   ├── ocr/             # 单据OCR
│   ├── forecast/        # 需求预测
│   ├── report/          # 自动化报表
│   ├── rag/             # 智能问答RAG
│   ├── aiops/           # AIOps
│   ├── voice/           # 语音拣货
│   ├── drone/           # 无人机盘点
│   └── video/           # 打包视频分析
└── deploy/              # 部署配置
    ├── docker-compose.yml
    ├── k3s/             # K3s部署YAML
    └── scripts/         # 初始化脚本
```
