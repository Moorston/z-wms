"""
LLM 答案生成器 — 从 RAG 主流程提取，支持流式输出
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

from typing import List, Dict, Optional, AsyncIterator
from loguru import logger

from common.ai_client import ai_client
from common.config import settings
from modules.rag.verifier.llm_verifier import LLMVerifier


class LLMGenerator:
    """LLM 答案生成器（封装 prompt 构建 + 上下文注入）"""

    async def generate(self, question: str, context_docs: List[Dict],
                       history: List[Dict] = None,
                       stream: bool = False) -> Dict:
        """
        生成答案（V2: system+user 双消息格式）

        Args:
            question:      用户问题
            context_docs:  检索到的参考文档（含 title/content/relevance_score）
            history:       对话历史 [{"role":..., "content":...}]
            stream:         是否流式输出
        Returns:
            {"answer": str, "confidence": float, "model": str, "latency_ms": int}
        """
        messages = self._build_messages(question, context_docs, history)

        import time
        start = time.time()
        answer = await ai_client.llm_chat_messages(
            messages, task_type="rag", temperature=0.1,
        )
        latency_ms = int((time.time() - start) * 1000)

        # 置信度估算：基于参考文档的相关性分数
        confidence = self._estimate_confidence(context_docs)

        # R4: LLM Self-Check 答案验证（降级安全）
        verifier = LLMVerifier()
        verification = await verifier.verify(answer, context_docs, confidence)

        return {
            "answer": answer,
            "confidence": verification["confidence_adjusted"],
            "model": settings.llm_model,
            "latency_ms": latency_ms,
            "verification": verification,
        }

    async def generate_stream(self, question: str,
                              context_docs: List[Dict],
                              history: List[Dict] = None) -> AsyncIterator[str]:
        """
        流式生成答案（V2: system+user 双消息格式）

        使用 _build_messages() 构建双消息，通过 llm_chat_messages_stream 流式输出。
        流式模式下不调用 LLMVerifier（避免延迟），置信度基于 retrieval 估算。
        """
        messages = self._build_messages(question, context_docs, history)
        async for chunk in ai_client.llm_chat_messages_stream(
            messages, temperature=0.1,
        ):
            yield chunk

    def _build_prompt(self, question: str, context_docs: List[Dict],
                      history: List[Dict] = None) -> str:
        """构建 LLM prompt（注入参考文档 + 对话历史）"""
        # 参考文档拼接
        ref_parts = []
        for i, doc in enumerate(context_docs, 1):
            title = doc.get("title", "")
            content = doc.get("content", "")
            ref_parts.append(f"[参考{i}] {title}\n{content}")
        context = "\n\n".join(ref_parts) if ref_parts else "无相关参考资料"

        # 对话历史拼接
        history_text = ""
        if history and len(history) > 1:
            history_lines = []
            for msg in history[:-1]:  # 排除当前问题
                role = "用户" if msg["role"] == "user" else "助手"
                history_lines.append(f"{role}: {msg['content'][:200]}")
            history_text = "\n\n对话历史:\n" + "\n".join(history_lines)

        prompt = f"""基于以下参考资料回答用户问题。如果资料中没有答案，请说"暂无相关信息"，不要编造。

参考资料：
{context}
{history_text}

用户问题：{question}

请用简洁的中文回答，分点说明操作步骤。如果引用了参考资料，在句末标注[参考序号]。"""
        return prompt

    def _build_messages(self, question: str, context_docs: List[Dict],
                        history: List[Dict] = None) -> List[Dict]:
        """
        构建 system+user 双消息格式（V2 替代 _build_prompt 的单字符串）

        - system 消息含角色定位 + chunk_type 动态风格指令 + 引用格式说明
        - user 消息含参考文档 + 对话历史（512 截断+标记）+ 用户问题
        """
        # 根据 chunk_type 动态生成回答风格指令
        chunk_types = set(d.get("chunk_type", "text") for d in context_docs)
        if "table" in chunk_types:
            style_hint = "如果涉及表格数据，请以表格形式呈现关键信息。"
        elif "heading" in chunk_types:
            style_hint = "请按标题结构组织回答，分点说明。"
        else:
            style_hint = "请用简洁的中文回答，分点说明操作步骤。"

        system_msg = (
            "你是一个仓储管理系统（WMS）智能助手。基于参考资料回答用户问题。"
            "如果资料中没有答案，请说\"暂无相关信息\"，不要编造。"
            f"{style_hint}"
            "引用格式：在引用的句子末尾标注[参考序号]，如[1]。"
        )

        # 参考文档拼接
        ref_parts = []
        for i, doc in enumerate(context_docs, 1):
            title = doc.get("title", "")
            content = doc.get("content", "")
            ref_parts.append(f"[参考{i}] {title}\n{content}")
        context = "\n\n".join(ref_parts) if ref_parts else "无相关参考资料"

        # 历史拼接（智能截断：512 字符 + 截断标记）
        history_text = ""
        if history and len(history) > 1:
            history_lines = []
            for msg in history[:-1]:  # 排除当前问题
                role = "用户" if msg.get("role") == "user" else "助手"
                raw_content = msg.get("content", "")
                content = raw_content[:512]
                if len(raw_content) > 512:
                    content += "...（已截断）"
                history_lines.append(f"{role}: {content}")
            history_text = "\n\n对话历史:\n" + "\n".join(history_lines)

        user_msg = f"""参考资料：
{context}
{history_text}

用户问题：{question}"""

        return [
            {"role": "system", "content": system_msg},
            {"role": "user", "content": user_msg},
        ]

    def _estimate_confidence(self, docs: List[Dict]) -> float:
        """基于参考文档相关性分数估算置信度"""
        if not docs:
            return 0.0
        # 取前3个文档的分数均值（score 或 rrf_score 或 relevance_score）
        scores = []
        for d in docs[:3]:
            s = d.get("relevance_score") or d.get("rrf_score") or d.get("score", 0)
            scores.append(float(s))
        if not scores:
            return 0.5
        avg = sum(scores) / len(scores)
        # 归一化到 0-1（RRF score 通常较小，放大）
        return min(avg * 5, 1.0) if avg < 0.2 else min(avg, 1.0)
