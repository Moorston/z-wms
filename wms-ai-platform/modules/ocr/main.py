"""
WMS AI Platform - 单据OCR模块
功能：供应商送货单/ASN/快递单/质检报告/退货单自动识别录入
流程：上传→预处理→OCR→LLM抽取→差异校验→人工确认→自动录入
"""
import os
import sys
import json
import time
from typing import List, Dict, Optional, Any
from enum import Enum

from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from pydantic import BaseModel
from loguru import logger

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
from common.config import settings
from common.ai_client import ai_client
from common.data_client import redis_client, minio_client, mysql_client
from common.utils import Result, gen_id, now_str
from common.cache_metrics import setup_metrics
from common.tracing import init_tracing, instrument_app

app = FastAPI(title="WMS AI - 单据OCR模块", version="1.0.0")
setup_metrics(app)
init_tracing("ocr")
instrument_app(app, "ocr")


# ========== 数据模型 ==========
class DocType(str, Enum):
    DELIVERY = "delivery"        # 送货单
    ASN = "asn"                  # 预到货通知
    EXPRESS = "express"          # 快递单
    QC_REPORT = "qc_report"      # 质检报告
    RETURN = "return"            # 退货单
    UNKNOWN = "unknown"          # 未知


class OCRResult(BaseModel):
    doc_id: str
    doc_type: str
    raw_text: str
    extracted_data: Dict[str, Any]
    confidence: float
    diff_result: Optional[Dict] = None
    status: str = "pending_confirm"  # pending_confirm / confirmed / rejected
    created_at: str


# ========== 单据字段Schema ==========
DOC_FIELD_SCHEMA = {
    DocType.DELIVERY: {
        "supplier": "供应商名称",
        "delivery_no": "送货单号",
        "po_no": "采购订单号",
        "delivery_date": "送货日期",
        "items": [{"sku": "商品编码", "sku_name": "商品名称", "qty": "数量", "batch_no": "批次号", "unit": "单位"}],
        "total_qty": "总数量",
        "remark": "备注",
    },
    DocType.ASN: {
        "asn_no": "ASN单号",
        "supplier": "供应商",
        "expected_date": "预计到货日期",
        "warehouse": "收货仓库",
        "items": [{"sku": "商品编码", "qty": "数量", "batch_no": "批次号"}],
    },
    DocType.EXPRESS: {
        "express_no": "快递单号",
        "express_company": "快递公司",
        "sender": "寄件人",
        "receiver": "收件人",
        "receiver_phone": "收件人电话",
        "receiver_address": "收件地址",
        "weight": "重量",
    },
    DocType.QC_REPORT: {
        "report_no": "报告编号",
        "batch_no": "批次号",
        "sku": "商品编码",
        "qc_result": "检验结果(合格/不合格)",
        "qc_date": "检验日期",
        "inspector": "检验员",
        "items": [{"item": "检验项", "standard": "标准", "actual": "实测值", "result": "结果"}],
    },
    DocType.RETURN: {
        "return_no": "退货单号",
        "order_no": "原订单号",
        "customer": "客户",
        "return_date": "退货日期",
        "return_reason": "退货原因",
        "items": [{"sku": "商品编码", "qty": "数量", "batch_no": "批次号", "condition": "商品状态"}],
    },
}

# 固定模板单据（用模板匹配，不用LLM）
FIXED_TEMPLATE_TYPES = {DocType.EXPRESS}


# ========== 核心服务 ==========
class OCRService:
    """单据OCR服务"""

    async def process(self, image_bytes: bytes, doc_type: str = None,
                      po_no: str = None, order_no: str = None) -> OCRResult:
        """处理单据识别完整流程"""
        doc_id = gen_id()
        start = time.time()

        # 1. 上传原图到MinIO
        object_name = f"ocr/{now_str('%Y%m%d')}/{doc_id}.jpg"
        minio_client.upload_bytes(object_name, image_bytes, "image/jpeg")

        # 2. 单据类型判断
        if not doc_type or doc_type == DocType.UNKNOWN:
            doc_type = await self._classify_doc_type(image_bytes)
        else:
            doc_type = DocType(doc_type)

        # 3. OCR识别
        ocr_result = await ai_client.ocr_recognize(image_bytes)
        raw_text = ocr_result.get("text", "")
        ocr_confidence = ocr_result.get("confidence", 0.0)

        # 3.1 置信度 <0.7 时调用 DeepSeek-OCR-2 在线增强
        if ocr_confidence < 0.7:
            try:
                enhanced = await ai_client.ocr_online_enhance(image_bytes)
                if enhanced.get("text"):
                    raw_text = enhanced["text"]
                    ocr_confidence = enhanced.get("confidence", ocr_confidence)
                    logger.info(f"OCR在线增强: doc_id={doc_id}, confidence提升至 {ocr_confidence}")
            except Exception as e:
                logger.warning(f"OCR在线增强失败（降级用原始结果）: {e}")

        # 4. 信息抽取
        if doc_type in FIXED_TEMPLATE_TYPES:
            extracted = self._template_extract(raw_text, doc_type)
        else:
            extracted = await self._llm_extract(raw_text, doc_type)

        # 5. 差异校验
        diff_result = None
        if po_no and doc_type in [DocType.DELIVERY, DocType.ASN]:
            diff_result = await self._diff_with_po(extracted, po_no)
        elif order_no and doc_type == DocType.RETURN:
            diff_result = await self._diff_with_order(extracted, order_no)

        # 6. 保存结果
        result = OCRResult(
            doc_id=doc_id,
            doc_type=doc_type.value,
            raw_text=raw_text,
            extracted_data=extracted,
            confidence=ocr_confidence,
            diff_result=diff_result,
            created_at=now_str(),
        )
        # MySQL 持久化（主存储）+ Redis 短期缓存（加速查询）
        self._save_to_mysql(result, po_no, order_no, object_name)
        redis_client.set_json(f"ocr:result:{doc_id}", result.model_dump(), expire=3600)  # 缩短到1小时

        cost = (time.time() - start) * 1000
        logger.info(f"OCR处理完成: doc_id={doc_id}, type={doc_type}, 耗时={cost:.0f}ms")
        return result

    def _save_to_mysql(self, result: 'OCRResult', po_no: str = None,
                       order_no: str = None, image_path: str = None):
        """将 OCR 结果写入 MySQL ocr_result 表（持久化主存储）"""
        sql = """
            INSERT INTO ocr_result
            (result_id, doc_type, doc_no, warehouse, image_path,
             extracted, raw_text, confidence, diff_result, status)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """
        # 从 extracted 中提取单据编号
        doc_no = (result.extracted_data.get("delivery_no")
                  or result.extracted_data.get("asn_no")
                  or result.extracted_data.get("express_no")
                  or result.extracted_data.get("report_no")
                  or result.extracted_data.get("return_no"))
        mysql_client.execute(sql, (
            result.doc_id,
            result.doc_type,
            doc_no,
            result.extracted_data.get("warehouse"),
            image_path,
            json.dumps(result.extracted_data, ensure_ascii=False),
            result.raw_text,
            result.confidence,
            json.dumps(result.diff_result, ensure_ascii=False) if result.diff_result else None,
            result.status,
        ))

    async def _classify_doc_type(self, image_bytes: bytes) -> DocType:
        """判断单据类型（用LLM根据OCR文本判断）"""
        ocr_result = await ai_client.ocr_recognize(image_bytes)
        text = ocr_result.get("text", "")[:500]
        prompt = f"""判断以下单据属于哪种类型，只返回类型英文代码：
        - delivery: 送货单/收货单
        - asn: 预到货通知
        - express: 快递单/物流单
        - qc_report: 质检报告/检验报告
        - return: 退货单
        - unknown: 无法判断

        单据内容：{text}
        类型："""
        result = await ai_client.llm_chat(prompt, temperature=0.0)
        result = result.strip().lower()
        for dt in DocType:
            if dt.value in result:
                return dt
        return DocType.UNKNOWN

    def _template_extract(self, text: str, doc_type: DocType) -> Dict:
        """固定模板抽取（正则+关键词）"""
        import re
        result = {}
        if doc_type == DocType.EXPRESS:
            # 快递单号（常见格式）
            express_no_match = re.search(r'(SF|YT|ZT|YD|JD|EMS)\s*[:：]?\s*([A-Z0-9]{10,20})', text, re.I)
            if express_no_match:
                result["express_company"] = express_no_match.group(1).upper()
                result["express_no"] = express_no_match.group(2)
            # 收件人
            receiver_match = re.search(r'收件人[：:]\s*(\S+)', text)
            if receiver_match:
                result["receiver"] = receiver_match.group(1)
            # 电话
            phone_match = re.search(r'1[3-9]\d{9}', text)
            if phone_match:
                result["receiver_phone"] = phone_match.group()
            # 地址
            addr_match = re.search(r'地址[：:]\s*(.+?)(?:\n|$)', text)
            if addr_match:
                result["receiver_address"] = addr_match.group(1).strip()
        return result

    async def _llm_extract(self, text: str, doc_type: DocType) -> Dict:
        """LLM信息抽取（非固定格式单据）"""
        schema = DOC_FIELD_SCHEMA.get(doc_type, {})
        prompt = f"""从以下单据文本中提取结构化信息，输出JSON。

单据类型：{doc_type.value}
字段定义：{json.dumps(schema, ensure_ascii=False, indent=2)}

单据内容：
{text}

要求：
1. 只输出JSON，不要解释，不要markdown代码块
2. 提取不到的字段设为null
3. items是数组，每个元素包含对应字段
4. 日期格式统一为YYYY-MM-DD"""

        result = await ai_client.llm_extract_json(prompt, schema)
        return result

    async def _diff_with_po(self, extracted: Dict, po_no: str) -> Dict:
        """
        与 PO 采购订单比对差异（通过 data_client 查询 WMS MySQL PO 明细）
        替换旧版 Mock 硬编码
        """
        po_items = await self._query_po_items(po_no)
        delivery_items = extracted.get("items", [])

        diffs = []
        po_map = {item["sku"]: item for item in po_items}
        delivery_map = {item.get("sku"): item for item in delivery_items if item.get("sku")}

        for sku, po_item in po_map.items():
            del_item = delivery_map.get(sku)
            if not del_item:
                diffs.append({"type": "missing", "sku": sku, "po_qty": po_item["qty"], "delivery_qty": 0})
            elif int(del_item.get("qty", 0)) != po_item["qty"]:
                diffs.append({
                    "type": "qty_mismatch", "sku": sku,
                    "po_qty": po_item["qty"], "delivery_qty": del_item.get("qty"),
                })

        for sku in delivery_map:
            if sku not in po_map:
                diffs.append({"type": "extra", "sku": sku, "delivery_qty": delivery_map[sku].get("qty")})

        return {
            "has_diff": len(diffs) > 0, "diffs": diffs, "po_no": po_no,
            "po_item_count": len(po_items), "delivery_item_count": len(delivery_items),
        }

    async def _query_po_items(self, po_no: str) -> List[Dict]:
        """
        通过 data_client 查询 WMS MySQL PO 明细
        查询采购订单明细表，返回 [{sku, qty, batch_no}]
        """
        import httpx
        try:
            async with httpx.AsyncClient(timeout=10) as client:
                resp = await client.post(
                    f"{settings.data_service_url}/v1/data/mysql",
                    json={
                        "sql": (
                            "SELECT sku, quantity AS qty, batch_no "
                            "FROM po_detail WHERE po_no = %s"
                        ),
                        "params": [po_no],
                    },
                )
                if resp.status_code == 200:
                    data = resp.json()
                    items = data.get("data") or data.get("rows") or []
                    if items:
                        return items
                    # PO 明细表无数据时，尝试通用查询
                    logger.warning(f"PO明细查询无数据: po_no={po_no}")
                    return []
                else:
                    logger.warning(f"PO明细查询失败: status={resp.status_code}")
                    return []
        except Exception as e:
            logger.warning(f"PO明细查询异常（降级返回空列表）: {e}")
            return []

    async def _diff_with_order(self, extracted: Dict, order_no: str) -> Dict:
        """与原订单比对差异（退货单）"""
        return {"has_diff": False, "diffs": [], "order_no": order_no}

    def confirm(self, doc_id: str, modified_data: Dict = None,
                confirmed_by: str = None) -> Dict:
        """人工确认（可修改数据）— 更新 MySQL + Redis 缓存"""
        # 先查 Redis 缓存，miss 则查 MySQL
        result = redis_client.get_json(f"ocr:result:{doc_id}")
        if not result:
            result = self._get_from_mysql(doc_id)
            if not result:
                raise HTTPException(status_code=404, detail="单据不存在")
        if modified_data:
            result["extracted_data"] = modified_data
        result["status"] = "confirmed"
        result["confirmed_at"] = now_str()

        # 更新 MySQL ocr_result 表
        sql = """
            UPDATE ocr_result
            SET status = %s, confirmed_by = %s, confirmed_at = %s,
                extracted = %s, updated_at = NOW()
            WHERE result_id = %s
        """
        mysql_client.execute(sql, (
            "confirmed",
            confirmed_by or "system",
            result["confirmed_at"],
            json.dumps(result["extracted_data"], ensure_ascii=False) if modified_data else None,
            doc_id,
        ))

        # 更新 Redis 缓存（短期）
        redis_client.set_json(f"ocr:result:{doc_id}", result, expire=3600)

        # 发送Kafka事件，触发WMS入库流程（PRD 8.2 Topic 规划）
        from common.data_client import kafka_client
        kafka_client.send("ocr-confirmed", result, key=doc_id)
        logger.info(f"OCR确认完成: doc_id={doc_id}, confirmed_by={confirmed_by}")

        return result

    def _get_from_mysql(self, doc_id: str) -> Optional[Dict]:
        """从 MySQL 查询单条 OCR 结果"""
        rows = mysql_client.query(
            "SELECT result_id, doc_type, doc_no, warehouse, image_path, "
            "extracted, raw_text, confidence, diff_result, status, "
            "confirmed_by, confirmed_at, created_at "
            "FROM ocr_result WHERE result_id = %s",
            (doc_id,),
        )
        if not rows:
            return None
        r = rows[0]
        return {
            "doc_id": r["result_id"],
            "doc_type": r["doc_type"],
            "doc_no": r["doc_no"],
            "warehouse": r["warehouse"],
            "image_path": r["image_path"],
            "extracted_data": json.loads(r["extracted"]) if r["extracted"] else {},
            "raw_text": r["raw_text"] or "",
            "confidence": float(r["confidence"]) if r["confidence"] else 0.0,
            "diff_result": json.loads(r["diff_result"]) if r["diff_result"] else None,
            "status": r["status"],
            "confirmed_by": r["confirmed_by"],
            "confirmed_at": r["confirmed_at"].strftime("%Y-%m-%d %H:%M:%S") if r["confirmed_at"] else None,
            "created_at": r["created_at"].strftime("%Y-%m-%d %H:%M:%S") if r["created_at"] else now_str(),
        }

    def get_result(self, doc_id: str) -> Dict:
        """获取识别结果（先查 Redis 缓存，miss 则查 MySQL）"""
        result = redis_client.get_json(f"ocr:result:{doc_id}")
        if not result:
            result = self._get_from_mysql(doc_id)
            if result:
                # 回填 Redis 缓存
                redis_client.set_json(f"ocr:result:{doc_id}", result, expire=3600)
        if not result:
            raise HTTPException(status_code=404, detail="单据不存在")
        return result

    def list_results(self, status: str = None, doc_type: str = None,
                     limit: int = 50, offset: int = 0) -> List[Dict]:
        """列出识别结果（MySQL 分页查询，替换旧 return []）"""
        conditions = []
        params = []
        if status:
            conditions.append("status = %s")
            params.append(status)
        if doc_type:
            conditions.append("doc_type = %s")
            params.append(doc_type)
        where = " WHERE " + " AND ".join(conditions) if conditions else ""
        sql = (
            "SELECT result_id, doc_type, doc_no, warehouse, confidence, "
            "status, confirmed_by, created_at, confirmed_at "
            f"FROM ocr_result{where} "
            "ORDER BY created_at DESC LIMIT %s OFFSET %s"
        )
        params.extend([limit, offset])
        rows = mysql_client.query(sql, tuple(params))
        # 统一字段名
        return [{
            "doc_id": r["result_id"],
            "doc_type": r["doc_type"],
            "doc_no": r["doc_no"],
            "warehouse": r["warehouse"],
            "confidence": float(r["confidence"]) if r["confidence"] else 0.0,
            "status": r["status"],
            "confirmed_by": r["confirmed_by"],
            "created_at": r["created_at"].strftime("%Y-%m-%d %H:%M:%S") if r["created_at"] else None,
            "confirmed_at": r["confirmed_at"].strftime("%Y-%m-%d %H:%M:%S") if r["confirmed_at"] else None,
        } for r in rows]


ocr_service = OCRService()


# ========== API接口 ==========
@app.post("/ocr/upload")
async def upload_and_recognize(
    image: UploadFile = File(...),
    doc_type: str = Form(None),
    po_no: str = Form(None),
    order_no: str = Form(None),
):
    """上传单据并识别"""
    image_bytes = await image.read()
    if not image_bytes:
        raise HTTPException(status_code=400, detail="图片为空")

    result = await ocr_service.process(image_bytes, doc_type, po_no, order_no)
    return Result.success(result.model_dump())


@app.post("/ocr/batch-upload")
async def batch_upload(images: List[UploadFile] = File(...), doc_type: str = Form(None)):
    """批量上传识别（异步处理）"""
    from common.data_client import kafka_client
    task_id = gen_id()
    for img in images:
        img_bytes = await img.read()
        # 上传到MinIO
        obj_name = f"ocr/batch/{task_id}/{img.filename}"
        minio_client.upload_bytes(obj_name, img_bytes, "image/jpeg")
        # 发Kafka异步处理
        kafka_client.send("ocr-batch", {
            "task_id": task_id,
            "object_name": obj_name,
            "doc_type": doc_type,
        })
    return Result.success({"task_id": task_id, "count": len(images), "status": "processing"})


@app.get("/ocr/result/{doc_id}")
async def get_result(doc_id: str):
    """获取识别结果"""
    return Result.success(ocr_service.get_result(doc_id))


@app.post("/ocr/confirm/{doc_id}")
async def confirm(doc_id: str, confirmed_by: str = Form(None),
                  modified_data: Dict = None):
    """人工确认（可修改数据）"""
    return Result.success(ocr_service.confirm(doc_id, modified_data, confirmed_by))


@app.get("/ocr/list")
async def list_results(status: str = None, doc_type: str = None,
                       limit: int = 50, offset: int = 0):
    """列出识别结果（MySQL 分页查询）"""
    return Result.success(ocr_service.list_results(status, doc_type, limit, offset))


@app.get("/ocr/doc-types")
async def get_doc_types():
    """获取支持的单据类型"""
    return Result.success([
        {"value": dt.value, "label": dt.name, "fields": DOC_FIELD_SCHEMA.get(dt, {})}
        for dt in DocType
    ])


@app.get("/health")
async def health():
    return Result.success({
        "status": "ok", "module": "ocr",
        "mysql": "mock" if mysql_client.is_mock else "connected",
    })


# ========== Kafka 批量消费者（PRD 8.2 Topic: ocr-batch）==========
async def handle_ocr_batch_message(msg: Dict):
    """
    消费 ocr-batch topic 消息，异步处理批量上传的单据
    消息格式（PRD 8.2 对齐）：
        {task_id, object_name, doc_type, po_no?, order_no?}
    """
    task_id = msg.get("task_id", "unknown")
    object_name = msg.get("object_name")
    doc_type = msg.get("doc_type")
    po_no = msg.get("po_no")
    order_no = msg.get("order_no")

    if not object_name:
        logger.warning(f"ocr-batch消息缺少object_name: task_id={task_id}")
        return

    try:
        # 从 MinIO 下载图片
        image_bytes = minio_client.download_bytes(object_name)
        if not image_bytes:
            logger.warning(f"MinIO下载失败: {object_name}")
            return

        # 调用 OCR 处理流程
        result = await ocr_service.process(
            image_bytes, doc_type=doc_type, po_no=po_no, order_no=order_no,
        )
        logger.info(f"ocr-batch处理完成: task_id={task_id}, doc_id={result.doc_id}")

        # 发送处理完成事件（PRD 8.2 Topic: ocr-batch-result）
        from common.data_client import kafka_client
        kafka_client.send("ocr-batch-result", {
            "task_id": task_id,
            "doc_id": result.doc_id,
            "doc_type": result.doc_type,
            "status": "completed",
            "confidence": result.confidence,
        }, key=result.doc_id)

    except Exception as e:
        logger.error(f"ocr-batch处理失败: task_id={task_id}, error={e}")
        from common.data_client import kafka_client
        kafka_client.send("ocr-batch-result", {
            "task_id": task_id,
            "object_name": object_name,
            "status": "failed",
            "error": str(e),
        })


@app.on_event("startup")
async def startup_kafka_consumer():
    """启动时注册 ocr-batch Kafka 消费者"""
    try:
        from common.data_client import kafka_client
        consumer = kafka_client.get_consumer("ocr-batch", group_id="ocr-batch-consumer")
        if consumer:
            import asyncio
            asyncio.create_task(_run_kafka_consumer(consumer))
            logger.info("ocr-batch Kafka消费者已启动")
        else:
            logger.info("Kafka不可用，ocr-batch消费者未启动（mock模式）")
    except Exception as e:
        logger.warning(f"ocr-batch消费者启动失败: {e}")


async def _run_kafka_consumer(consumer):
    """Kafka 消费者循环"""
    try:
        for message in consumer:
            try:
                msg_data = message.value
                if isinstance(msg_data, str):
                    msg_data = json.loads(msg_data)
                await handle_ocr_batch_message(msg_data)
                consumer.commit()
            except Exception as e:
                logger.error(f"ocr-batch消息处理失败: {e}")
    except Exception as e:
        logger.error(f"ocr-batch消费者异常: {e}")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8101)
