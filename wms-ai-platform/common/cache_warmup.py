"""
WMS AI Platform - 缓存预热 + 主动失效
- 预热：apscheduler 定时任务，从 Redis 读高频问题，预生成缓存
- 主动失效：
  - 知识库更新→递增 knowledge_version（旧 L3 语义缓存自动不命中）
  - Prompt 更新→递增 prompt_version
  - 模型升级→模型字段变更（缓存键自动变化）
  - 用户反馈→feedback_score=0 + 删除低分条目
"""
import asyncio
import json
from typing import Optional
from loguru import logger

from common.config import settings
from common.data_client import redis_client


class CacheWarmup:
    """缓存预热 + 主动失效"""

    def __init__(self):
        self._scheduler = None

    # ========== 版本号管理 ==========

    @staticmethod
    def _incr_version(module: str) -> int:
        """
        递增缓存版本号，同时写入 "v{n}" 格式到 Redis
        确保 _get_version 读出来始终是 "v{n}" 格式
        """
        key = f"cache:version:{module}"
        raw = redis_client.get(key)
        cur = int(raw[1:]) if (raw and raw.startswith("v")) else 0
        new_ver = cur + 1
        redis_client.set(key, f"v{new_ver}")
        return new_ver

    # ========== 预热 ==========

    async def warmup(self, method: str = "llm", top_n: int = 50):
        """
        缓存预热：从 Redis 读高频 prompt，逐个调用触发缓存写入
        高频 prompt 维护在 cache:hot:{method}（ZSet，score=命中次数）

        Args:
            method:  预热的方法（llm / nl2sql / rag ...）
            top_n:   预热 Top N 条
        """
        try:
            # 从 ZSet 读高频 prompt（score 降序）
            hot_key = f"cache:hot:{method}"
            items = await asyncio.to_thread(
                redis_client.client.zrevrange, hot_key, 0, top_n - 1, withscores=True
            )
            if not items:
                logger.info(f"缓存预热：无高频 prompt（{method}），跳过")
                return

            logger.info(f"缓存预热：{method} 共 {len(items)} 条，开始预生成")

            from common.ai_client import ai_client

            for item in items:
                prompt_text = item[0] if isinstance(item[0], str) else item[0].decode("utf-8")
                try:
                    # 走正常 llm_chat 流程，自动写入五级缓存
                    task_type = "nl2sql" if method == "nl2sql" else "rag"
                    await ai_client.llm_chat(
                        prompt_text,
                        temperature=0.0,
                        task_type=task_type,
                    )
                except Exception as e:
                    logger.warning(f"缓存预热单条失败: {prompt_text[:50]}... → {e}")

            logger.info(f"缓存预热完成：{method} {len(items)} 条")
        except Exception as e:
            logger.error(f"缓存预热失败: {e}")

    def start_scheduler(self):
        """启动 apscheduler 定时预热"""
        if self._scheduler is not None:
            return

        try:
            from apscheduler.schedulers.asyncio import AsyncIOScheduler
            from apscheduler.triggers.cron import CronTrigger

            self._scheduler = AsyncIOScheduler()

            # 解析 cron 表达式
            parts = settings.cache_warmup_cron.split()
            trigger = CronTrigger(
                minute=parts[0],
                hour=parts[1],
                day=parts[2],
                month=parts[3],
                day_of_week=parts[4],
            )

            # 每日定时预热 LLM + NL2SQL
            self._scheduler.add_job(
                lambda: asyncio.create_task(self.warmup("llm")),
                trigger=trigger,
                id="warmup_llm",
                name="缓存预热-LLM",
                replace_existing=True,
            )
            self._scheduler.add_job(
                lambda: asyncio.create_task(self.warmup("nl2sql")),
                trigger=trigger,
                id="warmup_nl2sql",
                name="缓存预热-NL2SQL",
                replace_existing=True,
            )

            self._scheduler.start()
            logger.info(f"缓存预热调度器已启动（cron={settings.cache_warmup_cron}）")
        except Exception as e:
            logger.warning(f"缓存预热调度器启动失败（不影响主流程）: {e}")

    def stop_scheduler(self):
        """停止定时预热"""
        if self._scheduler is not None:
            try:
                self._scheduler.shutdown(wait=False)
                logger.info("缓存预热调度器已停止")
            except Exception:
                pass
            self._scheduler = None

    # ========== 主动失效 ==========

    def invalidate_by_doc(self, doc_id: str = None, module: str = "rag"):
        """
        知识库文档更新→递增 knowledge_version
        旧 L3 语义缓存的 knowledge_version 过滤条件不匹配，自动失效
        无需批量删除 Milvus 条目
        """
        try:
            new_ver = self._incr_version(module)
            version_str = f"v{new_ver}"
            logger.info(f"知识库版本升级: {module} → {version_str}（doc_id={doc_id}）")

            try:
                from common.cache_metrics import record_invalidation
                record_invalidation("doc_update")
            except Exception:
                pass

            # 可选：按 doc_id 删除 L4 检索缓存
            if doc_id:
                try:
                    # L4 检索缓存键含 knowledge_version，版本升级后自动失效
                    # 这里只做日志记录，实际 L4 缓存键会因版本号变化而自然失效
                    logger.debug(f"L4 检索缓存因版本升级自动失效: {doc_id}")
                except Exception:
                    pass

            return version_str
        except Exception as e:
            logger.error(f"知识库版本升级失败: {e}")
            return None

    def invalidate_by_prompt(self, module: str = "llm"):
        """
        Prompt 模板更新→递增 prompt_version
        """
        try:
            new_ver = self._incr_version(module)
            version_str = f"v{new_ver}"
            logger.info(f"Prompt 版本升级: {module} → {version_str}")

            try:
                from common.cache_metrics import record_invalidation
                record_invalidation("prompt_update")
            except Exception:
                pass

            return version_str
        except Exception as e:
            logger.error(f"Prompt 版本升级失败: {e}")
            return None

    def invalidate_by_model(self, module: str = "llm"):
        """
        模型升级→模型字段变更
        缓存键含 model 字段，模型变更后旧键自然不命中
        这里递增版本号确保彻底失效
        """
        try:
            new_ver = self._incr_version(module)
            version_str = f"v{new_ver}"
            logger.info(f"模型升级触发版本升级: {module} → {version_str}")

            try:
                from common.cache_metrics import record_invalidation
                record_invalidation("model_update")
            except Exception:
                pass

            return version_str
        except Exception as e:
            logger.error(f"模型升级版本失效失败: {e}")
            return None

    def feedback(self, question: str, positive: bool, task_type: str = "rag"):
        """
        用户反馈：影响语义缓存质量
        - 点赞：延长 TTL + feedback_score 维持
        - 点踩：feedback_score=0 + 删除低分条目
        """
        try:
            from common.cache_layer import llm_cache_layer

            sem_cache = llm_cache_layer.semantic

            if positive:
                # 点赞：语义缓存中该条目 feedback_score 维持高分
                # 简化实现：记录正向反馈次数
                redis_client.incr(f"cache:feedback:positive:{task_type}")
                logger.debug(f"用户正向反馈: {question[:50]}...")
            else:
                # 点踩：触发语义缓存低分清理
                sem_cache.delete_by_feedback(negative=True)
                redis_client.incr(f"cache:feedback:negative:{task_type}")
                logger.info(f"用户负向反馈，触发语义缓存低分清理: {question[:50]}...")

                try:
                    from common.cache_metrics import record_invalidation
                    record_invalidation("feedback")
                except Exception:
                    pass
        except Exception as e:
            logger.warning(f"用户反馈处理失败: {e}")

    # ========== 高频问题维护 ==========

    @staticmethod
    def record_hot_prompt(method: str, prompt: str):
        """
        记录高频 prompt（供预热使用）
        在 ai_client.llm_chat 每次调用时调用此方法
        维护 ZSet cache:hot:{method}，score=调用次数
        """
        try:
            hot_key = f"cache:hot:{method}"
            redis_client.client.zincrby(hot_key, 1, prompt)
            # 只保留 Top 200，防止无限增长
            redis_client.client.zremrangebyrank(hot_key, 0, -201)
        except Exception:
            pass  # 高频维护失败不影响主流程


# 全局单例
warmup_service = CacheWarmup()
