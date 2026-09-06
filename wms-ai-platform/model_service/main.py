"""
WMS AI Platform - 统一模型服务
提供 LLM / OCR / 目标检测 / 图像分类 / ASR / TTS / 时序预测 / 异常检测 统一API
支持 Mock 模式（开发环境不加载真实模型，返回模拟数据）

PRD V2.0 改动：
  - LLM: DeepSeek-V4-Pro 主力 + GLM-5.2 备选，自动故障转移
  - Embedding: SiliconFlow bge-m3 API（不再本地加载模型）
  - 新增 /v1/llm/rerank: SiliconFlow bge-reranker-v2-m3
  - OCR 统一改为 DeepSeek-OCR-2 API（移除本地 OCR 引擎）
  - ASR 统一改为 XingChenASR-V3.2-Ultra API（移除本地 FunASR 引擎）
  - 模型路由: 按 task_type 选模型
"""
import os
import sys
import json
import time
import asyncio
import base64
from typing import List, Dict, Any, Optional, Tuple
from contextlib import asynccontextmanager

import httpx
from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.responses import Response
from pydantic import BaseModel
from loguru import logger

# 添加上级目录到path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from common.config import settings
from common.utils import Result, gen_id
from common.cache_metrics import setup_metrics
from common.tracing import init_tracing, instrument_app

# Mock模式开关（开发环境设为true，不加载真实模型）
MOCK_MODE = os.getenv("MOCK_MODE", "false").lower() == "true"

# 模型实例（懒加载）
_llm_primary = None       # DeepSeek 主力
_llm_fallback = None      # GLM 备选
_yolo_model = None
_tts_model = None
_clip_model = None
# 共享 httpx 客户端（SiliconFlow API 调用复用连接池）
_sf_http: Optional[httpx.AsyncClient] = None


def _get_sf_http() -> httpx.AsyncClient:
    """获取 SiliconFlow 共享 httpx 客户端"""
    global _sf_http
    if _sf_http is None:
        _sf_http = httpx.AsyncClient(timeout=60.0)
    return _sf_http


def get_llm_clients() -> Tuple[Any, Any]:
    """
    获取 LLM 客户端元组 (primary, fallback)
    - primary: DeepSeek-V4-Pro
    - fallback: GLM-5.2（自动故障转移备选）
    懒加载，MOCK_MODE 返回 (None, None)
    """
    global _llm_primary, _llm_fallback
    if MOCK_MODE:
        return None, None
    if _llm_primary is None:
        from openai import OpenAI
        _llm_primary = OpenAI(
            base_url=settings.llm_base_url,
            api_key=settings.llm_api_key,
        )
        logger.info(f"LLM主力客户端就绪: {settings.llm_base_url} / {settings.llm_model}")
    if _llm_fallback is None:
        from openai import OpenAI
        _llm_fallback = OpenAI(
            base_url=settings.llm_fallback_base_url,
            api_key=settings.llm_fallback_api_key,
        )
        logger.info(f"LLM备选客户端就绪: {settings.llm_fallback_base_url} / {settings.llm_fallback_model}")
    return _llm_primary, _llm_fallback


# 兼容旧调用（get_llm_client 返回主力客户端）
def get_llm_client():
    primary, _ = get_llm_clients()
    return primary


def _get_model_for_task(task_type: str = "default") -> str:
    """
    模型路由：按 task_type 从 settings.model_route JSON 选模型
    task_type: rag / nl2sql / ocr_extract / default
    """
    try:
        route = json.loads(settings.model_route)
        return route.get(task_type, route.get("default", settings.llm_model))
    except Exception:
        return settings.llm_model


def get_ocr_engine():
    """获取OCR引擎（DeepSeek-OCR-2 API，通过 SiliconFlow 代理）"""
    return True  # 始终可用，实际调用走 SiliconFlow API


def get_yolo_model():
    """获取YOLO目标检测模型"""
    global _yolo_model
    if MOCK_MODE:
        return None
    if _yolo_model is None:
        from ultralytics import YOLO
        _yolo_model = YOLO("yolov10n.pt")
    return _yolo_model


def get_clip_model():
    """获取CLIP模型（Few-shot零样本SKU分类）"""
    global _clip_model
    if MOCK_MODE:
        return None
    if _clip_model is None:
        from sentence_transformers import SentenceTransformer
        _clip_model = SentenceTransformer("clip-ViT-B-32")
    return _clip_model


# ========== 请求/响应模型 ==========
class ChatRequest(BaseModel):
    model: str = "deepseek-v4-pro"
    messages: List[Dict[str, str]]
    temperature: float = 0.1
    max_tokens: int = 4096
    stream: bool = False
    task_type: str = "default"  # rag / nl2sql / ocr_extract / default


class EmbedRequest(BaseModel):
    texts: List[str]
    model: str = "BAAI/bge-m3"


class RerankRequest(BaseModel):
    query: str
    documents: List[str]
    top_n: int = 5
    model: str = "BAAI/bge-reranker-v2-m3"


class OCROnlineRequest(BaseModel):
    image_base64: str
    prompt: Optional[str] = None
    model: str = "deepseek-ai/DeepSeek-OCR-2"


class ForecastRequest(BaseModel):
    series: List[float]
    periods: int = 30
    model: str = "prophet"
    extras: Dict[str, Any] = {}


class AnomalyRequest(BaseModel):
    data: List[float]
    method: str = "iforest"


class TTSRequest(BaseModel):
    text: str


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期"""
    logger.info(f"模型服务启动, MOCK_MODE={MOCK_MODE}")
    if not MOCK_MODE:
        logger.info("LLM客户端懒加载（首次请求时初始化）")
    yield
    logger.info("模型服务关闭")
    # 关闭共享 httpx 客户端
    global _sf_http
    if _sf_http is not None:
        await _sf_http.aclose()
        _sf_http = None


app = FastAPI(title="WMS AI Model Service", version="2.0.0", lifespan=lifespan)
setup_metrics(app)
init_tracing("model_service")
instrument_app(app, "model_service")


# ========== LLM 接口 ==========
@app.post("/v1/llm/chat")
async def llm_chat(req: ChatRequest):
    """
    LLM聊天接口（DeepSeek 主力 → GLM 备选自动故障转移）
    返回中含 fallback 字段标识是否触发了降级
    """
    if MOCK_MODE:
        mock_responses = {
            "default": "这是Mock模式下的模拟回复。生产环境会调用真实LLM模型。",
        }
        content = mock_responses.get("default")
        if len(req.messages) > 0:
            user_msg = req.messages[-1].get("content", "")
            if "JSON" in user_msg or "json" in user_msg:
                content = json.dumps({"status": "mock", "message": "模拟JSON输出"}, ensure_ascii=False)
        return Result.success({"content": content, "model": req.model, "fallback": False})

    # 模型路由：按 task_type 选模型（覆盖 req.model）
    routed_model = _get_model_for_task(req.task_type)
    primary, fallback = get_llm_clients()

    # 1. 先调主力 DeepSeek
    try:
        response = primary.chat.completions.create(
            model=routed_model,
            messages=req.messages,
            temperature=req.temperature,
            max_tokens=req.max_tokens,
        )
        return Result.success({
            "content": response.choices[0].message.content,
            "model": routed_model,
            "usage": response.usage.model_dump() if response.usage else None,
            "fallback": False,
        })
    except Exception as e:
        logger.warning(f"主力LLM(DeepSeek)调用失败，切换备选(GLM): {e}")

    # 2. 主力失败 → 自动切换 GLM 备选
    try:
        fallback_model = settings.llm_fallback_model
        response = fallback.chat.completions.create(
            model=fallback_model,
            messages=req.messages,
            temperature=req.temperature,
            max_tokens=req.max_tokens,
        )
        logger.info("GLM备选LLM调用成功（已降级）")
        return Result.success({
            "content": response.choices[0].message.content,
            "model": fallback_model,
            "usage": response.usage.model_dump() if response.usage else None,
            "fallback": True,
        })
    except Exception as e:
        logger.error(f"备选LLM(GLM)也调用失败: {e}")
        raise HTTPException(status_code=503, detail=f"主力+备选LLM均不可用: {e}")


@app.post("/v1/llm/embed")
async def llm_embed(req: EmbedRequest):
    """
    文本向量化（SiliconFlow bge-m3 API）
    不再本地加载 SentenceTransformer 模型
    """
    if MOCK_MODE:
        import random
        embeddings = [[random.uniform(-0.1, 0.1) for _ in range(settings.embedding_dim)] for _ in req.texts]
        return Result.success({"embeddings": embeddings, "model": req.model})

    try:
        http = _get_sf_http()
        resp = await http.post(
            f"{settings.embedding_base_url}/embeddings",
            headers={"Authorization": f"Bearer {settings.embedding_api_key}"},
            json={
                "model": req.model,
                "input": req.texts,
                "encoding_format": "float",
            },
        )
        resp.raise_for_status()
        data = resp.json()
        embeddings = [item["embedding"] for item in data["data"]]
        return Result.success({"embeddings": embeddings, "model": req.model})
    except Exception as e:
        logger.error(f"Embedding(SiliconFlow)失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/v1/llm/rerank")
async def llm_rerank(req: RerankRequest):
    """
    重排序接口（SiliconFlow bge-reranker-v2-m3 API）
    Top20 召回 → Rerank → Top5
    """
    if MOCK_MODE:
        # Mock：按原文档顺序返回，score 递减
        results = [
            {"index": i, "document": doc, "relevance_score": round(1.0 - i * 0.15, 4)}
            for i, doc in enumerate(req.documents[:req.top_n])
        ]
        return Result.success({"results": results, "model": req.model})

    try:
        http = _get_sf_http()
        resp = await http.post(
            f"{settings.rerank_base_url}/rerank",
            headers={"Authorization": f"Bearer {settings.rerank_api_key}"},
            json={
                "model": req.model,
                "query": req.query,
                "documents": req.documents,
                "top_n": req.top_n,
                "return_documents": True,
            },
        )
        resp.raise_for_status()
        data = resp.json()
        results = [
            {
                "index": item["index"],
                "document": item.get("document", req.documents[item["index"]]),
                "relevance_score": item["relevance_score"],
            }
            for item in data["results"]
        ]
        return Result.success({"results": results, "model": req.model})
    except Exception as e:
        logger.error(f"Rerank(SiliconFlow)失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/v1/llm/ocr_online")
async def llm_ocr_online(req: OCROnlineRequest):
    """
    在线OCR增强接口（SiliconFlow DeepSeek-OCR-2）
    图片以 base64 image_url 传入，模型返回结构化文字
    """
    if MOCK_MODE:
        return Result.success({
            "text": "模拟在线OCR识别结果\n供应商：测试供应商\n商品：测试商品\n数量：100",
            "model": req.model,
        })

    try:
        http = _get_sf_http()
        # 构建 chat completions 请求，图片作为 image_url
        user_content = []
        if req.prompt:
            user_content.append({"type": "text", "text": req.prompt})
        else:
            user_content.append({"type": "text", "text": "请识别图片中的所有文字，按原始布局输出。"})
        user_content.append({
            "type": "image_url",
            "image_url": {"url": f"data:image/jpeg;base64,{req.image_base64}"},
        })

        resp = await http.post(
            f"{settings.ocr_online_base_url}/chat/completions",
            headers={"Authorization": f"Bearer {settings.ocr_online_api_key}"},
            json={
                "model": req.model,
                "messages": [{"role": "user", "content": user_content}],
                "temperature": 0.0,
            },
        )
        resp.raise_for_status()
        data = resp.json()
        text = data["choices"][0]["message"]["content"]
        return Result.success({"text": text, "model": req.model})
    except Exception as e:
        logger.error(f"OCR Online(SiliconFlow)失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ========== CV - OCR 接口 ==========
@app.post("/v1/cv/ocr")
async def cv_ocr(image: UploadFile = File(...), lang: str = Form("ch")):
    """OCR识别（DeepSeek-OCR-2 API，通过 SiliconFlow 代理）"""
    img_bytes = await image.read()

    if MOCK_MODE:
        return Result.success({
            "text": "模拟OCR识别文字\n供应商：测试供应商\n商品：测试商品\n数量：100",
            "lines": [
                {"text": "供应商：测试供应商", "bbox": [[10, 10], [200, 10], [200, 30], [10, 30]]},
                {"text": "商品：测试商品", "bbox": [[10, 40], [180, 40], [180, 60], [10, 60]]},
                {"text": "数量：100", "bbox": [[10, 70], [120, 70], [120, 90], [10, 90]]},
            ],
            "confidence": 0.95,
        })

    try:
        # 走 SiliconFlow DeepSeek-OCR-2 chat/completions（base64 image_url）
        img_b64 = base64.b64encode(img_bytes).decode("utf-8")
        user_content = [
            {"type": "text", "text": "请识别图片中的所有文字，按原始布局输出。"},
            {"type": "image_url", "image_url": {"url": f"data:image/jpeg;base64,{img_b64}"}},
        ]
        http = _get_sf_http()
        resp = await http.post(
            f"{settings.ocr_online_base_url}/chat/completions",
            headers={"Authorization": f"Bearer {settings.ocr_online_api_key}"},
            json={
                "model": settings.ocr_online_model,
                "messages": [{"role": "user", "content": user_content}],
                "temperature": 0.0,
            },
        )
        resp.raise_for_status()
        data = resp.json()
        text = data["choices"][0]["message"]["content"]
        # 按行拆分 lines（DeepSeek-OCR-2 不返回 bbox，lines 仅含 text）
        lines = [{"text": line, "bbox": [], "confidence": 0.95} for line in text.split("\n") if line.strip()]
        return Result.success({
            "text": text,
            "lines": lines,
            "confidence": 0.95,
        })
    except Exception as e:
        logger.error(f"OCR失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ========== CV - 目标检测接口 ==========
@app.post("/v1/cv/detect")
async def cv_detect(image: UploadFile = File(...), model: str = Form("yolov10")):
    """目标检测"""
    img_bytes = await image.read()

    if MOCK_MODE:
        return Result.success({
            "detections": [
                {"class": "box", "confidence": 0.92, "bbox": [100, 100, 300, 400]},
                {"class": "person", "confidence": 0.88, "bbox": [400, 50, 550, 450]},
            ]
        })

    try:
        import numpy as np
        import cv2
        yolo = get_yolo_model()
        img_array = np.frombuffer(img_bytes, np.uint8)
        img = cv2.imdecode(img_array, cv2.IMREAD_COLOR)
        results = yolo(img)

        detections = []
        for r in results:
            for box in r.boxes:
                detections.append({
                    "class": yolo.names[int(box.cls)],
                    "confidence": float(box.conf),
                    "bbox": box.xyxy[0].tolist(),
                })
        return Result.success({"detections": detections})
    except Exception as e:
        logger.error(f"目标检测失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ========== CV - 图像分类接口 ==========
@app.post("/v1/cv/classify")
async def cv_classify(image: UploadFile = File(...), candidate_skus: str = Form("")):
    """图像分类（Few-shot识别SKU）"""
    img_bytes = await image.read()

    if MOCK_MODE:
        skus = candidate_skus.split(",") if candidate_skus else ["SKU001", "SKU002"]
        return Result.success({
            "sku": skus[0] if skus else "UNKNOWN",
            "confidence": 0.87,
            "candidates": [{"sku": s, "score": 0.9 - i * 0.1} for i, s in enumerate(skus[:5])],
        })

    try:
        from PIL import Image
        import io

        skus = [s.strip() for s in candidate_skus.split(",") if s.strip()]
        if not skus:
            return Result.success({"sku": "UNKNOWN", "confidence": 0.0, "candidates": []})

        model = get_clip_model()
        img = Image.open(io.BytesIO(img_bytes)).convert("RGB")
        text_emb = model.encode(skus, normalize_embeddings=True)
        img_emb = model.encode(img, normalize_embeddings=True)
        scores = (img_emb @ text_emb.T).squeeze(0)

        candidates = sorted(
            [{"sku": s, "score": round(float(sc), 4)} for s, sc in zip(skus, scores)],
            key=lambda x: x["score"],
            reverse=True,
        )
        best_idx = int(scores.argmax())
        return Result.success({
            "sku": skus[best_idx],
            "confidence": round(float(scores[best_idx]), 4),
            "candidates": candidates,
        })
    except Exception as e:
        logger.error(f"图像分类失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ========== 语音接口 ==========
@app.post("/v1/voice/asr")
async def voice_asr(audio: UploadFile = File(...)):
    """语音识别（XingChenASR-V3.2-Ultra API，通过 SiliconFlow 代理）"""
    audio_bytes = await audio.read()

    if MOCK_MODE:
        return Result.success({"text": "这是Mock语音识别结果", "confidence": 0.95})

    try:
        # 走 SiliconFlow XingChenASR-V3.2-Ultra chat/completions（base64 audio_url）
        audio_b64 = base64.b64encode(audio_bytes).decode("utf-8")
        user_content = [
            {"type": "text", "text": "请识别音频中的所有语音内容，输出纯文本。"},
            {"type": "audio_url", "audio_url": {"url": f"data:audio/wav;base64,{audio_b64}"}},
        ]
        http = _get_sf_http()
        resp = await http.post(
            f"{settings.asr_base_url}/chat/completions",
            headers={"Authorization": f"Bearer {settings.asr_api_key}"},
            json={
                "model": settings.asr_model,
                "messages": [{"role": "user", "content": user_content}],
                "temperature": 0.0,
            },
        )
        resp.raise_for_status()
        data = resp.json()
        text = data["choices"][0]["message"]["content"]
        return Result.success({"text": text, "confidence": 0.95})
    except Exception as e:
        logger.error(f"ASR失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


def _silent_wav() -> bytes:
    """静音WAV（Mock返回/TTS失败降级用）"""
    return bytes([
        0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00,
        0x57, 0x41, 0x56, 0x45, 0x66, 0x6d, 0x74, 0x20,
        0x10, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00,
        0x44, 0xac, 0x00, 0x00, 0x88, 0x58, 0x01, 0x00,
        0x02, 0x00, 0x10, 0x00, 0x64, 0x61, 0x74, 0x61,
        0x00, 0x00, 0x00, 0x00,
    ])


@app.post("/v1/voice/tts")
async def voice_tts(req: TTSRequest):
    """语音合成"""
    if MOCK_MODE:
        return Response(content=_silent_wav(), media_type="audio/wav")

    try:
        client = get_llm_client()
        resp = client.audio.speech.create(
            model=settings.tts_model,
            voice=settings.tts_voice,
            input=req.text,
            response_format=settings.tts_format,
        )
        media_type = "audio/wav" if settings.tts_format == "wav" else "audio/mpeg"
        return Response(content=resp.content, media_type=media_type)
    except Exception as e:
        logger.error(f"TTS失败，降级为静音: {e}")
        return Response(content=_silent_wav(), media_type="audio/wav")


# ========== 时序预测接口 ==========
@app.post("/v1/forecast/predict")
async def forecast_predict(req: ForecastRequest):
    """时序预测"""
    if len(req.series) < 10:
        raise HTTPException(status_code=400, detail="序列数据至少10个点")

    if MOCK_MODE:
        last_val = req.series[-1]
        trend = (req.series[-1] - req.series[0]) / len(req.series)
        forecast = [last_val + trend * (i + 1) for i in range(req.periods)]
        return Result.success({
            "forecast": forecast,
            "lower": [v * 0.9 for v in forecast],
            "upper": [v * 1.1 for v in forecast],
            "model": req.model,
        })

    try:
        import pandas as pd
        from prophet import Prophet

        dates = pd.date_range(end=pd.Timestamp.now(), periods=len(req.series), freq="D")
        df = pd.DataFrame({"ds": dates, "y": req.series})

        model = Prophet(
            yearly_seasonality=True,
            weekly_seasonality=True,
            daily_seasonality=False,
        )
        model.fit(df)

        future = model.make_future_dataframe(periods=req.periods)
        forecast_df = model.predict(future)

        forecast_vals = forecast_df["yhat"].tail(req.periods).tolist()
        lower_vals = forecast_df["yhat_lower"].tail(req.periods).tolist()
        upper_vals = forecast_df["yhat_upper"].tail(req.periods).tolist()

        return Result.success({
            "forecast": forecast_vals,
            "lower": lower_vals,
            "upper": upper_vals,
            "model": req.model,
        })
    except Exception as e:
        logger.error(f"预测失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ========== 异常检测接口 ==========
@app.post("/v1/anomaly/detect")
async def anomaly_detect(req: AnomalyRequest):
    """异常检测"""
    if len(req.data) < 5:
        raise HTTPException(status_code=400, detail="数据至少5个点")

    if MOCK_MODE:
        import numpy as np
        arr = np.array(req.data)
        mean, std = arr.mean(), arr.std()
        anomalies = [abs(x - mean) > 3 * std for x in arr]
        return Result.success({"anomalies": anomalies, "method": "zscore"})

    try:
        import numpy as np
        from pyod.models.iforest import IForest

        X = np.array(req.data).reshape(-1, 1)
        clf = IForest(contamination=0.1)
        clf.fit(X)
        labels = clf.labels_
        return Result.success({
            "anomalies": [bool(l) for l in labels],
            "method": req.method,
        })
    except Exception as e:
        logger.error(f"异常检测失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ========== 健康检查 ==========
@app.get("/health")
async def health():
    return Result.success({
        "status": "ok",
        "mock_mode": MOCK_MODE,
        "models": {
            "llm": "deepseek-v4-pro + glm-5.2(fallback)" if not MOCK_MODE else "mock",
            "embedding": "BAAI/bge-m3(SiliconFlow)" if not MOCK_MODE else "mock",
            "rerank": "BAAI/bge-reranker-v2-m3(SiliconFlow)" if not MOCK_MODE else "mock",
            "ocr_online": "deepseek-ai/DeepSeek-OCR-2(SiliconFlow)" if not MOCK_MODE else "mock",
            "ocr": "loaded" if not MOCK_MODE else "mock",
            "yolo": "loaded" if not MOCK_MODE else "mock",
        },
    })


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
