"""
WMS AI Platform - AI模型统一调用客户端
所有业务模块通过此客户端调用AI能力，不直接依赖具体模型框架
支持：LLM / OCR / 目标检测 / 图像分类 / ASR / TTS / 时序预测 / 异常检测
"""
import httpx
import re
import time
import imghdr
from collections.abc import AsyncIterator
from typing import List, Dict, Any, Optional
from loguru import logger
from tenacity import retry, stop_after_attempt, wait_exponential

from common.config import settings
from common.cache import cacheable


class AIClient:
    """AI模型统一调用客户端"""

    def __init__(self, base_url: str = None):
        self.base_url = base_url or settings.model_service_url
        self.timeout = 60.0
        # 共享httpx客户端，复用连接池，避免每次调用重复TCP/TLS握手
        self._http = httpx.AsyncClient(timeout=self.timeout)

    def _image_field(self, image_bytes: bytes, field_name: str = "image"):
        """根据字节内容检测图片格式，返回 (filename, bytes, content_type) 三元组供 httpx files 上传"""
        ext = imghdr.what(None, h=image_bytes) or "jpg"
        # imghdr 返回 jpeg 时 MIME 仍是 image/jpeg
        mime_ext = "jpeg" if ext in ("jpg", "jpeg") else ext
        return (field_name, (f"image.{ext}", image_bytes, f"image/{mime_ext}"))

    # ========== LLM ==========
    @retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=2, max=10))
    async def llm_chat(self, prompt: str, model: str = None,
                       temperature: float = None, max_tokens: int = None,
                       task_type: str = "rag") -> str:
        """
        调用LLM聊天接口（五级缓存编排：L1→L2→L3 语义缓存）

        Args:
            prompt:       用户 prompt
            model:        模型名（None 用默认 llm_model）
            temperature:  采样温度（仅 temperature=0 或 None 时才缓存，避免随机性）
            max_tokens:   最大生成 token
            task_type:    任务类型（rag / nl2sql / llm ...），影响标准化策略和语义阈值
        """
        from common.cache_layer import llm_cache_layer

        actual_model = model or settings.llm_model
        # temperature=0 或 None 时启用缓存（确定性输出）
        cacheable_temp = temperature is None or temperature == 0.0

        # 1. 查五级缓存（仅确定性输出才查）
        if cacheable_temp:
            cached = await llm_cache_layer.get(task_type, actual_model, prompt)
            if cached is not None:
                logger.debug(f"LLM缓存命中({cached['source']}): {task_type}")
                # 记录 LLM 调用日志（缓存命中）
                try:
                    from common.tracing import llm_call_logger
                    llm_call_logger.log_call(
                        task_type=task_type, model=actual_model,
                        prompt=prompt, answer=cached["answer"],
                        latency_ms=0, cache_hit=cached["source"],
                        cache_key="", status="success",
                    )
                except Exception:
                    pass
                # 记录高频 prompt（供预热）
                try:
                    from common.cache_warmup import warmup_service
                    warmup_service.record_hot_prompt(
                        "nl2sql" if task_type == "nl2sql" else "llm", prompt
                    )
                except Exception:
                    pass
                return cached["answer"]

        # 2. 真实调用 LLM
        _llm_start = time.time()
        payload = {
            "model": actual_model,
            "messages": [{"role": "user", "content": prompt}],
            "temperature": temperature if temperature is not None else settings.llm_temperature,
            "max_tokens": max_tokens or settings.llm_max_tokens,
        }
        _llm_status = "success"
        _llm_error = None
        answer = ""
        try:
            resp = await self._http.post(f"{self.base_url}/v1/llm/chat", json=payload)
            resp.raise_for_status()
            answer = resp.json()["content"]
        except Exception as e:
            _llm_status = "error"
            _llm_error = str(e)
            # 记录 LLM 调用日志（调用失败）
            try:
                from common.tracing import llm_call_logger
                llm_call_logger.log_call(
                    task_type=task_type, model=actual_model,
                    prompt=prompt, answer="",
                    latency_ms=(time.time() - _llm_start) * 1000,
                    cache_hit="none", status=_llm_status,
                    error_msg=_llm_error, module=task_type,
                )
            except Exception:
                pass
            raise

        # 记录 LLM 调用日志（真实调用成功）
        _llm_latency = (time.time() - _llm_start) * 1000
        try:
            from common.tracing import llm_call_logger
            llm_call_logger.log_call(
                task_type=task_type, model=actual_model,
                prompt=prompt, answer=answer,
                latency_ms=_llm_latency, cache_hit="none",
                status=_llm_status, module=task_type,
            )
        except Exception:
            pass

        # 3. 回写五级缓存（仅确定性输出）
        if cacheable_temp:
            try:
                await llm_cache_layer.put(task_type, actual_model, prompt, answer)
                # 记录高频 prompt
                from common.cache_warmup import warmup_service
                warmup_service.record_hot_prompt(
                    "nl2sql" if task_type == "nl2sql" else "llm", prompt
                )
            except Exception as e:
                logger.warning(f"LLM缓存回写失败，跳过: {e}")

        return answer

    async def llm_chat_messages(self, messages: List[Dict], model: str = None,
                                temperature: float = None, max_tokens: int = None,
                                task_type: str = "rag") -> str:
        """
        多消息格式调用 LLM（支持 system+user 双消息）

        与 llm_chat() 的区别：接受完整 messages 列表而非单个 prompt 字符串。
        复用现有缓存（L1→L2→L3）/重试/降级逻辑，缓存键隔离用 md5(messages 序列)。

        Args:
            messages:     消息列表 [{"role":"system","content":...}, {"role":"user","content":...}]
            model:        模型名（None 用默认 llm_model）
            temperature:  采样温度（仅 temperature=0 或 None 时才缓存）
            max_tokens:   最大生成 token
            task_type:    任务类型
        """
        import json
        from common.cache_layer import llm_cache_layer

        actual_model = model or settings.llm_model
        cacheable_temp = temperature is None or temperature == 0.0
        # 缓存键：messages 序列化的 md5（与 llm_chat 的 prompt 键隔离）
        cache_prompt_key = json.dumps(messages, ensure_ascii=False)

        # 1. 查五级缓存（仅确定性输出才查）
        if cacheable_temp:
            cached = await llm_cache_layer.get(task_type, actual_model, cache_prompt_key)
            if cached is not None:
                logger.debug(f"LLM(messages)缓存命中({cached['source']}): {task_type}")
                return cached["answer"]

        # 2. 真实调用 LLM
        payload = {
            "model": actual_model,
            "messages": messages,
            "temperature": temperature if temperature is not None else settings.llm_temperature,
            "max_tokens": max_tokens or settings.llm_max_tokens,
        }
        try:
            resp = await self._http.post(f"{self.base_url}/v1/llm/chat", json=payload)
            resp.raise_for_status()
            answer = resp.json()["content"]
        except Exception as e:
            logger.error(f"LLM(messages)调用失败: {e}")
            raise

        # 3. 回写五级缓存（仅确定性输出）
        if cacheable_temp:
            try:
                await llm_cache_layer.put(task_type, actual_model, cache_prompt_key, answer)
            except Exception as e:
                logger.warning(f"LLM(messages)缓存回写失败，跳过: {e}")

        return answer

    async def llm_chat_stream(self, prompt: str, model: str = None) -> AsyncIterator[str]:
        """流式调用LLM（单字符串 prompt，兼容旧接口）"""
        payload = {
            "model": model or settings.llm_model,
            "messages": [{"role": "user", "content": prompt}],
            "stream": True,
        }
        async with self._http.stream("POST", f"{self.base_url}/v1/llm/chat", json=payload) as resp:
            async for line in resp.aiter_lines():
                if line.startswith("data: "):
                    yield line[6:]

    async def llm_chat_messages_stream(self, messages: List[Dict], model: str = None,
                                       temperature: float = None) -> AsyncIterator[str]:
        """
        流式调用 LLM（双消息格式，支持 system+user）

        与 llm_chat_stream 的区别：接受完整 messages 列表而非单个 prompt 字符串。
        """
        payload = {
            "model": model or settings.llm_model,
            "messages": messages,
            "temperature": temperature if temperature is not None else settings.llm_temperature,
            "stream": True,
        }
        async with self._http.stream("POST", f"{self.base_url}/v1/llm/chat", json=payload) as resp:
            async for line in resp.aiter_lines():
                if line.startswith("data: "):
                    yield line[6:]

    async def llm_extract_json(self, prompt: str, schema: Dict = None) -> Dict:
        """LLM抽取结构化JSON数据"""
        full_prompt = prompt
        if schema:
            full_prompt += f"\n\n输出JSON格式，字段定义：{schema}"
        full_prompt += "\n\n只输出JSON，不要解释，不要markdown代码块。"
        result = await self.llm_chat(full_prompt)
        import json
        try:
            # 用正则提取首个 {...} 块，兼容 ```{...}``` 同行/无换行/mixed text
            result = result.strip()
            m = re.search(r'\{.*\}', result, re.DOTALL)
            if m:
                result = m.group(0)
            return json.loads(result)
        except json.JSONDecodeError as e:
            logger.error(f"LLM JSON解析失败: {result[:200]}, error: {e}")
            return {"raw": result, "parse_error": str(e)}

    # ========== CV - OCR ==========
    @cacheable("ai:ocr", ttl=86400, key_args=["image_bytes", "lang"])
    @retry(stop=stop_after_attempt(2), wait=wait_exponential(multiplier=1, min=1, max=5))
    async def ocr_recognize(self, image_bytes: bytes, lang: str = None) -> Dict:
        """OCR识别，返回文字+位置（DeepSeek-OCR-2 API，通过 model_service 代理）"""
        files = {"image": self._image_field(image_bytes)}
        data = {}
        resp = await self._http.post(f"{self.base_url}/v1/cv/ocr", files=files, data=data)
        resp.raise_for_status()
        return resp.json()

    # ========== CV - 目标检测 ==========
    @cacheable("ai:cv:detect", ttl=86400, key_args=["image_bytes", "model"])
    async def object_detect(self, image_bytes: bytes, model: str = "yolov10") -> List[Dict]:
        """目标检测"""
        files = {"image": self._image_field(image_bytes)}
        data = {"model": model}
        resp = await self._http.post(f"{self.base_url}/v1/cv/detect", files=files, data=data)
        resp.raise_for_status()
        return resp.json()["detections"]

    # ========== CV - 图像分类 ==========
    @cacheable("ai:cv:classify", ttl=86400, key_args=["image_bytes", "candidate_skus"])
    async def image_classify(self, image_bytes: bytes, candidate_skus: List[str] = None) -> Dict:
        """图像分类（Few-shot识别SKU）"""
        files = {"image": self._image_field(image_bytes)}
        data = {"candidate_skus": ",".join(candidate_skus or [])}
        resp = await self._http.post(f"{self.base_url}/v1/cv/classify", files=files, data=data)
        resp.raise_for_status()
        return resp.json()

    # ========== 语音 ==========
    async def asr_recognize(self, audio_bytes: bytes) -> str:
        """语音识别"""
        files = {"audio": ("audio.wav", audio_bytes, "audio/wav")}
        resp = await self._http.post(f"{self.base_url}/v1/voice/asr", files=files)
        resp.raise_for_status()
        return resp.json()["text"]

    async def tts_synthesize(self, text: str) -> bytes:
        """语音合成"""
        resp = await self._http.post(f"{self.base_url}/v1/voice/tts", json={"text": text})
        resp.raise_for_status()
        return resp.content

    # ========== 时序预测 ==========
    @cacheable("ai:forecast", ttl=3600, key_args=["series", "periods", "model"])
    async def forecast_predict(self, series: List[float], periods: int = 30,
                               model: str = None, extras: Dict = None) -> Dict:
        """时序预测"""
        payload = {
            "series": series,
            "periods": periods,
            "model": model or settings.forecast_model,
            "extras": extras or {},
        }
        resp = await self._http.post(f"{self.base_url}/v1/forecast/predict", json=payload)
        resp.raise_for_status()
        return resp.json()

    # ========== 异常检测 ==========
    async def anomaly_detect(self, data: List[float], method: str = "iforest") -> List[bool]:
        """异常检测，返回是否异常的布尔列表"""
        payload = {"data": data, "method": method}
        resp = await self._http.post(f"{self.base_url}/v1/anomaly/detect", json=payload)
        resp.raise_for_status()
        return resp.json()["anomalies"]

    # ========== Embedding ==========
    @cacheable("ai:embed", ttl=2592000, key_args=["texts"])
    async def embed(self, texts: List[str]) -> List[List[float]]:
        """文本向量化（底层走 SiliconFlow bge-m3 API，由 model_service 代理）"""
        payload = {"texts": texts, "model": settings.embedding_model}
        resp = await self._http.post(f"{self.base_url}/v1/llm/embed", json=payload)
        resp.raise_for_status()
        return resp.json()["embeddings"]

    # ========== Rerank ==========
    async def rerank(self, query: str, documents: List[str], top_n: int = 5) -> List[Dict]:
        """
        重排序（SiliconFlow bge-reranker-v2-m3，由 model_service 代理）
        Top20 召回 → Rerank → Top5

        Args:
            query:    查询文本
            documents: 召回文档列表
            top_n:     返回前 N 条
        Returns:
            [{"index": int, "document": str, "relevance_score": float}, ...]
        """
        payload = {
            "query": query,
            "documents": documents,
            "top_n": top_n,
            "model": settings.rerank_model,
        }
        resp = await self._http.post(f"{self.base_url}/v1/llm/rerank", json=payload)
        resp.raise_for_status()
        return resp.json()["results"]

    # ========== OCR Online 增强 ==========
    async def ocr_online_enhance(self, image_bytes: bytes, prompt: str = None) -> Dict:
        """
        在线OCR增强（SiliconFlow DeepSeek-OCR-2，由 model_service 代理）
        用于二次增强或需要结构化提取时调用

        Args:
            image_bytes: 图片二进制数据
            prompt:      附加指令（可选）
        Returns:
            {"text": str, "model": str}
        """
        import base64
        img_b64 = base64.b64encode(image_bytes).decode("utf-8")
        payload = {
            "image_base64": img_b64,
            "prompt": prompt,
            "model": settings.ocr_online_model,
        }
        resp = await self._http.post(f"{self.base_url}/v1/llm/ocr_online", json=payload)
        resp.raise_for_status()
        return resp.json()


# 全局单例
ai_client = AIClient()
