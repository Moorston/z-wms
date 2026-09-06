"""
WMS AI Platform - 公共配置模块
统一管理所有服务的配置，支持环境变量覆盖
"""
from pydantic_settings import BaseSettings
from pydantic import model_validator
from typing import Optional
from functools import lru_cache


class Settings(BaseSettings):
    """全局配置"""
    # 服务配置
    service_name: str = "wms-ai-platform"
    env: str = "development"
    log_level: str = "INFO"

    # Redis
    redis_host: str = "localhost"
    redis_port: int = 6379
    redis_password: Optional[str] = None
    redis_db: int = 0

    # Kafka
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_group_id: str = "wms-ai-group"

    # MySQL（新增，PRD V2.0 主数据库）
    mysql_host: str = "localhost"
    mysql_port: int = 3306
    mysql_user: str = "wms_ai"
    mysql_password: str = "wms_password"
    mysql_database: str = "wms_ai"

    # @deprecated PRD V2.0 改用 MySQL
    oracle_dsn: str = "localhost:1521/ORCLPDB1"
    # @deprecated PRD V2.0 改用 MySQL
    oracle_user: str = "wms_user"
    # @deprecated PRD V2.0 改用 MySQL
    oracle_password: str = "wms_password"

    # ClickHouse
    clickhouse_host: str = "localhost"
    clickhouse_port: int = 8123
    clickhouse_user: str = "default"
    clickhouse_password: str = ""
    clickhouse_database: str = "wms_ai"

    # MinIO
    minio_endpoint: str = "localhost:9000"
    minio_access_key: str = "minioadmin"
    minio_secret_key: str = "minioadmin"
    minio_bucket: str = "wms-ai"
    minio_secure: bool = False

    # Milvus
    milvus_host: str = "localhost"
    milvus_port: int = 19530

    # 模型服务
    model_service_url: str = "http://localhost:8001"

    # data_service_url（用于 data_client 显式引用，不再靠字符串 replace）
    data_service_url: str = "http://localhost:8002"

    # DeepSeek LLM（主力商用 API）
    llm_base_url: str = "https://api.deepseek.com/v1"
    llm_api_key: str = "sk-xxx"
    llm_model: str = "deepseek-v4-pro"
    llm_max_tokens: int = 4096
    llm_temperature: float = 0.1

    # GLM LLM（备选）
    llm_fallback_base_url: str = "https://open.bigmodel.cn/api/paas/v4"
    llm_fallback_api_key: str = "sk-xxx"
    llm_fallback_model: str = "glm-5.2"

    # SiliconFlow API（Embedding / Rerank / OCR 在线服务统一网关）
    siliconflow_base_url: str = "https://api.siliconflow.cn/v1"
    siliconflow_api_key: str = "sk-xxx"

    # Embedding（可独立配置，默认同 SiliconFlow）
    embedding_base_url: str = "https://api.siliconflow.cn/v1"
    embedding_api_key: str = "sk-xxx"
    embedding_model: str = "BAAI/bge-m3"
    embedding_dim: int = 1024

    # Rerank
    rerank_base_url: str = "https://api.siliconflow.cn/v1"
    rerank_api_key: str = "sk-xxx"
    rerank_model: str = "BAAI/bge-reranker-v2-m3"

    # OCR Online（SiliconFlow 托管的 DeepSeek-OCR）
    ocr_online_base_url: str = "https://api.siliconflow.cn/v1"
    ocr_online_api_key: str = "sk-xxx"
    ocr_online_model: str = "deepseek-ai/DeepSeek-OCR-2"

    # 模型路由（JSON 字符串，按 task_type 选模型）
    model_route: str = '{"rag": "deepseek-v4-pro", "nl2sql": "deepseek-v4-pro", "ocr_extract": "deepseek-v4-pro", "default": "deepseek-v4-pro"}'

    # TTS（vLLM/OpenAI兼容 audio.speech 端点）
    tts_model: str = "qwen2.5-omni-tts"
    tts_voice: str = "default"
    tts_format: str = "wav"

    # ASR（SiliconFlow 托管的 XingChenASR-V3.2-Ultra）
    asr_base_url: str = "https://api.siliconflow.cn/v1"
    asr_api_key: str = "sk-xxx"
    asr_model: str = "XingChenAGI/XingChenASR-V3.2-Ultra"

    # 网关
    gateway_port: int = 8000
    jwt_secret: str = "your-jwt-secret-key-change-in-production"
    jwt_algorithm: str = "HS256"
    jwt_expire_minutes: int = 1440

    # OCR（DeepSeek-OCR-2 API，ocr_online_* 系列见上方）
    pass

    # 预测
    forecast_default_periods: int = 30
    forecast_model: str = "prophet"
    forecast_train_data_days: int = 365

    # 五级缓存配置
    cache_l1_max_size: int = 10000           # L1 本地缓存容量
    cache_l1_ttl: int = 3600                 # L1 TTL（秒）
    cache_l2_ttl: int = 86400                # L2 精确缓存 TTL（秒）
    cache_l3_ttl: int = 604800               # L3 语义缓存 TTL（7天）
    cache_l4_ttl: int = 604800               # L4 检索缓存 TTL（7天）
    cache_l5_ttl: int = 2592000              # L5 Embedding TTL（30天）
    cache_semantic_threshold: float = 0.92   # 语义缓存相似度阈值
    cache_nl2sql_threshold: float = 0.95     # NL2SQL 语义缓存阈值（更高）
    cache_lock_timeout: int = 30             # 防击穿互斥锁超时（秒）
    cache_ttl_jitter: float = 0.1           # TTL 随机抖动比例（±10%）
    cache_warmup_cron: str = "0 2 * * *"     # 预热任务 cron（每日凌晨2点）
    cache_metrics_enabled: bool = True       # 缓存监控开关

    # RAG 检索参数（可配置化，替换硬编码）
    rag_retrieval_vector_ef: int = 128              # HNSW ef 搜索参数（从 64 提升到 128）
    rag_retrieval_vector_m: int = 16                # HNSW M 参数
    rag_retrieval_vector_ef_construction: int = 200 # HNSW efConstruction
    rag_retrieval_rrf_k: int = 60                   # RRF 融合参数
    rag_retrieval_top_k: int = 20                   # 混合召回 Top-K
    rag_retrieval_rerank_top_n: int = 5             # Rerank Top-N
    rag_retrieval_keyword_mode: str = "bm25"        # 关键词召回模式：bm25 / mysql_fulltext

    # KG（知识图谱）配置
    rag_kg_enabled: bool = True              # KG 总开关
    rag_kg_extract_llm: bool = True          # LLM 补充抽取开关
    rag_kg_max_hops: int = 2                 # 最大跳数
    rag_kg_min_confidence: float = 0.5       # 最低置信度阈值
    rag_kg_top_k: int = 10                   # 最大返回路径数

    @model_validator(mode="after")
    def _check_production_secrets(self):
        """生产环境禁止使用占位默认密钥，防止凭据外泄"""
        if self.env != "production":
            return self
        violations = []
        checks = [
            ("jwt_secret", "your-jwt-secret-key-change-in-production"),
            ("oracle_password", "wms_password"),
            ("minio_secret_key", "minioadmin"),
            ("llm_api_key", "sk-xxx"),
            ("llm_fallback_api_key", "sk-xxx"),
            ("siliconflow_api_key", "sk-xxx"),
            ("embedding_api_key", "sk-xxx"),
            ("rerank_api_key", "sk-xxx"),
            ("ocr_online_api_key", "sk-xxx"),
            ("asr_api_key", "sk-xxx"),
            ("mysql_password", "wms_password"),
        ]
        for field, placeholder in checks:
            if getattr(self, field) == placeholder:
                violations.append(field)
        if violations:
            raise ValueError(
                f"生产环境禁止使用占位密钥，请通过环境变量设置: {', '.join(violations)}"
            )
        return self

    class Config:
        env_file = ".env"
        case_sensitive = False


@lru_cache()
def get_settings() -> Settings:
    """获取单例配置"""
    return Settings()


settings = get_settings()
