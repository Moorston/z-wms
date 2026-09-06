"""
WMS AI Platform - 数据服务客户端
统一访问WMS业务数据（MySQL/Oracle/ClickHouse/Kafka/MinIO），业务模块不直连数据库

PRD V2.0 改动：
  - 新增 MySQLClient（pymysql，懒加载，连接失败降级 "mock"）
  - 全局单例改为懒加载（首次调用时连接），避免导入时网络阻塞
  - query_wms/query_olap 改用显式 data_service_url（不再字符串 replace）
"""
import json
from typing import List, Dict, Any, Optional
from loguru import logger

from common.config import settings


class RedisClient:
    """Redis客户端封装"""

    def __init__(self):
        import redis
        self._client = redis.Redis(
            host=settings.redis_host,
            port=settings.redis_port,
            password=settings.redis_password,
            db=settings.redis_db,
            decode_responses=True,
        )

    def get(self, key: str) -> Optional[str]:
        return self._client.get(key)

    def set(self, key: str, value: str, expire: int = None):
        self._client.set(key, value, ex=expire)

    def set_json(self, key: str, value: Any, expire: int = None):
        self._client.set(key, json.dumps(value, ensure_ascii=False), ex=expire)

    def get_json(self, key: str) -> Optional[Any]:
        val = self._client.get(key)
        return json.loads(val) if val else None

    def delete(self, key: str):
        self._client.delete(key)

    def exists(self, key: str) -> bool:
        return self._client.exists(key) > 0

    def incr(self, key: str, amount: int = 1) -> int:
        return self._client.incr(key, amount)

    def lock(self, key: str, timeout: int = 30) -> bool:
        """分布式锁，返回是否获取成功"""
        return self._client.set(key, "1", ex=timeout, nx=True) is not None

    def unlock(self, key: str):
        self._client.delete(key)

    @property
    def client(self):
        return self._client


class MySQLClient:
    """
    MySQL 客户端封装（PRD V2.0 主数据库）
    懒加载连接，连接失败降级 "mock"（不影响服务启动）
    """

    def __init__(self):
        self._conn = None
        self._connected = False

    def _connect(self):
        """懒加载建立 MySQL 连接"""
        if self._connected:
            return self._conn
        try:
            import pymysql
            self._conn = pymysql.connect(
                host=settings.mysql_host,
                port=settings.mysql_port,
                user=settings.mysql_user,
                password=settings.mysql_password,
                database=settings.mysql_database,
                charset="utf8mb4",
                cursorclass=pymysql.cursors.DictCursor,
                autocommit=True,
            )
            self._connected = True
            logger.info(f"MySQL连接成功: {settings.mysql_host}:{settings.mysql_port}/{settings.mysql_database}")
        except Exception as e:
            logger.warning(f"MySQL连接失败，降级 mock 模式: {e}")
            self._conn = "mock"
            self._connected = True
        return self._conn

    def get_conn(self):
        """获取连接（懒加载）"""
        return self._connect()

    def query(self, sql: str, params: tuple = None) -> List[Dict]:
        """执行 SELECT 查询，返回字典列表"""
        conn = self._connect()
        if conn == "mock":
            return []
        try:
            with conn.cursor() as cursor:
                cursor.execute(sql, params)
                return cursor.fetchall()
        except Exception as e:
            logger.error(f"MySQL查询失败: {e}")
            # 连接可能已断开，重置以便重连
            try:
                conn.close()
            except Exception:
                pass
            self._conn = None
            self._connected = False
            return []

    def execute(self, sql: str, params: tuple = None) -> int:
        """执行 INSERT/UPDATE/DELETE，返回受影响行数"""
        conn = self._connect()
        if conn == "mock":
            return 0
        try:
            with conn.cursor() as cursor:
                affected = cursor.execute(sql, params)
                conn.commit()
                return affected
        except Exception as e:
            logger.error(f"MySQL执行失败: {e}")
            try:
                conn.close()
            except Exception:
                pass
            self._conn = None
            self._connected = False
            return 0

    def insert(self, sql: str, params: tuple = None) -> Optional[int]:
        """执行 INSERT，返回自增主键 ID"""
        conn = self._connect()
        if conn == "mock":
            return None
        try:
            with conn.cursor() as cursor:
                cursor.execute(sql, params)
                conn.commit()
                return cursor.lastrowid
        except Exception as e:
            logger.error(f"MySQL插入失败: {e}")
            try:
                conn.close()
            except Exception:
                pass
            self._conn = None
            self._connected = False
            return None

    @property
    def is_mock(self) -> bool:
        """当前是否处于 mock 模式"""
        self._connect()
        return self._conn == "mock"


class KafkaClient:
    """Kafka客户端封装"""

    def __init__(self):
        self._bootstrap = settings.kafka_bootstrap_servers
        self._producer = None

    def get_producer(self):
        from kafka import KafkaProducer
        if self._producer is None:
            self._producer = KafkaProducer(
                bootstrap_servers=self._bootstrap,
                value_serializer=lambda v: json.dumps(v, ensure_ascii=False).encode("utf-8"),
                key_serializer=lambda k: k.encode("utf-8") if k else None,
                retries=3,
                acks="all",
            )
        return self._producer

    def send(self, topic: str, value: Any, key: str = None):
        """发送消息（非阻塞，不调用 future.get 避免阻塞事件循环）"""
        try:
            producer = self.get_producer()
            producer.send(topic, value=value, key=key)
            logger.debug(f"Kafka发送成功: topic={topic}, key={key}")
        except Exception as e:
            logger.error(f"Kafka发送失败: {e}")
            raise

    def get_consumer(self, topic: str, group_id: str = None,
                     auto_offset_reset: str = "latest"):
        """创建消费者"""
        from kafka import KafkaConsumer
        return KafkaConsumer(
            topic,
            bootstrap_servers=self._bootstrap,
            group_id=group_id or settings.kafka_group_id,
            auto_offset_reset=auto_offset_reset,
            enable_auto_commit=False,
            value_deserializer=lambda m: json.loads(m.decode("utf-8")),
            max_poll_records=100,
        )


class MinioClient:
    """MinIO对象存储封装（懒加载，首次 upload 时才连接+建桶）"""

    def __init__(self):
        self._client = None
        self._bucket_ready = False

    def _get_client(self):
        """懒加载 MinIO 客户端"""
        if self._client is not None:
            return self._client
        from minio import Minio
        self._client = Minio(
            settings.minio_endpoint,
            access_key=settings.minio_access_key,
            secret_key=settings.minio_secret_key,
            secure=settings.minio_secure,
        )
        return self._client

    def _ensure_bucket(self):
        """首次上传时建桶（懒加载，避免导入时网络阻塞）"""
        if self._bucket_ready:
            return
        try:
            client = self._get_client()
            if not client.bucket_exists(settings.minio_bucket):
                client.make_bucket(settings.minio_bucket)
            self._bucket_ready = True
        except Exception as e:
            logger.warning(f"MinIO bucket检查/创建失败（降级跳过）: {e}")

    def upload_bytes(self, object_name: str, data: bytes, content_type: str = "application/octet-stream") -> str:
        """上传字节数据"""
        import io
        self._ensure_bucket()
        client = self._get_client()
        client.put_object(
            settings.minio_bucket, object_name, io.BytesIO(data), len(data), content_type=content_type
        )
        return object_name

    def upload_file(self, file_path: str, object_name: str) -> str:
        """上传文件"""
        self._ensure_bucket()
        client = self._get_client()
        client.fput_object(settings.minio_bucket, object_name, file_path)
        return object_name

    def download_bytes(self, object_name: str) -> bytes:
        """下载字节数据"""
        client = self._get_client()
        response = client.get_object(settings.minio_bucket, object_name)
        return response.read()

    def get_presigned_url(self, object_name: str, expires: int = 3600) -> str:
        """获取预签名URL"""
        client = self._get_client()
        return client.presigned_get_object(settings.minio_bucket, object_name, expires=expires)

    def remove_object(self, object_name: str):
        client = self._get_client()
        client.remove_object(settings.minio_bucket, object_name)


class DataClient:
    """数据服务统一客户端"""

    def __init__(self):
        self.redis = RedisClient()
        self.kafka = KafkaClient()
        self.minio = MinioClient()
        self.mysql = MySQLClient()
        self._http_client: Optional["httpx.AsyncClient"] = None

    def _get_http_client(self):
        """获取全局复用的 httpx 连接池（懒加载，避免每请求新建）"""
        import httpx
        if self._http_client is None or self._http_client.is_closed:
            self._http_client = httpx.AsyncClient(
                timeout=httpx.Timeout(30.0, connect=5.0),
                limits=httpx.Limits(max_connections=50, max_keepalive_connections=10),
            )
        return self._http_client

    async def aclose(self):
        """关闭 HTTP 客户端（用于优雅停机）"""
        if self._http_client and not self._http_client.is_closed:
            await self._http_client.aclose()

    async def query_wms(self, sql: str, params: Dict = None) -> List[Dict]:
        """查询WMS Oracle数据（通过数据服务，只读）"""
        client = self._get_http_client()
        resp = await client.post(
            f"{settings.data_service_url}/v1/data/query",
            json={"sql": sql, "params": params or {}},
        )
        resp.raise_for_status()
        return resp.json()["data"]

    async def query_olap(self, sql: str, params: Dict = None) -> List[Dict]:
        """查询ClickHouse OLAP数据（支持 @paramName 参数化查询）"""
        import urllib.parse
        client = self._get_http_client()
        query_params = ""
        if params:
            encoded = urllib.parse.urlencode({f"@{k}": v for k, v in params.items()})
            query_params = f"?{encoded}"
        resp = await client.post(
            f"{settings.data_service_url}/v1/data/olap{query_params}",
            json={"sql": sql},
        )
        resp.raise_for_status()
        return resp.json()["data"]

    async def query_mysql(self, sql: str, params: Dict = None) -> List[Dict]:
        """查询 MySQL 数据（通过数据服务，只读）"""
        client = self._get_http_client()
        resp = await client.post(
            f"{settings.data_service_url}/v1/data/mysql",
            json={"sql": sql, "params": params or {}},
        )
        resp.raise_for_status()
        return resp.json()["data"]


# 全局单例（Redis/Kafka/Minio/MySQL 客户端本身不在此处建立网络连接，首次调用时才连）
data_client = DataClient()
redis_client = data_client.redis
kafka_client = data_client.kafka
minio_client = data_client.minio
mysql_client = data_client.mysql
