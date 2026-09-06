"""
WMS AI Platform - 五级缓存编排层
L1 本地缓存（TTLCache）→ L2 Redis 精确缓存 → L3 Milvus 语义缓存
L4 检索结果缓存（RAG 模块内实现）→ L5 Embedding 缓存（@cacheable on embed）

本模块提供：
  - SemanticCache:  L3 语义缓存（Milvus 向量检索相似问题）
  - LLMCacheLayer:  LLM 文本方法五级编排（L1→L2→L3）

关键：避免循环依赖
  - L3 语义缓存需要 embed() 生成向量
  - embed() 有 @cacheable（L1+L2+L5），不含 L3 语义缓存
  - 所以 L3 调 embed 不会触发 L3 自身，无循环

Milvus 不可用时降级为 "mock" 模式（跳过 L3，不影响 L1/L2）
"""
import asyncio
import hashlib
import time
from typing import Optional, Dict, Any, List
from loguru import logger

try:
    from cachetools import TTLCache
except ImportError:
    TTLCache = None

from common.config import settings
from common.data_client import redis_client
from common.cache_normalize import normalize_prompt


def _md5_hex(text: str) -> str:
    """字符串 → 32位 md5（用于缓存 key）"""
    return hashlib.md5(text.encode("utf-8")).hexdigest()


def _jitter_ttl(ttl: int, jitter: float = None) -> int:
    """TTL 随机抖动（防雪崩）"""
    import random
    if jitter is None:
        jitter = settings.cache_ttl_jitter
    if jitter <= 0:
        return ttl
    return max(1, int(ttl * (1 - jitter + 2 * jitter * random.random())))


class SemanticCache:
    """
    L3 语义缓存：Milvus 向量检索相似问题
    - 懒加载 Milvus 连接，不可用时降级 "mock"
    - get: 向量搜索 + 过滤 task_type/model/knowledge_version + 阈值判断 + feedback_score≥0.5
    - put: 插入 question+embedding+answer+元数据
    - 命中后异步更新 hit_count（不阻塞响应）
    """

    def __init__(self):
        self._collection = None  # 懒加载
        self._connected = False

    def _ensure_connection(self):
        """建立 Milvus 连接（使用独立 alias "cache" 避免与 RAG 模块的 default alias 冲突）"""
        if self._connected:
            return True
        try:
            from pymilvus import connections, utility

            connections.connect(
                alias="cache",
                host=settings.milvus_host,
                port=str(settings.milvus_port),
            )
            self._connected = True
            return True
        except Exception as e:
            logger.warning(f"语义缓存 Milvus 连接失败，降级跳过 L3: {e}")
            self._connected = False
            return False

    def _get_collection(self):
        """获取/创建 semantic_cache 集合"""
        if self._collection is not None:
            return self._collection

        if not self._ensure_connection():
            self._collection = "mock"
            return self._collection

        try:
            from pymilvus import connections, Collection, FieldSchema, CollectionSchema, DataType, utility

            col_name = "semantic_cache"
            if not utility.has_collection(col_name, using="cache"):
                # 创建集合
                fields = [
                    FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=True),
                    FieldSchema(name="question", dtype=DataType.VARCHAR, max_length=2000),
                    FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=settings.embedding_dim),
                    FieldSchema(name="answer", dtype=DataType.VARCHAR, max_length=8000),
                    FieldSchema(name="task_type", dtype=DataType.VARCHAR, max_length=64),
                    FieldSchema(name="model", dtype=DataType.VARCHAR, max_length=64),
                    FieldSchema(name="knowledge_version", dtype=DataType.VARCHAR, max_length=32),
                    FieldSchema(name="hit_count", dtype=DataType.INT32),
                    FieldSchema(name="feedback_score", dtype=DataType.FLOAT),
                    FieldSchema(name="create_time", dtype=DataType.INT64),
                ]
                schema = CollectionSchema(fields, description="WMS AI 语义缓存")
                col = Collection(col_name, schema, using="cache")
                # HNSW 索引，COSINE 距离
                col.create_index(
                    field_name="embedding",
                    index_params={
                        "index_type": "HNSW",
                        "metric_type": "COSINE",
                        "params": {"M": 16, "efConstruction": 200},
                    },
                )
                logger.info(f"语义缓存集合 {col_name} 创建成功")
                self._collection = col
            else:
                self._collection = Collection(col_name, using="cache")

            self._collection.load()
            return self._collection
        except Exception as e:
            logger.warning(f"语义缓存集合加载失败，降级跳过 L3: {e}")
            self._collection = "mock"
            return self._collection

    async def _embed_for_cache(self, question: str) -> List[float]:
        """
        获取问题向量（走 ai_client.embed，含 L5 缓存）
        注意：embed 的 @cacheable 只有 L1+L2，不含 L3，所以这里不会触发循环依赖
        """
        from common.ai_client import ai_client

        embeddings = await ai_client.embed([question])
        return embeddings[0] if embeddings else []

    def _do_search(self, col, embedding, task_type, model, knowledge_version, threshold) -> list:
        """同步 Milvus 搜索（在 to_thread 中调用）"""
        try:
            results = col.search(
                data=[embedding],
                anns_field="embedding",
                param={
                    "metric_type": "COSINE",
                    "params": {"ef": 64},
                },
                limit=1,
                expr=f'task_type == "{task_type}" and model == "{model}" '
                     f'and knowledge_version == "{knowledge_version}"',
                output_fields=["answer", "feedback_score", "question"],
            )
            return results
        except Exception as e:
            logger.warning(f"语义缓存搜索失败: {e}")
            return []

    async def get(
        self,
        question: str,
        task_type: str,
        model: str,
        knowledge_version: str,
        threshold: float = None,
    ) -> Optional[Dict[str, Any]]:
        """
        语义缓存查询：搜索相似问题，阈值≥0.92 且 feedback_score≥0.5 才命中

        Returns:
            {"answer": ..., "source": "l3_semantic", "similarity": ...} 或 None
        """
        col = self._get_collection()
        if col == "mock":
            return None

        # 阈值：NL2SQL 场景更高
        if threshold is None:
            threshold = (
                settings.cache_nl2sql_threshold
                if task_type == "nl2sql"
                else settings.cache_semantic_threshold
            )

        try:
            embedding = await self._embed_for_cache(question)
            if not embedding:
                return None

            results = await asyncio.to_thread(
                self._do_search, col, embedding, task_type, model, knowledge_version, threshold
            )

            if results and results[0]:
                hit = results[0][0]
                if hit.score >= threshold:
                    feedback = hit.entity.get("feedback_score", 1.0)
                    if feedback >= 0.5:
                        # 异步更新命中次数（不阻塞响应）
                        asyncio.create_task(asyncio.to_thread(self._incr_hit, col, hit.id))
                        answer = hit.entity.get("answer", "")
                        logger.debug(f"L3语义缓存命中: score={hit.score:.4f}")
                        try:
                            from common.cache_metrics import record_hit
                            record_hit("l3", "llm")
                        except Exception:
                            pass
                        return {
                            "answer": answer,
                            "source": "l3_semantic",
                            "similarity": hit.score,
                        }
        except Exception as e:
            logger.warning(f"语义缓存查询异常，跳过: {e}")

        return None

    def _incr_hit(self, col, pk: int):
        """同步更新命中次数"""
        try:
            col.update(ids=[pk], fields={"hit_count": 1})  # Milvus update semantics
        except Exception:
            pass  # 命中次数更新失败不影响主流程

    def _do_insert(self, col, question, embedding, answer, task_type, model, knowledge_version):
        """同步 Milvus 插入（在 to_thread 中调用）"""
        try:
            col.insert([
                [question],
                [embedding],
                [answer],
                [task_type],
                [model],
                [knowledge_version],
                [0],   # hit_count
                [1.0], # feedback_score 初始满分
                [int(time.time())],
            ])
        except Exception as e:
            logger.warning(f"语义缓存写入失败: {e}")

    async def put(
        self,
        question: str,
        answer: str,
        task_type: str,
        model: str,
        knowledge_version: str,
    ):
        """语义缓存写入（异步，不阻塞响应）"""
        col = self._get_collection()
        if col == "mock":
            return

        try:
            embedding = await self._embed_for_cache(question)
            if not embedding:
                return
            await asyncio.to_thread(
                self._do_insert,
                col,
                question,
                embedding,
                answer,
                task_type,
                model,
                knowledge_version,
            )
            logger.debug(f"L3语义缓存写入: task_type={task_type}")
        except Exception as e:
            logger.warning(f"语义缓存写入异常，跳过: {e}")

    def delete_by_feedback(self, negative: bool = True):
        """
        用户反馈差评时主动失效
        feedback_score 置 0 → 后续不再命中
        """
        col = self._get_collection()
        if col == "mock":
            return
        # 简化实现：实际场景按 feedback_score < 0.5 条件删除
        # Milvus delete by expr
        try:
            col.delete(expr='feedback_score < 0.5')
            logger.info("用户反馈触发 L3 语义缓存清理（低分条目）")
        except Exception as e:
            logger.warning(f"语义缓存反馈清理失败: {e}")


class LLMCacheLayer:
    """
    LLM 文本方法五级缓存编排：L1→L2→L3
    - L1: 本地 TTLCache
    - L2: Redis 精确缓存
    - L3: Milvus 语义缓存（仅 LLM 文本方法，非二进制/确定性方法）
    - L4: 检索结果缓存（在 RAG 模块内实现）
    - L5: Embedding 缓存（在 ai_client.embed 的 @cacheable 实现）

    缓存键格式：{task_type}:{model}:{version}:{md5(normalized_prompt)}
    """

    def __init__(self):
        # L1 本地缓存
        if TTLCache is not None:
            self.l1 = TTLCache(
                maxsize=settings.cache_l1_max_size,
                ttl=settings.cache_l1_ttl,
            )
        else:
            self.l1 = None

        self.redis = redis_client
        self.semantic = SemanticCache()

    def _get_version(self, module: str = "llm") -> str:
        """从 Redis 读版本号，不存在则 "v1" """
        try:
            v = self.redis.get(f"cache:version:{module}")
            return v if v else "v1"
        except Exception:
            return "v1"

    def _build_key(self, task_type: str, model: str, prompt: str, version: str) -> str:
        """构造缓存键"""
        normalized = normalize_prompt(prompt, task_type)
        return f"{task_type}:{model}:{version}:{_md5_hex(normalized)}"

    async def get(
        self,
        task_type: str,
        model: str,
        prompt: str,
        knowledge_version: str = None,
    ) -> Optional[Dict[str, Any]]:
        """
        查询五级缓存（L1→L2→L3）

        Returns:
            {"answer": ..., "source": "l1"/"l2"/"l3_semantic", ...} 或 None
        """
        version = knowledge_version or self._get_version(task_type)
        l1_key = self._build_key(task_type, model, prompt, version)

        # ===== L1 本地缓存 =====
        if self.l1 is not None:
            try:
                cached = self.l1.get(l1_key)
                if cached is not None:
                    logger.debug(f"LLMCacheLayer L1命中: {task_type}")
                    try:
                        from common.cache_metrics import record_hit, record_cost_saved
                        record_hit("l1", "llm")
                        record_cost_saved("llm")
                    except Exception:
                        pass
                    return {"answer": cached, "source": "l1"}
            except Exception as e:
                logger.warning(f"L1读取失败: {e}")

        # ===== L2 Redis 精确缓存 =====
        l2_key = f"llm:exact:{l1_key}"
        try:
            cached = await asyncio.to_thread(self.redis.get_json, l2_key)
            if cached is not None:
                logger.debug(f"LLMCacheLayer L2命中: {task_type}")
                # 回写 L1
                if self.l1 is not None:
                    try:
                        self.l1[l1_key] = cached
                    except Exception:
                        pass
                try:
                    from common.cache_metrics import record_hit, record_cost_saved
                    record_hit("l2", "llm")
                    record_cost_saved("llm")
                except Exception:
                    pass
                return {"answer": cached, "source": "l2"}
        except Exception as e:
            logger.warning(f"L2读取失败: {e}")

        # ===== L3 语义缓存 =====
        try:
            sem = await self.semantic.get(
                normalize_prompt(prompt, task_type),
                task_type,
                model,
                version,
            )
            if sem is not None:
                # 回写 L1 + L2
                answer = sem["answer"]
                if self.l1 is not None:
                    try:
                        self.l1[l1_key] = answer
                    except Exception:
                        pass
                try:
                    jittered = _jitter_ttl(settings.cache_l2_ttl)
                    await asyncio.to_thread(self.redis.set_json, l2_key, answer, jittered)
                except Exception:
                    pass
                try:
                    from common.cache_metrics import record_hit, record_cost_saved
                    record_hit("l3", "llm")
                    record_cost_saved("llm")
                except Exception:
                    pass
                return sem
        except Exception as e:
            logger.warning(f"L3语义缓存查询失败: {e}")

        return None

    async def put(
        self,
        task_type: str,
        model: str,
        prompt: str,
        answer: str,
        knowledge_version: str = None,
    ):
        """回写五级缓存（L1 + L2 + L3异步）"""
        version = knowledge_version or self._get_version(task_type)
        l1_key = self._build_key(task_type, model, prompt, version)
        l2_key = f"llm:exact:{l1_key}"

        # 写 L1
        if self.l1 is not None:
            try:
                self.l1[l1_key] = answer
            except Exception as e:
                logger.warning(f"L1写入失败: {e}")

        # 写 L2（TTL 随机抖动）
        try:
            jittered = _jitter_ttl(settings.cache_l2_ttl)
            await asyncio.to_thread(self.redis.set_json, l2_key, answer, jittered)
            logger.debug(f"LLMCacheLayer L2写入: {task_type} (TTL={jittered}s)")
        except Exception as e:
            logger.warning(f"L2写入失败: {e}")

        # 写 L3 语义缓存（异步，不阻塞响应）
        asyncio.create_task(
            self.semantic.put(
                normalize_prompt(prompt, task_type),
                answer,
                task_type,
                model,
                version,
            )
        )


# 全局单例
llm_cache_layer = LLMCacheLayer()
