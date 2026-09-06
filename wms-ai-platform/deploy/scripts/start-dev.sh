#!/bin/bash
# WMS AI Platform - 开发环境启动脚本
# 用法：./start-dev.sh [service]
# service可选：all/gateway/model/data/scheduler/ocr/forecast/report/rag/aiops/voice/drone/video

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  WMS AI Platform - 开发环境启动${NC}"
echo -e "${GREEN}========================================${NC}"

# 检查Python环境
if ! command -v python &> /dev/null; then
    echo -e "${RED}错误：未找到Python，请先安装Python 3.11+${NC}"
    exit 1
fi

# 检查依赖
if [ ! -d ".venv" ]; then
    echo -e "${YELLOW}创建虚拟环境...${NC}"
    python -m venv .venv
fi

source .venv/bin/activate 2>/dev/null || source .venv/Scripts/activate 2>/dev/null || true

echo -e "${YELLOW}安装依赖...${NC}"
pip install -q -r requirements.txt

# 环境变量
export ENV=development
export MOCK_MODE=true
export PYTHONPATH=$PROJECT_DIR

# 启动函数
start_service() {
    local name=$1
    local module=$2
    local port=$3
    echo -e "${GREEN}启动 $name (端口 $port)...${NC}"
    python -m uvicorn "$module:app" --host 0.0.0.0 --port "$port" --reload &
    echo $! > "/tmp/wms-ai-$name.pid"
    sleep 2
}

# 启动中间件（Docker Compose）
echo -e "${YELLOW}启动中间件(Redis/Kafka/MinIO/ClickHouse)...${NC}"
docker-compose up -d redis kafka minio clickhouse 2>/dev/null || echo -e "${YELLOW}Docker Compose未运行，跳过中间件启动${NC}"

sleep 3

# 根据参数启动服务
case "${1:-all}" in
    all)
        start_service "model-service" "model_service.main" 8001
        start_service "data-service" "data_service.main" 8002
        start_service "scheduler" "scheduler.main" 8003
        start_service "module-ocr" "modules.ocr.main" 8101
        start_service "module-forecast" "modules.forecast.main" 8102
        start_service "module-report" "modules.report.main" 8103
        start_service "module-rag" "modules.rag.main" 8104
        start_service "module-aiops" "modules.aiops.main" 8105
        start_service "module-voice" "modules.voice.main" 8106
        start_service "module-drone" "modules.drone.main" 8107
        start_service "module-video" "modules.video.main" 8108
        start_service "gateway" "gateway.main" 8000
        ;;
    gateway) start_service "gateway" "gateway.main" 8000 ;;
    model) start_service "model-service" "model_service.main" 8001 ;;
    data) start_service "data-service" "data_service.main" 8002 ;;
    scheduler) start_service "scheduler" "scheduler.main" 8003 ;;
    ocr) start_service "module-ocr" "modules.ocr.main" 8101 ;;
    forecast) start_service "module-forecast" "modules.forecast.main" 8102 ;;
    report) start_service "module-report" "modules.report.main" 8103 ;;
    rag) start_service "module-rag" "modules.rag.main" 8104 ;;
    aiops) start_service "module-aiops" "modules.aiops.main" 8105 ;;
    voice) start_service "module-voice" "modules.voice.main" 8106 ;;
    drone) start_service "module-drone" "modules.drone.main" 8107 ;;
    video) start_service "module-video" "modules.video.main" 8108 ;;
    *)
        echo -e "${RED}未知服务: $1${NC}"
        echo "可用服务: all/gateway/model/data/scheduler/ocr/forecast/report/rag/aiops/voice/drone/video"
        exit 1
        ;;
esac

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  启动完成！${NC}"
echo -e "${GREEN}  网关地址: http://localhost:8000${NC}"
echo -e "${GREEN}  API文档: http://localhost:8000/docs${NC}"
echo -e "${GREEN}  停止服务: ./stop-dev.sh${NC}"
echo -e "${GREEN}========================================${NC}"

# 保持前台运行
wait
