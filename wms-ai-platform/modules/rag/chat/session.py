"""
对话会话管理 — 多轮对话上下文 + MySQL 持久化
写 chat_session / chat_message 表，支持 >=10 轮上下文
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

import json
from typing import List, Dict, Optional
from loguru import logger

from common.data_client import mysql_client
from common.utils import gen_id, now_str


class ChatSessionManager:
    """对话会话管理（MySQL 持久化，mock 降级安全）"""

    MAX_CONTEXT_MESSAGES = 10  # 最多取最近 10 轮上下文

    def create_session(self, user_id: str = None, module: str = "rag",
                       title: str = None, warehouse: str = None) -> str:
        """创建新会话，返回 session_id"""
        session_id = gen_id()
        sql = """
            INSERT INTO chat_session (session_id, user_id, module, title, warehouse, message_count)
            VALUES (%s, %s, %s, %s, %s, %s)
        """
        mysql_client.execute(sql, (session_id, user_id, module, title, warehouse, 0))
        logger.info(f"会话创建: session_id={session_id}, module={module}")
        return session_id

    def add_message(self, session_id: str, role: str, content: str,
                    sources: list = None, confidence: float = None,
                    model: str = None, latency_ms: int = None) -> int:
        """
        添加对话消息

        Args:
            session_id: 会话ID
            role:       角色（user/assistant）
            content:    消息内容
            sources:    引用来源列表
            confidence: 置信度
            model:      使用的模型名
            latency_ms: 响应延迟
        Returns:
            message_id
        """
        sql = """
            INSERT INTO chat_message
            (session_id, role, content, sources, confidence, model, latency_ms)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
        """
        message_id = mysql_client.insert(sql, (
            session_id, role, content,
            json.dumps(sources, ensure_ascii=False) if sources else None,
            confidence, model, latency_ms,
        ))

        # 更新会话消息数
        mysql_client.execute(
            "UPDATE chat_session SET message_count = message_count + 1 WHERE session_id = %s",
            (session_id,),
        )
        return message_id

    def get_context(self, session_id: str, max_messages: int = None) -> List[Dict]:
        """
        获取最近 N 轮对话上下文（默认 10 轮 = 20 条消息）

        Args:
            session_id:  会话ID
            max_messages: 最大消息数
        Returns:
            [{"role":..., "content":...}, ...]
        """
        limit = max_messages or self.MAX_CONTEXT_MESSAGES * 2
        rows = mysql_client.query(
            "SELECT role, content FROM chat_message WHERE session_id = %s "
            "ORDER BY created_at DESC LIMIT %s",
            (session_id, limit),
        )
        # 反转为时间正序
        rows.reverse()
        return [{"role": r["role"], "content": r["content"]} for r in rows]

    def list_sessions(self, user_id: str = None, module: str = None,
                      limit: int = 50) -> List[Dict]:
        """列出会话"""
        conditions = []
        params = []
        if user_id:
            conditions.append("user_id = %s")
            params.append(user_id)
        if module:
            conditions.append("module = %s")
            params.append(module)
        where = " WHERE " + " AND ".join(conditions) if conditions else ""
        sql = f"SELECT * FROM chat_session{where} ORDER BY updated_at DESC LIMIT %s"
        params.append(limit)
        return mysql_client.query(sql, tuple(params))

    def get_messages(self, session_id: str, limit: int = 100) -> List[Dict]:
        """获取会话消息历史"""
        return mysql_client.query(
            "SELECT * FROM chat_message WHERE session_id = %s ORDER BY created_at",
            (session_id,),
        )

    def update_feedback(self, message_id: int, feedback: str,
                        feedback_note: str = None):
        """
        更新消息反馈（like/dislike）
        dislike 时触发 L3 语义缓存失效
        """
        sql = "UPDATE chat_message SET feedback = %s, feedback_note = %s WHERE message_id = %s"
        mysql_client.execute(sql, (feedback, feedback_note, message_id))
        logger.info(f"反馈更新: message_id={message_id}, feedback={feedback}")

        if feedback == "dislike":
            # 触发 L3 语义缓存失效
            try:
                from common.cache_warmup import warmup_service
                warmup_service.invalidate_by_doc(doc_id=None, module="rag")
                logger.debug("dislike 触发 L3 语义缓存失效")
            except Exception as e:
                logger.warning(f"L3 缓存失效失败: {e}")
