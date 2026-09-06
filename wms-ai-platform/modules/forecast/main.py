"""
WMS AI Platform - 需求预测模块
功能：SKU级出库量预测，指导补货/波次/排班/大促备货
模型：Prophet（默认）
"""
import os
import sys
import io
import json
import time
import pickle
import hashlib
from typing import List, Dict, Optional, Any
from datetime import datetime, timedelta
from enum import Enum

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from loguru import logger
import pandas as pd
import numpy as np

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
from common.config import settings
from common.ai_client import ai_client
from common.data_client import redis_client, kafka_client, minio_client, data_client
from common.utils import Result, gen_id, now_str, date_str
from common.cache_metrics import setup_metrics
from common.tracing import init_tracing, instrument_app

app = FastAPI(title="WMS AI - 需求预测模块", version="2.0.0")
setup_metrics(app)
init_tracing("forecast")
instrument_app(app, "forecast")

# MinIO 模型存储前缀（替代本地 MODEL_DIR）
MODEL_MINIO_PREFIX = "models/forecast"


# ========== 数据模型 ==========
class ForecastRequest(BaseModel):
    sku: str
    warehouse: str = "WH001"
    owner: str = "default"
    periods: int = 30
    model: str = "prophet"


class ForecastResult(BaseModel):
    sku: str
    warehouse: str
    forecast_date: str
    periods: int
    forecast: List[float]
    lower: List[float]
    upper: List[float]
    mape: float = 0.0
    model: str
    created_at: str


class ReplenishmentRequest(BaseModel):
    sku: str
    warehouse: str = "WH001"
    current_stock: float
    lead_time_days: int = 3
    service_level: float = 0.95


class TrainRequest(BaseModel):
    sku: str
    warehouse: str = "WH001"
    owner: str = "default"
    history_days: int = 365
    model: str = "prophet"


# ========== 核心服务 ==========
class ForecastService:
    """需求预测服务"""

    def _get_model_object(self, sku: str, warehouse: str, model: str,
                          version: str = "latest") -> str:
        """MinIO 对象名（支持版本回滚）"""
        return f"{MODEL_MINIO_PREFIX}/{warehouse}_{sku}_{model}_{version}.pkl"

    def _upload_model_to_minio(self, obj_name: str, model_obj) -> bool:
        """上传模型到 MinIO（本地内存序列化 → MinIO）"""
        try:
            buf = pickle.dumps(model_obj)
            minio_client.upload_bytes(obj_name, buf)
            logger.info(f"模型已上传 MinIO: {obj_name} ({len(buf)} bytes)")
            return True
        except Exception as e:
            logger.warning(f"MinIO 模型上传失败（降级本地）: {e}")
            return False

    def _download_model_from_minio(self, obj_name: str):
        """从 MinIO 下载模型；失败返回 None"""
        try:
            buf = minio_client.get_bytes(obj_name)
            if buf is None:
                return None
            return pickle.loads(buf)
        except Exception as e:
            logger.warning(f"MinIO 模型下载失败: {e}")
            return None

    async def train(self, req: TrainRequest) -> Dict:
        """训练预测模型"""
        start = time.time()
        version = datetime.now().strftime("%Y%m%d%H%M%S")
        obj_latest = self._get_model_object(req.sku, req.warehouse, req.model, "latest")
        obj_versioned = self._get_model_object(req.sku, req.warehouse, req.model, version)

        # 1. 获取历史出库数据（ClickHouse 真实查询，失败降级 Mock）
        history = await self._get_history_data(req.sku, req.warehouse, req.owner, req.history_days)
        if len(history) < 30:
            raise HTTPException(status_code=400, detail=f"历史数据不足，仅{len(history)}条，至少需要30条")

        # 2. 训练模型
        if req.model == "prophet":
            model, metrics = self._train_prophet(history)
        else:
            raise HTTPException(status_code=400, detail=f"不支持的模型: {req.model}")

        # 3. 保存模型到 MinIO（latest + 版本号双写，支持回滚）
        self._upload_model_to_minio(obj_latest, model)
        self._upload_model_to_minio(obj_versioned, model)

        # 4. 缓存模型元数据
        redis_client.set_json(f"forecast:model:{req.warehouse}:{req.sku}", {
            "model": req.model,
            "mape": metrics.get("mape", 0),
            "trained_at": now_str(),
            "data_points": len(history),
            "version": version,
            "minio_object": obj_latest,
        }, expire=86400 * 30)

        cost = (time.time() - start) * 1000
        logger.info(f"模型训练完成: sku={req.sku}, model={req.model}, MAPE={metrics.get('mape', 0):.4f}, version={version}, 耗时={cost:.0f}ms")
        return {"sku": req.sku, "model": req.model, "metrics": metrics, "version": version, "trained_at": now_str()}

    def _train_prophet(self, history: List[Dict]) -> tuple:
        """训练Prophet模型"""
        from prophet import Prophet
        df = pd.DataFrame(history)
        df["ds"] = pd.to_datetime(df["date"])
        df["y"] = df["qty"]

        model = Prophet(
            yearly_seasonality=True,
            weekly_seasonality=True,
            daily_seasonality=False,
            changepoint_prior_scale=0.05,
            seasonality_prior_scale=10.0,
        )
        model.fit(df)

        # 计算MAPE（用最后30天做验证）
        if len(df) > 60:
            train_df = df.iloc[:-30]
            test_df = df.iloc[-30:]
            m = Prophet(yearly_seasonality=True, weekly_seasonality=True, daily_seasonality=False)
            m.fit(train_df)
            future = m.make_future_dataframe(periods=30)
            forecast = m.predict(future)
            pred = forecast["yhat"].tail(30).values
            actual = test_df["y"].values
            mape = np.mean(np.abs((actual - pred) / (actual + 1))) * 100
        else:
            mape = 15.0  # 默认值

        return model, {"mape": float(mape)}

    async def predict(self, req: ForecastRequest) -> ForecastResult:
        """预测未来出库量"""
        obj_name = self._get_model_object(req.sku, req.warehouse, req.model, "latest")
        meta = redis_client.get_json(f"forecast:model:{req.warehouse}:{req.sku}") or {}
        obj_name = meta.get("minio_object", obj_name)

        # 1. 从 MinIO 加载模型，不存在则先训练
        model = self._download_model_from_minio(obj_name)
        if model is None:
            await self.train(TrainRequest(
                sku=req.sku, warehouse=req.warehouse, model=req.model
            ))
            model = self._download_model_from_minio(obj_name)
            if model is None:
                raise HTTPException(status_code=500, detail="模型训练后仍无法加载，请检查 MinIO")

        # 3. 预测
        if req.model == "prophet":
            forecast_vals, lower_vals, upper_vals = self._predict_prophet(model, req.periods)
        else:
            forecast_vals, lower_vals, upper_vals = [], [], []

        # 4. 获取模型元数据（已在上方预取 meta）

        result = ForecastResult(
            sku=req.sku,
            warehouse=req.warehouse,
            forecast_date=date_str(),
            periods=req.periods,
            forecast=[round(v, 2) for v in forecast_vals],
            lower=[round(v, 2) for v in lower_vals],
            upper=[round(v, 2) for v in upper_vals],
            mape=meta.get("mape", 0),
            model=req.model,
            created_at=now_str(),
        )

        # 缓存预测结果
        redis_client.set_json(f"forecast:result:{req.warehouse}:{req.sku}", result.model_dump(), expire=86400)
        return result

    def _predict_prophet(self, model, periods: int) -> tuple:
        """Prophet预测"""
        future = model.make_future_dataframe(periods=periods)
        forecast = model.predict(future)
        forecast_vals = forecast["yhat"].tail(periods).tolist()
        lower_vals = forecast["yhat_lower"].tail(periods).tolist()
        upper_vals = forecast["yhat_upper"].tail(periods).tolist()
        # 预测值不能为负
        forecast_vals = [max(0, v) for v in forecast_vals]
        lower_vals = [max(0, v) for v in lower_vals]
        return forecast_vals, lower_vals, upper_vals

    async def calc_replenishment(self, req: ReplenishmentRequest) -> Dict:
        """计算补货建议"""
        # 1. 获取预测结果
        forecast = await self.predict(ForecastRequest(
            sku=req.sku, warehouse=req.warehouse, periods=30
        ))

        # 2. 补货提前期内预测出库量
        lead_demand = sum(forecast.forecast[:req.lead_time_days])

        # 3. 安全库存 = 预测标准差 × 服务水平系数
        std = np.std(forecast.forecast) if forecast.forecast else 0
        # 服务水平系数：95%=1.65, 98%=2.05, 99%=2.33
        z_score = {0.90: 1.28, 0.95: 1.65, 0.98: 2.05, 0.99: 2.33}.get(req.service_level, 1.65)
        safety_stock = std * z_score

        # 4. 补货点
        reorder_point = lead_demand + safety_stock

        # 5. 建议补货量 = 未来30天预测 - 当前库存 + 安全库存
        total_forecast = sum(forecast.forecast)
        suggest_qty = max(0, total_forecast - req.current_stock + safety_stock)

        # 6. 是否需要补货
        need_replenish = req.current_stock <= reorder_point

        return {
            "sku": req.sku,
            "warehouse": req.warehouse,
            "current_stock": req.current_stock,
            "lead_time_days": req.lead_time_days,
            "lead_demand": round(lead_demand, 2),
            "safety_stock": round(safety_stock, 2),
            "reorder_point": round(reorder_point, 2),
            "suggest_qty": round(suggest_qty, 2),
            "need_replenish": need_replenish,
            "forecast_30d": round(total_forecast, 2),
            "service_level": req.service_level,
        }

    async def _get_history_data(self, sku: str, warehouse: str, owner: str, days: int) -> List[Dict]:
        """
        获取历史出库数据
        数据源：ClickHouse wms_outbound_item 按日聚合（PRD 4.1）
        失败降级 Mock，保证训练流程不中断
        """
        # 先尝试 ClickHouse 真实查询
        try:
            end_date = datetime.now()
            start_date = end_date - timedelta(days=days)
            sql = (
                "SELECT toDate(create_time) AS date, sum(qty) AS qty "
                "FROM wms_outbound_item "
                f"WHERE sku = '{sku}' AND warehouse = '{warehouse}' "
                f"AND create_time >= toDateTime('{start_date.strftime('%Y-%m-%d')}') "
                "GROUP BY toDate(create_time) "
                "ORDER BY date"
            )
            rows = data_client.query_olap(sql)
            if rows:
                return [{"date": r["date"].strftime("%Y-%m-%d") if hasattr(r["date"], "strftime") else str(r["date"]),
                         "qty": int(r["qty"])} for r in rows]
        except Exception as e:
            logger.warning(f"ClickHouse 历史出库查询失败，降级 Mock: {e}")

        # 降级：生成带趋势和季节性的可重现模拟数据
        end_date = datetime.now()
        dates = pd.date_range(end=end_date, periods=days, freq="D")
        # 修复 Bug: hash(sku) 每次重启加盐不可重现 → 改用 hashlib.md5
        seed = int(hashlib.md5(sku.encode()).hexdigest(), 16) % 2**32
        rng = np.random.RandomState(seed)
        base = 50 + rng.randint(0, 100)
        trend = np.linspace(0, 20, days)
        weekly = 10 * np.sin(2 * np.pi * np.arange(days) / 7)
        yearly = 15 * np.sin(2 * np.pi * np.arange(days) / 365)
        noise = rng.normal(0, 5, days)
        qty = np.maximum(0, base + trend + weekly + yearly + noise).astype(int)

        return [{"date": d.strftime("%Y-%m-%d"), "qty": int(q)} for d, q in zip(dates, qty)]

    async def batch_train(self, skus: List[str], warehouse: str = "WH001") -> Dict:
        """批量训练模型"""
        results = []
        for sku in skus:
            try:
                result = await self.train(TrainRequest(sku=sku, warehouse=warehouse))
                results.append({"sku": sku, "status": "success", "mape": result["metrics"]["mape"]})
            except Exception as e:
                results.append({"sku": sku, "status": "failed", "error": str(e)})
        return {"total": len(skus), "results": results}

    async def daily_forecast_job(self):
        """
        每日预测定时任务（凌晨 2 点由 scheduler 触发）
        1. 从 ClickHouse 查询所有活跃 SKU 列表
        2. 批量训练 + 预测
        3. 结果写入 ClickHouse forecast_result 表
        """
        logger.info("开始每日预测任务")
        # 1. 获取活跃 SKU 列表
        try:
            skus = await self._get_active_skus()
        except Exception as e:
            logger.error(f"获取活跃 SKU 列表失败，每日预测任务终止: {e}")
            return

        if not skus:
            logger.warning("无活跃 SKU，每日预测任务跳过")
            return

        logger.info(f"待预测 SKU 数: {len(skus)}")
        success, failed = 0, 0

        # 2. 批量训练 + 预测
        results_to_write = []
        for sku, warehouse in skus:
            try:
                result = await self.predict(ForecastRequest(
                    sku=sku, warehouse=warehouse, periods=30
                ))
                # 3. 收集结果写入 ClickHouse
                results_to_write.append(result)
                success += 1
            except Exception as e:
                logger.warning(f"SKU={sku} 预测失败: {e}")
                failed += 1

        # 3. 批量写入 ClickHouse forecast_result
        if results_to_write:
            try:
                await self._write_results_to_clickhouse(results_to_write)
                logger.info(f"预测结果已写入 ClickHouse: {len(results_to_write)} 条")
            except Exception as e:
                logger.error(f"预测结果写 ClickHouse 失败: {e}")

        logger.info(f"每日预测任务完成: 成功 {success}, 失败 {failed}")

    async def _get_active_skus(self) -> List[tuple]:
        """从 ClickHouse 查询近 30 天有出库记录的活跃 SKU"""
        sql = (
            "SELECT sku, warehouse FROM wms_outbound_item "
            "WHERE create_time >= toDateTime(subtractDays(now(), 30)) "
            "GROUP BY sku, warehouse HAVING sum(qty) > 0"
        )
        rows = data_client.query_olap(sql)
        return [(r["sku"], r["warehouse"]) for r in rows]

    async def _write_results_to_clickhouse(self, results: List[ForecastResult]):
        """将预测结果批量写入 ClickHouse forecast_result 表"""
        rows = []
        now = now_str()
        for r in results:
            rows.append((
                r.sku, r.warehouse, r.forecast_date, r.periods,
                str(r.forecast), str(r.lower), str(r.upper),
                r.mape, r.model, now,
            ))
        sql = (
            "INSERT INTO forecast_result "
            "(sku, warehouse, forecast_date, periods, forecast, lower, upper, mape, model, created_at) "
            "VALUES"
        )
        data_client.insert_olap(sql, rows)


forecast_service = ForecastService()


# ========== API接口 ==========
@app.post("/forecast/train")
async def train_model(req: TrainRequest):
    """训练预测模型"""
    return Result.success(await forecast_service.train(req))


@app.post("/forecast/predict")
async def predict(req: ForecastRequest):
    """预测未来出库量"""
    return Result.success((await forecast_service.predict(req)).model_dump())


@app.post("/forecast/replenishment")
async def replenishment(req: ReplenishmentRequest):
    """计算补货建议"""
    return Result.success(await forecast_service.calc_replenishment(req))


@app.post("/forecast/batch-train")
async def batch_train(skus: List[str], warehouse: str = "WH001"):
    """批量训练"""
    return Result.success(await forecast_service.batch_train(skus, warehouse))


@app.get("/forecast/result/{warehouse}/{sku}")
async def get_cached_result(warehouse: str, sku: str):
    """获取缓存的预测结果"""
    result = redis_client.get_json(f"forecast:result:{warehouse}:{sku}")
    if not result:
        raise HTTPException(status_code=404, detail="预测结果不存在，请先调用预测接口")
    return Result.success(result)


@app.get("/forecast/model/{warehouse}/{sku}")
async def get_model_info(warehouse: str, sku: str):
    """获取模型信息"""
    meta = redis_client.get_json(f"forecast:model:{warehouse}:{sku}")
    if not meta:
        raise HTTPException(status_code=404, detail="模型未训练")
    return Result.success(meta)


@app.post("/forecast/daily-job")
async def daily_job():
    """每日预测定时任务入口（scheduler 调用）"""
    await forecast_service.daily_forecast_job()
    return Result.success({"status": "completed"})


@app.get("/health")
async def health():
    return Result.success({
        "status": "ok",
        "module": "forecast",
        "version": "2.0.0",
        "minio": "mock" if minio_client.is_mock else "connected",
        "clickhouse": "mock" if data_client.is_mock else "connected",
    })


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8102)
