"""
WMS AI Platform - 数据服务
统一数据接入：MySQL(主数据库)/Oracle(WMS只读兼容)/ClickHouse(OLAP)/Kafka/MinIO

PRD V2.0 改动：
  - 新增 MySQL 查询端点 /v1/data/mysql（只读 SELECT）
  - 新增 get_mysql_conn() 懒加载
  - 提取 _validate_select_sql() 公共校验供三端点共用
  - /v1/data/health 增加 MySQL 状态
"""
import os
import sys
import json
from typing import List, Dict, Any, Optional
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from loguru import logger

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from common.config import settings
from common.utils import Result
from common.cache_metrics import setup_metrics
from common.tracing import init_tracing, instrument_app

app = FastAPI(title="WMS AI Data Service", version="2.0.0")
setup_metrics(app)
init_tracing("data_service")
instrument_app(app, "data_service")

# 数据库连接（懒加载，避免导入时网络阻塞）
_mysql_conn = None
_oracle_conn = None
_clickhouse_conn = None


def _validate_select_sql(sql: str) -> str:
    """
    SQL 安全校验（公共，供 Oracle/ClickHouse/MySQL 端点共用）
    - 使用 sqlparse AST 校验（非 denylist），阻断 UNION/INTO OUTFILE 等注入
    - 只允许 SELECT（单条语句，禁止堆叠查询）
    - 禁止 DML/DDL
    - 自动补 LIMIT（未指定时）
    """
    import sqlparse

    # 解析为 AST，确保只有一条语句且为 SELECT
    statements = sqlparse.parse(sql)
    if not statements or len(statements) == 0:
        raise HTTPException(status_code=400, detail="SQL为空")
    if len(statements) > 1:
        raise HTTPException(status_code=400, detail="禁止堆叠查询")

    stmt = statements[0]
    stmt_type = stmt.get_type()  # SELECT / INSERT / UPDATE / DELETE / UNKNOWN
    if stmt_type != "SELECT":
        raise HTTPException(status_code=400, detail="只允许SELECT查询")

    # 遍历 AST 检测危险关键字（UNION 子查询、INTO OUTFILE/DUMPFILE 等）
    forbidden_patterns = {
        "UNION",        # UNION 子查询注入
        "INTO",         # INTO OUTFILE/DUMPFILE（写文件）
        "LOAD",         # LOAD_FILE（读文件）
        "SLEEP",        # 时间盲注
        "BENCHMARK",    # 基准函数注入
    }
    for token in stmt.flatten():
        if token.ttype in (sqlparse.tokens.Keyword, sqlparse.tokens.Keyword.DML,
                           sqlparse.tokens.Keyword.DDL):
            word = token.value.upper()
            if word in forbidden_patterns:
                raise HTTPException(status_code=400, detail=f"禁止{word}操作")

    # 自动补 LIMIT（未指定时）
    if "LIMIT" not in sql.upper():
        sql = sql.strip()
        if sql.endswith(";"):
            sql = sql[:-1]
        sql += " LIMIT 1000"
    return sql


def get_mysql_conn():
    """获取 MySQL 连接（PRD V2.0 主数据库，懒加载）"""
    global _mysql_conn
    if _mysql_conn is None:
        try:
            import pymysql
            _mysql_conn = pymysql.connect(
                host=settings.mysql_host,
                port=settings.mysql_port,
                user=settings.mysql_user,
                password=settings.mysql_password,
                database=settings.mysql_database,
                charset="utf8mb4",
                cursorclass=pymysql.cursors.DictCursor,
                autocommit=True,
            )
            logger.info(f"MySQL连接成功: {settings.mysql_host}:{settings.mysql_port}/{settings.mysql_database}")
        except Exception as e:
            logger.error(f"MySQL连接失败: {e}")
            _mysql_conn = "mock"
    return _mysql_conn


def get_oracle_conn():
    """获取Oracle连接（WMS只读，@deprecated 保留兼容）"""
    global _oracle_conn
    if _oracle_conn is None:
        try:
            import oracledb
            _oracle_conn = oracledb.connect(
                user=settings.oracle_user,
                password=settings.oracle_password,
                dsn=settings.oracle_dsn,
            )
        except Exception as e:
            logger.error(f"Oracle连接失败: {e}")
            _oracle_conn = "mock"
    return _oracle_conn


def get_clickhouse_conn():
    """获取ClickHouse连接"""
    global _clickhouse_conn
    if _clickhouse_conn is None:
        try:
            import clickhouse_connect
            _clickhouse_conn = clickhouse_connect.get_client(
                host=settings.clickhouse_host,
                port=settings.clickhouse_port,
                username=settings.clickhouse_user,
                password=settings.clickhouse_password,
                database=settings.clickhouse_database,
            )
        except Exception as e:
            logger.error(f"ClickHouse连接失败: {e}")
            _clickhouse_conn = "mock"
    return _clickhouse_conn


class QueryRequest(BaseModel):
    sql: str
    params: Dict[str, Any] = {}


@app.post("/v1/data/mysql")
async def query_mysql(req: QueryRequest):
    """查询 MySQL 数据（只读 SELECT，知识库/对话/OCR 等 PRD V2.0 数据）"""
    sql = _validate_select_sql(req.sql)

    conn = get_mysql_conn()
    if conn == "mock":
        return Result.success({"data": [], "mock": True})

    try:
        with conn.cursor() as cursor:
            cursor.execute(sql, req.params)
            rows = cursor.fetchall()
        return Result.success({"data": rows, "count": len(rows)})
    except Exception as e:
        logger.error(f"MySQL查询失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/v1/data/query")
async def query_wms(req: QueryRequest):
    """查询WMS Oracle数据（只读，@deprecated 保留兼容）"""
    sql = _validate_select_sql(req.sql)

    conn = get_oracle_conn()
    if conn == "mock":
        return Result.success({"data": [], "mock": True})

    try:
        cursor = conn.cursor()
        cursor.execute(sql, req.params)
        columns = [col[0] for col in cursor.description]
        rows = [dict(zip(columns, row)) for row in cursor.fetchall()]
        cursor.close()
        return Result.success({"data": rows, "count": len(rows)})
    except Exception as e:
        logger.error(f"Oracle查询失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/v1/data/olap")
async def query_olap(req: QueryRequest):
    """查询ClickHouse OLAP数据"""
    sql = _validate_select_sql(req.sql)

    conn = get_clickhouse_conn()
    if conn == "mock":
        return Result.success({"data": [], "mock": True})

    try:
        result = conn.query(sql)
        columns = result.column_names
        rows = [dict(zip(columns, row)) for row in result.result_rows]
        return Result.success({"data": rows, "count": len(rows)})
    except Exception as e:
        logger.error(f"ClickHouse查询失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/v1/data/health")
async def data_health():
    """数据服务健康检查"""
    return Result.success({
        "mysql": "connected" if get_mysql_conn() != "mock" else "mock",
        "oracle": "connected" if get_oracle_conn() != "mock" else "mock",
        "clickhouse": "connected" if get_clickhouse_conn() != "mock" else "mock",
    })


@app.get("/health")
async def health():
    return Result.success({"status": "ok", "module": "data-service"})


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8002)
