# WMS AI Platform - 通用Dockerfile
# 所有Python服务共用此镜像，通过启动命令区分服务
FROM python:3.11-slim

# 设置工作目录
WORKDIR /app

# 安装系统依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    g++ \
    libgl1-mesa-glx \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender-dev \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# 安装Python依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制项目代码
COPY . .

# 设置Python路径
ENV PYTHONPATH=/app
ENV PYTHONUNBUFFERED=1

# 暴露端口（各服务不同，通过启动命令指定）
EXPOSE 8000 8001 8002 8003 8101 8102 8103 8104 8105 8106 8107 8108

# 默认启动命令（通过K8s command覆盖）
CMD ["python", "-m", "uvicorn", "gateway.main:app", "--host", "0.0.0.0", "--port", "8000"]
