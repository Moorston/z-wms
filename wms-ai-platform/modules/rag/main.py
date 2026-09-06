"""
WMS AI Platform - 智能问答 RAG 模块（PRD V2.0 重构）

重构内容：
  - upload_document：ParserRegistry 解析（5种格式）→ DefaultChunker 分块 → embed → Milvus + MySQL
  - chat：问题改写 → HybridRetriever 混合检索 → BgeReranker 重排 → LLMGenerator 生成
  - list_documents：MySQL 查询真实文档
  - 新增 feedback 端点：点赞/点踩，点踩触发 L3 语义缓存失效
  - 删除旧 _split_text、假 Rerank sorted(docs, key=score)
"""
import os
import sys
import time
import asyncio
from typing import List, Dict, Optional, AsyncIterator
from fastapi import FastAPI, UploadFile, File, HTTPException, Form
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from loguru import logger

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
from common.config import settings
from common.ai_client import ai_client
from common.data_client import redis_client, minio_client, mysql_client
from common.utils import Result, gen_id, now_str, md5
from common.cache_metrics import setup_metrics
from common.tracing import init_tracing, instrument_app

# B2-T1~T4 子模块
from modules.rag.parser import ParserRegistry
from modules.rag.chunker import DefaultChunker
from modules.rag.retriever import HybridRetriever
from modules.rag.rerank import BgeReranker
from modules.rag.monitor import retrieval_monitor
from modules.rag.eval.evaluator import RetrievalEvaluator
from modules.rag.knowledge import KnowledgeManager
from modules.rag.chat import ChatSessionManager
from modules.rag.generator import LLMGenerator
from modules.rag.kg import KGStore, KnowledgeGraphRetriever, extract_triples

app = FastAPI(title="WMS AI - 智能问答RAG模块", version="2.0.0")
setup_metrics(app)
init_tracing("rag")
instrument_app(app, "rag")


# ========== 请求/响应模型 ==========

class ChatRequest(BaseModel):
    question: str
    session_id: Optional[str] = None
    top_k: int = 5
    user_id: Optional[str] = None
    warehouse: Optional[str] = None


class FeedbackRequest(BaseModel):
    message_id: int
    feedback: str  # like / dislike
    feedback_note: Optional[str] = None


class KnowledgeDoc(BaseModel):
    title: str
    content: str
    category: str = "general"
    tags: List[str] = []


# ========== RAG 服务（重构后） ==========

class RAGService:
    """RAG 问答服务（集成解析+分块+混合检索+Rerank+生成）"""

    def __init__(self):
        self.collection_name = "wms_knowledge"
        self.parser = ParserRegistry.get_instance()
        self.chunker = DefaultChunker(chunk_size=512, overlap=64)
        self.retriever = HybridRetriever()
        self.reranker = BgeReranker(top_n=settings.rag_retrieval_rerank_top_n)
        self.generator = LLMGenerator()
        self.knowledge = KnowledgeManager()
        self.session = ChatSessionManager()
        self.kg_store = KGStore()
        self.kg_retriever = KnowledgeGraphRetriever(store=self.kg_store)

    def _get_knowledge_version(self) -> str:
        """知识库版本号（缓存键组成部分）"""
        v = redis_client.get("cache:version:rag")
        return v if v else "v1"

    async def upload_document(self, file: UploadFile, category: str = "general",
                              warehouse: str = None, created_by: str = None) -> Dict:
        """
        上传文档：解析 → 分块 → 向量化 → Milvus + MySQL
        修复旧版 content.decode("utf-8") 乱码 bug
        """
        content = await file.read()
        filename = file.filename or "unknown"
        file_ext = os.path.splitext(filename)[1].lower()
        file_type = file_ext.lstrip(".") or "unknown"

        # 1. 上传到 MinIO
        minio_object = f"{self.knowledge.minio_prefix}/{gen_id()}_{filename}"
        try:
            minio_client.upload_bytes(minio_object, content)
        except Exception as e:
            logger.warning(f"MinIO上传失败（降级跳过）: {e}")
            minio_object = None

        # 2. 创建文档记录（status=pending）
        doc_id = self.knowledge.create_document(
            title=filename, category=category, warehouse=warehouse,
            source_file=filename, file_type=file_type,
            file_size=len(content), minio_object=minio_object,
            created_by=created_by,
        )

        # 3. 更新状态为 processing
        self.knowledge.update_status(doc_id, "processing")

        try:
            # 4. 解析文档（ParserRegistry 自动判断类型）
            parsed = self.parser.parse(filename=filename, content=content)

            # 5. 分块（DefaultChunker 自动选策略）
            chunks = self.chunker.chunk(
                sections=parsed.sections, tables=parsed.tables,
                text=parsed.get_full_text() if not parsed.sections and not parsed.tables else None,
            )

            if not chunks:
                self.knowledge.update_status(doc_id, "failed", error_msg="解析后无有效内容")
                return {"doc_id": doc_id, "status": "failed", "error": "解析后无有效内容"}

            # 6. 向量化
            texts = [c.content for c in chunks]
            embeddings = await ai_client.embed(texts)

            # 7. 存入 Milvus
            embedding_ids = await self._insert_to_milvus(doc_id, chunks, embeddings)

            # 8. 分块写入 MySQL
            self.knowledge.save_chunks(doc_id, chunks, embedding_ids)

            # 8.5 KG 知识图谱抽取（降级安全：失败不翻转文档状态）
            if settings.rag_kg_enabled:
                try:
                    all_text = "\n".join(c.content for c in chunks)
                    triples = extract_triples(all_text, doc_id)
                    if triples:
                        self.kg_store.save_triples(triples)
                except Exception as e:
                    logger.warning(f"KG 抽取失败（降级跳过）: doc_id={doc_id}, error={e}")

            # 9. 知识库版本递增（旧 L3/L4 缓存自动失效）
            redis_client.incr("cache:version:rag")
            try:
                from common.cache_warmup import warmup_service
                warmup_service.invalidate_by_doc(doc_id=doc_id, module="rag")
            except Exception:
                pass

            logger.info(f"文档上传完成: doc_id={doc_id}, chunks={len(chunks)}")
            return {"doc_id": doc_id, "status": "completed", "chunks": len(chunks)}

        except Exception as e:
            logger.error(f"文档处理失败: doc_id={doc_id}, error={e}")
            self.knowledge.update_status(doc_id, "failed", error_msg=str(e))
            return {"doc_id": doc_id, "status": "failed", "error": str(e)}

    async def _insert_to_milvus(self, doc_id: str, chunks: list,
                                embeddings: list) -> list:
        """插入向量到 Milvus，返回 embedding_id 列表"""
        collection = self.retriever.vector_retriever._get_collection()
        if collection is None:
            logger.warning("Milvus不可用，跳过向量插入")
            return [None] * len(chunks)

        try:
            titles = [doc_id] * len(chunks)
            contents = [c.content[:8000] for c in chunks]
            doc_ids = [doc_id] * len(chunks)
            chunk_indices = [c.position for c in chunks]

            # 使用 upsert 获取 Milvus 返回的真实 auto-ID
            ins = collection.upsert([
                embeddings,  # embedding
                titles,
                contents,
                doc_ids,
                chunk_indices,
            ])
            collection.flush()
            ids = [str(i) for i in ins.primary_keys] if hasattr(ins, "primary_keys") else [None] * len(chunks)
            logger.info(f"Milvus插入 {len(chunks)} 条向量, doc_id={doc_id}, ids={ids[:3]}...")
            return ids
        except Exception as e:
            logger.error(f"Milvus插入失败: {e}")
            return [None] * len(chunks)

    async def chat(self, req: ChatRequest) -> Dict:
        """
        RAG 问答：问题改写 → 混合检索 → Rerank → LLM 生成
        """
        import time
        start = time.time()

        # 1. 创建/复用会话
        session_id = req.session_id
        if not session_id:
            session_id = self.session.create_session(
                user_id=req.user_id, module="rag",
                title=req.question[:50], warehouse=req.warehouse,
            )

        # 2. 保存用户问题
        self.session.add_message(session_id, "user", req.question)

        # 2.5 R5 意图路由：简单问候/管理指令直接回复，跳过完整 RAG 流水线
        intent = self._classify_intent(req.question)
        if intent != "knowledge":
            direct_answer = self._direct_response(intent)
            self.session.add_message(session_id, "assistant", direct_answer,
                                     sources=[], confidence=1.0,
                                     model="intent_router", latency_ms=0)
            return {
                "question": req.question,
                "answer": direct_answer,
                "sources": [],
                "session_id": session_id,
                "message_id": "",
                "confidence": 1.0,
                "intent": intent,
            }

        # 3. 获取对话上下文（提前到改写之前，供改写+生成共用）
        history = self.session.get_context(session_id)

        # 4. 三路并行查询生成（原文 + 改写 + 扩展）
        rewritten_q, expanded_variants = await asyncio.gather(
            self._rewrite_question(req.question, history=history),
            self._expand_query(req.question),
            return_exceptions=True,
        )
        # 降级：改写失败用原问题
        if isinstance(rewritten_q, Exception) or not rewritten_q:
            logger.warning(f"问题改写异常，使用原问题: {rewritten_q}")
            rewritten_q = req.question
        # 降级：扩展失败返回空列表
        if isinstance(expanded_variants, Exception):
            logger.warning(f"查询扩展异常，降级跳过: {expanded_variants}")
            expanded_variants = []

        # 合并三路查询，去重（保持顺序：原文→改写→扩展）
        queries = [req.question]
        if rewritten_q and rewritten_q != req.question:
            queries.append(rewritten_q)
        for v in expanded_variants:
            if v and v not in queries:
                queries.append(v)
        refined_q = rewritten_q or req.question  # 用于缓存键和 Rerank

        # 5. L4 检索结果缓存（含 warehouse 维度，避免跨仓库缓存污染）
        kv = self._get_knowledge_version()
        l4_key = f"rag:retrieval:{kv}:{req.warehouse or ''}:{md5(refined_q)}"
        cached = None
        try:
            cached = redis_client.get_json(l4_key)
        except Exception:
            pass

        if cached is not None:
            logger.debug(f"L4检索缓存命中: {req.question[:30]}...")
            docs = cached
        else:
            # 6. 多路混合检索（向量+关键词 RRF 融合，warehouse 过滤穿透）
            docs = await self.retriever.retrieve_multi(
                queries, top_k=settings.rag_retrieval_top_k,
                warehouse=req.warehouse,
            )

            # 6.5 KG 检索（独立通道，不走 RRF）
            if settings.rag_kg_enabled and self.kg_retriever:
                try:
                    kg_docs = await self.kg_retriever.retrieve(
                        refined_q, top_k=settings.rag_kg_top_k,
                        warehouse=req.warehouse,
                    )
                    docs.extend(kg_docs)
                except Exception as e:
                    logger.warning(f"KG 检索失败（降级跳过）: {e}")

            # 7. Rerank（BgeReranker Top5）
            if docs:
                try:
                    docs = await self.reranker.rerank(refined_q, docs, top_n=req.top_k)
                except Exception as e:
                    logger.warning(f"Rerank失败，降级用检索结果: {e}")
                    docs = docs[:req.top_k]

            # 写 L4 缓存
            try:
                redis_client.set_json(l4_key, docs, expire=settings.cache_l4_ttl)
            except Exception:
                pass

        # 8. LLM 生成答案
        result = await self.generator.generate(req.question, docs, history)
        answer = result["answer"]
        confidence = result["confidence"]
        latency_ms = int((time.time() - start) * 1000)

        # 9. 保存 AI 回复
        message_id = self.session.add_message(
            session_id, "assistant", answer,
            sources=docs, confidence=confidence,
            model=result.get("model"), latency_ms=latency_ms,
        )

        # 10. 记录检索监控
        await self._log_retrieval(req, refined_q, docs, latency_ms, session_id)

        # 11. 返回结果（R4：sources 含 doc_id/chunk_index，客户端可精确链接知识库分块）
        return {
            "question": req.question,
            "answer": answer,
            "sources": [{"title": d.get("title", ""),
                         "content": d.get("content", "")[:200],
                         "score": d.get("relevance_score", d.get("rrf_score", 0)),
                         "doc_id": d.get("doc_id", ""),
                         "chunk_index": d.get("chunk_index", 0)}
                        for d in docs],
            "session_id": session_id,
            "message_id": message_id,
            "confidence": confidence,
            "verification": result.get("verification"),
        }

    async def chat_stream(self, req: ChatRequest) -> AsyncIterator[str]:
        """
        RAG 问答（流式输出 SSE）：问题改写 → 混合检索 → Rerank → 流式生成

        SSE 格式：
          首条: data: {"type":"meta","sources":[...],"session_id":"..."}
          中间: data: {"type":"chunk","content":"..."}
          末尾: data: {"type":"done","answer":"完整答案","confidence":0.84}
                data: [DONE]
        """
        import json as json_mod
        import time as time_mod

        start = time_mod.time()

        # 1. 创建/复用会话
        session_id = req.session_id
        if not session_id:
            session_id = self.session.create_session(
                user_id=req.user_id, module="rag",
                title=req.question[:50], warehouse=req.warehouse,
            )

        # 2. 保存用户问题
        self.session.add_message(session_id, "user", req.question)

        # 2.5 R5 意图路由：简单问候/管理指令直接回复，跳过完整 RAG 流水线
        intent = self._classify_intent(req.question)
        if intent != "knowledge":
            direct_answer = self._direct_response(intent)
            self.session.add_message(session_id, "assistant", direct_answer,
                                     sources=[], confidence=1.0,
                                     model="intent_router", latency_ms=0)
            # SSE: meta + done
            meta_payload = json_mod.dumps(
                {"type": "meta", "sources": [], "session_id": session_id, "intent": intent},
                ensure_ascii=False,
            )
            yield "data: " + meta_payload + "\n\n"
            chunk_payload = json_mod.dumps(
                {"type": "chunk", "content": direct_answer},
                ensure_ascii=False,
            )
            yield "data: " + chunk_payload + "\n\n"
            done_payload = json_mod.dumps(
                {"type": "done", "answer": direct_answer, "confidence": 1.0,
                 "message_id": "", "latency_ms": 0},
                ensure_ascii=False,
            )
            yield "data: " + done_payload + "\n\n"
            yield "data: [DONE]\n\n"
            return

        # 3. 获取对话上下文
        history = self.session.get_context(session_id)

        # 4. 三路并行查询生成（原文 + 改写 + 扩展）
        rewritten_q, expanded_variants = await asyncio.gather(
            self._rewrite_question(req.question, history=history),
            self._expand_query(req.question),
            return_exceptions=True,
        )
        if isinstance(rewritten_q, Exception) or not rewritten_q:
            logger.warning(f"问题改写异常，使用原问题: {rewritten_q}")
            rewritten_q = req.question
        if isinstance(expanded_variants, Exception):
            logger.warning(f"查询扩展异常，降级跳过: {expanded_variants}")
            expanded_variants = []

        # 合并三路查询，去重
        queries = [req.question]
        if rewritten_q and rewritten_q != req.question:
            queries.append(rewritten_q)
        for v in expanded_variants:
            if v and v not in queries:
                queries.append(v)
        refined_q = rewritten_q or req.question

        # 5. L4 检索结果缓存
        kv = self._get_knowledge_version()
        l4_key = f"rag:retrieval:{kv}:{req.warehouse or ''}:{md5(refined_q)}"
        cached = None
        try:
            cached = redis_client.get_json(l4_key)
        except Exception:
            pass

        if cached is not None:
            logger.debug(f"L4检索缓存命中: {req.question[:30]}...")
            docs = cached
        else:
            # 6. 多路混合检索
            docs = await self.retriever.retrieve_multi(
                queries, top_k=settings.rag_retrieval_top_k,
                warehouse=req.warehouse,
            )

            # 6.5 KG 检索（独立通道，不走 RRF）
            if settings.rag_kg_enabled and self.kg_retriever:
                try:
                    kg_docs = await self.kg_retriever.retrieve(
                        refined_q, top_k=settings.rag_kg_top_k,
                        warehouse=req.warehouse,
                    )
                    docs.extend(kg_docs)
                except Exception as e:
                    logger.warning(f"KG 检索失败（降级跳过）: {e}")

            # 7. Rerank
            if docs:
                try:
                    docs = await self.reranker.rerank(refined_q, docs, top_n=req.top_k)
                except Exception as e:
                    logger.warning(f"Rerank失败，降级用检索结果: {e}")
                    docs = docs[:req.top_k]

            # 写 L4 缓存
            try:
                redis_client.set_json(l4_key, docs, expire=settings.cache_l4_ttl)
            except Exception:
                pass

        # 8. 首条 SSE: meta 消息（sources + session_id，R4：含 doc_id/chunk_index）
        sources = [{"title": d.get("title", ""),
                    "content": d.get("content", "")[:200],
                    "score": d.get("relevance_score", d.get("rrf_score", 0)),
                    "doc_id": d.get("doc_id", ""),
                    "chunk_index": d.get("chunk_index", 0)}
                   for d in docs]
        meta_payload = json_mod.dumps(
            {"type": "meta", "sources": sources, "session_id": session_id},
            ensure_ascii=False,
        )
        yield "data: " + meta_payload + "\n\n"

        # 9. 流式生成答案
        full_answer = ""
        async for chunk in self.generator.generate_stream(req.question, docs, history):
            chunk_str = chunk if isinstance(chunk, str) else str(chunk)
            full_answer += chunk_str
            chunk_payload = json_mod.dumps(
                {"type": "chunk", "content": chunk_str},
                ensure_ascii=False,
            )
            yield "data: " + chunk_payload + "\n\n"

        # 10. 置信度估算（流式模式不调用 LLMVerifier）
        confidence = self.generator._estimate_confidence(docs)
        latency_ms = int((time_mod.time() - start) * 1000)

        # 11. 保存 AI 回复
        message_id = self.session.add_message(
            session_id, "assistant", full_answer,
            sources=docs, confidence=confidence,
            model=settings.llm_model, latency_ms=latency_ms,
        )

        # 12. 记录检索监控
        await self._log_retrieval(req, refined_q, docs, latency_ms, session_id)

        # 13. 末尾 SSE: done 消息 + [DONE]
        done_payload = json_mod.dumps(
            {"type": "done", "answer": full_answer,
             "confidence": confidence,
             "message_id": message_id,
             "latency_ms": latency_ms},
            ensure_ascii=False,
        )
        yield "data: " + done_payload + "\n\n"
        yield "data: [DONE]\n\n"

    async def _log_retrieval(self, req: ChatRequest, refined_q: str,
                             docs: List[Dict], latency_ms: int,
                             session_id: str = None):
        """记录检索日志到监控（异步非阻塞，降级安全）"""
        try:
            from modules.rag.monitor import RetrievalLogEntry
            from datetime import datetime
            top_score = 0.0
            if docs:
                top_score = float(docs[0].get("relevance_score", docs[0].get("rrf_score", 0)))
            entry = RetrievalLogEntry(
                ts=datetime.now().isoformat(),
                query=req.question[:256],
                refined_query=refined_q[:256],
                total_docs=len(docs),
                latency_ms=latency_ms,
                top_score=top_score,
                hit=1 if docs else 0,
                chunk_ids=[d.get("chunk_index", d.get("chunk_id", 0)) for d in docs],
                session_id=session_id or "",
                user_id=req.user_id or "",
            )
            await retrieval_monitor.log(entry)
        except Exception:
            pass  # 监控绝不影响主流程

    def delete_document(self, doc_id: str):
        """
        完整删除文档：MySQL + Milvus 向量 + MinIO + 缓存失效
        修复 R1：删除后不再返回"幽灵答案"
        """
        # 1. 删除 MySQL（级联删 kb_chunk）
        self.knowledge.delete_document(doc_id)

        # 2. 删除 Milvus 向量（按 doc_id 过滤）
        try:
            collection = self.retriever.vector_retriever._get_collection()
            if collection is not None:
                expr = f'doc_id == "{doc_id}"'
                collection.delete(expr)
                collection.flush()
                logger.info(f"Milvus 向量已清除: doc_id={doc_id}")
        except Exception as e:
            logger.warning(f"Milvus 向量删除失败（降级跳过）: {e}")

        # 2.5 KG 三元组删除（降级安全）
        if settings.rag_kg_enabled:
            try:
                self.kg_store.delete_by_doc(doc_id)
            except Exception as e:
                logger.warning(f"KG 删除失败（降级跳过）: doc_id={doc_id}, error={e}")

        # 3. 缓存版本 bump — 使所有 L4 检索缓存失效
        try:
            current = redis_client.get("cache:version:rag")
            new_ver = f"v{int(current.lstrip('v')) + 1}" if current and current.startswith("v") else "v2"
            redis_client.set("cache:version:rag", new_ver)
            logger.info(f"RAG 缓存版本已更新: {current} → {new_ver}")
        except Exception as e:
            logger.warning(f"缓存版本更新失败: {e}")

    # ---- R5 意图路由 ----
    _INTENT_PATTERNS = {
        "greeting": [
            r"^(你好|您好|hello|hi|嗨|早上好|晚上好|下午好)(?:\s|$)",
            r"^(谢谢|感谢|thanks|thank you)(?:\s|$)",
            r"^(再见|bye|拜拜|goodbye)(?:\s|$)",
            r"^(你是谁|what are you)(?:\s|$)",
        ],
        "admin": [
            r"^(列出|查看|列出所有)\s*(文档|知识库|分类)",
            r"^(删除|移除)\s*文档",
            r"^(搜索|查找)\s*文档",
        ],
    }

    def _classify_intent(self, question: str) -> str:
        """
        R5 意图分类：区分简单交互（greeting/admin）与知识库查询（knowledge）
        返回 "greeting" / "admin" / "knowledge"
        """
        import re
        q = question.strip().lower()
        for intent, patterns in self._INTENT_PATTERNS.items():
            for pat in patterns:
                if re.match(pat, q):
                    return intent
        return "knowledge"

    def _direct_response(self, intent: str) -> str:
        """简单意图的固定回复"""
        if intent == "greeting":
            return "你好！我是 WMS 智能助手，可以帮你解答仓库管理相关问题。有什么可以帮你的？"
        if intent == "admin":
            return "请使用 /rag/documents 接口查看知识库文档列表，或使用 /rag/upload 上传新文档。"
        return "请问有什么问题？"

    async def _rewrite_question(self, question: str,
                               history: List[Dict] = None) -> str:
        """问题改写（提取关键词+扩展同义词），含对话上下文支持代词消解"""
        history_text = ""
        if history and len(history) > 1:
            lines = []
            for msg in history[:-1]:
                role = "用户" if msg.get("role") == "user" else "助手"
                lines.append(f"{role}: {msg.get('content', '')[:512]}")
            history_text = "\n\n对话历史:\n" + "\n".join(lines)

        prompt = f"""对以下问题进行改写，提取关键词并扩展同义词，支持代词消解。
{history_text}
原问题：{question}
改写后（只输出改写后的问题，不要解释）："""
        try:
            return await ai_client.llm_chat(prompt, temperature=0.0, task_type="rag")
        except Exception as e:
            logger.warning(f"问题改写失败，使用原问题: {e}")
            return question

    async def _expand_query(self, question: str) -> List[str]:
        """生成 2 个查询变体（同义词/缩写/英文翻译），降级安全"""
        prompt = f"""基于以下问题生成 2 个关键词变体（使用不同的同义词、缩写或英文翻译）。
每个变体单独一行，只输出变体，不要解释。

原问题：{question}
变体1：
变体2："""
        try:
            result = await ai_client.llm_chat(prompt, temperature=0.3, task_type="rag")
            lines = [l.strip() for l in result.strip().split("\n") if l.strip()]
            variants = []
            for line in lines:
                # 去除"变体1："等前缀
                line = line.split("：")[-1] if "：" in line else line
                line = line.split(":")[-1] if ":" in line and not line.startswith("http") else line
                line = line.strip()
                if line and line != question:
                    variants.append(line)
            return variants[:2]
        except Exception as e:
            logger.warning(f"查询扩展失败，降级为两路召回: {e}")
            return []

    def list_documents(self, category: str = None, status: str = None,
                       limit: int = 100, offset: int = 0) -> List[Dict]:
        """列出知识库文档（MySQL 查询，替换旧 return []）"""
        return self.knowledge.list_documents(
            category=category, status=status, limit=limit, offset=offset
        )

    def feedback(self, message_id: int, feedback: str,
                 feedback_note: str = None) -> Dict:
        """消息反馈（like/dislike）"""
        self.session.update_feedback(message_id, feedback, feedback_note)
        return {"message_id": message_id, "feedback": feedback}


rag_service = RAGService()


# ========== API 端点 ==========

@app.post("/rag/chat")
async def chat(req: ChatRequest):
    """智能问答（多轮对话 + RAG 检索）"""
    return Result.success(await rag_service.chat(req))


@app.post("/rag/chat/stream")
async def chat_stream(req: ChatRequest):
    """
    智能问答（流式输出 SSE）

    SSE 格式：
      data: {"type":"meta","sources":[...],"session_id":"..."}
      data: {"type":"chunk","content":"..."}
      data: {"type":"done","answer":"完整答案","confidence":0.84}
      data: [DONE]
    """
    return StreamingResponse(
        rag_service.chat_stream(req),
        media_type="text/event-stream",
    )


@app.post("/rag/upload")
async def upload_document(
    file: UploadFile = File(...),
    category: str = Form("general"),
    warehouse: str = Form(None),
    created_by: str = Form(None),
):
    """上传文档（支持 PDF/Word/Excel/Markdown/图片 5 种格式）"""
    return Result.success(
        await rag_service.upload_document(file, category, warehouse, created_by)
    )


@app.post("/rag/document")
async def add_document(doc: KnowledgeDoc):
    """直接添加文本知识（无需文件上传）"""
    # 转为虚拟文件上传
    content_bytes = doc.content.encode("utf-8")
    from io import BytesIO
    file = UploadFile(filename=f"{doc.title}.txt", file=BytesIO(content_bytes))
    return Result.success(await rag_service.upload_document(file, doc.category))


@app.get("/rag/documents")
async def list_documents(category: str = None, status: str = None,
                         limit: int = 100, offset: int = 0):
    """列出知识库文档"""
    return Result.success(
        rag_service.list_documents(category, status, limit, offset)
    )


@app.get("/rag/documents/{doc_id}")
async def get_document(doc_id: str):
    """获取单个文档详情"""
    doc = rag_service.knowledge.get_document(doc_id)
    if not doc:
        raise HTTPException(status_code=404, detail="文档不存在")
    return Result.success(doc)


@app.delete("/rag/documents/{doc_id}")
async def delete_document(doc_id: str):
    """删除文档（MySQL + Milvus 向量 + 缓存失效）"""
    rag_service.delete_document(doc_id)
    return Result.success({"doc_id": doc_id, "deleted": True})


@app.post("/rag/feedback")
async def feedback(req: FeedbackRequest):
    """消息反馈（点赞/点踩）"""
    if req.feedback not in ("like", "dislike"):
        raise HTTPException(status_code=400, detail="feedback 必须为 like 或 dislike")
    return Result.success(rag_service.feedback(req.message_id, req.feedback, req.feedback_note))


@app.get("/rag/categories")
async def get_categories():
    """获取分类树"""
    return Result.success(rag_service.knowledge.get_category_tree())


@app.get("/rag/evaluate")
async def evaluate_retrieval(top_k: int = 5, dataset: str = None):
    """
    RAG 检索质量评估（Recall@K + MRR）

    运行评估数据集，返回各指标得分
    """
    from modules.rag.eval.evaluator import run_evaluation
    try:
        metrics = run_evaluation(dataset_path=dataset, top_k=top_k, verbose=False)
        if metrics is None:
            return Result.error("无法初始化检索器，请检查 Milvus/MySQL 连接")
        return Result.success({
            "metrics": metrics.to_dict(),
            "per_sample": metrics.per_sample,
        })
    except Exception as e:
        return Result.error(f"评估失败: {str(e)}")


@app.get("/rag/monitor/stats")
async def monitor_stats():
    """RAG 检索监控统计"""
    return Result.success(retrieval_monitor.stats())


@app.on_event("startup")
async def on_startup():
    """启动时初始化监控后台任务"""
    await retrieval_monitor.start()


@app.on_event("shutdown")
async def on_shutdown():
    """关闭时刷入剩余监控数据"""
    await retrieval_monitor.stop()


@app.get("/health")
async def health():
    return Result.success({
        "status": "ok",
        "module": "rag",
        "version": "2.0.0",
        "parsers": list(rag_service.parser._parsers.keys()),
        "mysql": "mock" if mysql_client.is_mock else "connected",
    })


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8104)
