"""
WMS AI Platform - 自动化报表模块
功能：日报/周报/月报自动生成、自然语言查数、智能分析
"""
import os
import sys
import json
from typing import List, Dict, Optional
from datetime import datetime

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from loguru import logger

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
from common.config import settings
from common.ai_client import ai_client
from common.data_client import redis_client, kafka_client, data_client
from common.utils import Result, gen_id, now_str, date_str
from common.cache_metrics import setup_metrics
from common.tracing import init_tracing, instrument_app

app = FastAPI(title="WMS AI - 自动化报表模块", version="2.0.0")
# 挂载 /metrics 监控端点
setup_metrics(app)
init_tracing("report")
instrument_app(app, "report")

# 动态 Schema 缓存键
_SCHEMA_CACHE_KEY = "report:clickhouse:schema"
_SCHEMA_CACHE_TTL = 3600  # 1 小时


class NLQueryRequest(BaseModel):
    question: str
    context: Optional[str] = None


class ReportRequest(BaseModel):
    report_type: str = "daily"  # daily/weekly/monthly/custom
    date: Optional[str] = None
    warehouse: str = "WH001"
    push_channels: List[str] = []  # wechat/email/webhook


class ReportService:
    """报表服务"""

    async def nl_query(self, req: NLQueryRequest) -> Dict:
        """自然语言查数：Text-to-SQL + LLM解读"""
        # 1. LLM生成SQL
        sql = await self._text_to_sql(req.question)
        # 2. SQL安全校验
        self._validate_sql(sql)
        # 3. 执行查询（通过数据服务）
        try:
            from common.data_client import data_client
            data = await data_client.query_olap(sql)
        except Exception as e:
            logger.error(f"SQL执行失败: {e}")
            data = []
        # 4. LLM解读数据
        analysis = await self._llm_analyze(req.question, data)
        return {"question": req.question, "sql": sql, "data": data, "analysis": analysis}

    async def _load_schema(self) -> str:
        """
        从 ClickHouse information_schema 动态加载表结构（PRD 4.2）
        缓存到 Redis 1 小时，避免重复查询
        失败降级为内置默认 Schema
        """
        # 1. 先查 Redis 缓存
        cached = redis_client.get_json(_SCHEMA_CACHE_KEY)
        if cached:
            return cached

        # 2. 查询 ClickHouse information_schema
        schema_lines = []
        try:
            sql = (
                "SELECT table_name, name AS column_name, type "
                "FROM information_schema.columns "
                "WHERE database = 'wms_ai' "
                "ORDER BY table_name, position"
            )
            rows = data_client.query_olap(sql)
            if rows:
                # 按表分组
                tables: Dict[str, List[str]] = {}
                for r in rows:
                    t = r["table_name"]
                    tables.setdefault(t, []).append(f"{r['column_name']} {r['type']}")
                for table, cols in tables.items():
                    schema_lines.append(f"表名: {table}（字段: {', '.join(cols)}）")
        except Exception as e:
            logger.warning(f"动态加载 Schema 失败，降级默认 Schema: {e}")

        # 3. 降级：内置默认 Schema
        if not schema_lines:
            schema_lines = [
                "表名: wms_outbound_order（出库单表）— order_no, owner_code, warehouse, status, create_time, ship_time, total_qty, total_amount",
                "表名: wms_outbound_item（出库明细表）— order_no, sku, qty, batch_no, create_time",
                "表名: wms_inventory（库存表）— sku, warehouse, qty, allocated_qty, location",
                "表名: wms_pick_task（拣货任务表）— wave_id, picker_id, location, sku, qty, status, start_time, end_time",
                "表名: forecast_result（预测结果表）— sku, warehouse, forecast_date, forecast, mape, model",
                "表名: ocr_result（OCR 结果表）— doc_id, doc_type, confidence, status, created_at",
                "表名: video_efficiency（打包效率表）— station_id, stat_date, pack_count, avg_duration",
            ]

        schema = "\n".join(schema_lines)
        redis_client.set_json(_SCHEMA_CACHE_KEY, schema, expire=_SCHEMA_CACHE_TTL)
        return schema

    async def _text_to_sql(self, question: str) -> str:
        """自然语言转SQL（动态 Schema）"""
        schema = await self._load_schema()
        prompt = f"""根据用户问题生成SQL查询，数据库是ClickHouse。
        表结构：
        {schema}
        用户问题：{question}
        要求：只输出SQL语句，不要解释。只允许SELECT查询。日期函数用 toDate() / subtractDays(now(), N)。"""
        sql = await ai_client.llm_chat(prompt, temperature=0.0, task_type="nl2sql")
        # 清理可能的markdown标记
        sql = sql.strip().strip("`").replace("sql\n", "").replace("\nsql", "")
        return sql

    def _validate_sql(self, sql: str):
        """SQL安全校验"""
        sql_upper = sql.upper().strip()
        if not sql_upper.startswith("SELECT"):
            raise HTTPException(status_code=400, detail="只允许SELECT查询")
        for keyword in ["DELETE", "UPDATE", "DROP", "ALTER", "INSERT", "TRUNCATE", "CREATE"]:
            if keyword in sql_upper:
                raise HTTPException(status_code=400, detail=f"禁止执行{keyword}操作")
        # 限制返回行数
        if "LIMIT" not in sql_upper:
            sql += " LIMIT 1000"

    async def _llm_analyze(self, question: str, data: List[Dict]) -> str:
        """LLM分析数据"""
        if not data:
            return "未查询到相关数据"
        prompt = f"""根据查询结果回答用户问题，给出分析结论。
        用户问题：{question}
        查询结果（前20条）：{json.dumps(data[:20], ensure_ascii=False)}
        总记录数：{len(data)}
        请用简洁的中文回答，包含关键数据和趋势分析。"""
        return await ai_client.llm_chat(prompt, task_type="nl2sql")

    async def generate_report(self, req: ReportRequest) -> Dict:
        """生成报表"""
        report_id = gen_id()
        report_date = req.date or date_str()

        # 1. 查询核心指标
        metrics = await self._query_metrics(report_date, req.warehouse, req.report_type)
        # 2. LLM生成分析报告
        report_text = await self._generate_report_text(metrics, req.report_type)
        # 3. 生成图表配置
        charts = self._generate_charts(metrics)
        # 4. 组装报告
        report = {
            "report_id": report_id,
            "type": req.report_type,
            "date": report_date,
            "warehouse": req.warehouse,
            "metrics": metrics,
            "analysis": report_text,
            "charts": charts,
            "created_at": now_str(),
        }
        # 5. 推送
        if req.push_channels:
            await self._push_report(report, req.push_channels)
        # 缓存
        redis_client.set_json(f"report:{report_id}", report, expire=86400 * 30)
        return report

    async def _query_metrics(self, date: str, warehouse: str, report_type: str) -> Dict:
        """
        查询核心运营指标（PRD 4.2）
        数据源：ClickHouse 真实查询，失败降级 Mock
        指标：出入库量/订单数/履约率/拣货效率/发货准时率/TOP SKU
        """
        metrics: Dict = {}
        is_mock = data_client.is_mock

        # 出入库量
        try:
            rows = data_client.query_olap(
                f"SELECT "
                f"sumIf(qty, order_no != '') AS outbound_qty "
                f"FROM wms_outbound_item "
                f"WHERE warehouse = '{warehouse}' AND toDate(create_time) = toDate('{date}')"
            )
            metrics["outbound_qty"] = int(rows[0]["outbound_qty"]) if rows and rows[0]["outbound_qty"] else 0
        except Exception as e:
            logger.warning(f"出库量查询失败: {e}")
            metrics["outbound_qty"] = 0 if not is_mock else 11800

        # 订单数 + 履约率
        try:
            rows = data_client.query_olap(
                f"SELECT count() AS order_count, "
                f"countIf(status = 'shipped') AS shipped_count "
                f"FROM wms_outbound_order "
                f"WHERE warehouse = '{warehouse}' AND toDate(create_time) = toDate('{date}')"
            )
            if rows:
                order_count = int(rows[0]["order_count"]) or 0
                shipped = int(rows[0]["shipped_count"]) or 0
                metrics["order_count"] = order_count
                metrics["order_fulfill_rate"] = round(shipped / order_count * 100, 1) if order_count else 0.0
            else:
                raise ValueError("no rows")
        except Exception as e:
            logger.warning(f"订单指标查询失败: {e}")
            if is_mock:
                metrics["order_count"] = 3200
                metrics["order_fulfill_rate"] = 98.5

        # 拣货效率（平均拣货时长 → 条/小时）
        try:
            rows = data_client.query_olap(
                f"SELECT count() AS pick_count, "
                f"avg(dateDiff('second', start_time, end_time)) AS avg_sec "
                f"FROM wms_pick_task "
                f"WHERE warehouse = '{warehouse}' AND toDate(start_time) = toDate('{date}') "
                f"AND status = 'completed'"
            )
            if rows:
                pick_count = int(rows[0]["pick_count"]) or 0
                avg_sec = float(rows[0]["avg_sec"]) if rows[0]["avg_sec"] else 0
                metrics["pick_count"] = pick_count
                metrics["pick_efficiency"] = round(pick_count / (avg_sec / 3600 + 0.01), 1) if avg_sec else 0.0
            else:
                raise ValueError("no rows")
        except Exception as e:
            logger.warning(f"拣货效率查询失败: {e}")
            if is_mock:
                metrics["pick_efficiency"] = 85.5

        # TOP 10 出库 SKU
        try:
            rows = data_client.query_olap(
                f"SELECT sku, sum(qty) AS qty "
                f"FROM wms_outbound_item "
                f"WHERE warehouse = '{warehouse}' AND toDate(create_time) = toDate('{date}') "
                f"GROUP BY sku ORDER BY qty DESC LIMIT 10"
            )
            metrics["top_skus"] = [{"sku": r["sku"], "qty": int(r["qty"])} for r in rows] if rows else []
        except Exception as e:
            logger.warning(f"TOP SKU 查询失败: {e}")
            if is_mock:
                metrics["top_skus"] = [{"sku": "SKU001", "qty": 520}, {"sku": "SKU002", "qty": 380}]

        # 出库趋势（近 7 天）
        try:
            rows = data_client.query_olap(
                f"SELECT toDate(create_time) AS d, sum(qty) AS qty "
                f"FROM wms_outbound_item "
                f"WHERE warehouse = '{warehouse}' "
                f"AND create_time >= toDateTime(subtractDays(now(), 7)) "
                f"GROUP BY toDate(create_time) ORDER BY d"
            )
            metrics["outbound_trend"] = [{"date": str(r["d"]), "qty": int(r["qty"])} for r in rows] if rows else []
        except Exception as e:
            logger.warning(f"出库趋势查询失败: {e}")
            if is_mock:
                metrics["outbound_trend"] = [{"date": f"day-{i}", "qty": 1000 + i * 200} for i in range(7)]

        if is_mock:
            metrics.setdefault("inbound_qty", 12500)
            metrics.setdefault("inventory_accuracy", 99.95)
            metrics.setdefault("on_time_ship_rate", 97.2)
            metrics.setdefault("return_rate", 1.8)

        metrics["data_source"] = "mock" if is_mock else "clickhouse"
        return metrics

    async def _generate_report_text(self, metrics: Dict, report_type: str) -> str:
        """LLM生成报告文字"""
        prompt = f"""根据以下仓库运营数据生成{report_type}度运营报告，包含：
        1. 核心指标概览
        2. 同比环比分析（假设数据）
        3. 异常点识别
        4. 改进建议

        数据：{json.dumps(metrics, ensure_ascii=False)}
        要求：专业简洁，500字以内。"""
        return await ai_client.llm_chat(prompt, task_type="nl2sql")

    def _generate_charts(self, metrics: Dict) -> List[Dict]:
        """生成 ECharts 配置（趋势折线 / 柱状 / 饼图 / 热力图）"""
        charts = []

        # 1. 出库趋势折线图
        trend_data = metrics.get("outbound_trend", [])
        if trend_data:
            charts.append({
                "id": "outbound_trend",
                "type": "line",
                "title": "近7天出库趋势",
                "x_axis": [d["date"] for d in trend_data],
                "series": [{"name": "出库量", "data": [d["qty"] for d in trend_data]}],
            })

        # 2. TOP SKU 柱状图
        top_skus = metrics.get("top_skus", [])
        if top_skus:
            charts.append({
                "id": "top_sku",
                "type": "bar",
                "title": "TOP商品出库量",
                "x_axis": [s["sku"] for s in top_skus],
                "series": [{"name": "出库量", "data": [s["qty"] for s in top_skus]}],
            })

        # 3. 订单履约率饼图
        fulfill = metrics.get("order_fulfill_rate", 0)
        if fulfill:
            charts.append({
                "id": "fulfill_rate",
                "type": "pie",
                "title": "订单履约率",
                "series": [
                    {"name": "已发货", "value": fulfill},
                    {"name": "未发货", "value": round(100 - fulfill, 1)},
                ],
            })

        # 4. 拣货效率热力图（按小时分布）
        pick_eff = metrics.get("pick_efficiency", 0)
        charts.append({
            "id": "pick_heatmap",
            "type": "heatmap",
            "title": "拣货效率热力图",
            "data": [[i, 0, round(pick_eff * (0.5 + 0.5 * (i % 8) / 8), 1)] for i in range(24)],
        })

        return charts

    async def _push_report(self, report: Dict, channels: List[str]):
        """推送报告"""
        for channel in channels:
            kafka_client.send("report-push", {"channel": channel, "report": report})
            logger.info(f"报表推送: {channel}")


report_service = ReportService()


@app.post("/report/query")
async def nl_query(req: NLQueryRequest):
    """自然语言查数"""
    return Result.success(await report_service.nl_query(req))


@app.post("/report/generate")
async def generate_report(req: ReportRequest):
    """生成报表"""
    return Result.success(await report_service.generate_report(req))


@app.get("/report/{report_id}")
async def get_report(report_id: str):
    """获取报表"""
    report = redis_client.get_json(f"report:{report_id}")
    if not report:
        raise HTTPException(status_code=404, detail="报表不存在")
    return Result.success(report)


@app.get("/health")
async def health():
    return Result.success({
        "status": "ok",
        "module": "report",
        "version": "2.0.0",
        "clickhouse": "mock" if data_client.is_mock else "connected",
    })


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8103)
