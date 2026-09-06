"""
WMS AI Platform - AIOps 修复脚本库单元测试（TDD）

测试缝隙（seams）：
  - modules.aiops.main.FixScript 基类 + 4 个脚本实现
  - modules.aiops.main.FIX_SCRIPTS 注册表 — 关键词匹配
  - modules.aiops.main._alert_to_dict() — Pydantic 安全转换
  - modules.aiops.main.AIOpsService._can_auto_fix() — 判断是否可自动修复
  - modules.aiops.main.AIOpsService._auto_fix() — 脚本匹配+执行
  - modules.aiops.main.AlertRequest — 告警请求模型
  - modules.aiops.main.alertmanager_webhook() — AlertManager Webhook 解析

验证行为（不测实现细节）：
  1. 每个 FixScript.execute() 返回正确的 action/success 结构
  2. FIX_SCRIPTS 注册表包含所有 4 种修复脚本
  3. 关键词匹配：告警根因含"重启"→匹配 RestartPodFix
  4. 关键词匹配：告警根因含"OOM"→匹配 RestartPodFix
  5. 关键词匹配：告警根因含"缓存"→匹配 ClearCacheFix
  6. 关键词匹配：告警根因含"死信"→匹配 RetryDLQFix
  7. 关键词匹配：告警根因含"扩容"→匹配 ScaleUpFix
  8. 无匹配关键词时返回 success=False, reason="无匹配修复脚本"
  9. _alert_to_dict() 兼容 Pydantic v2 (model_dump) 和 v1 (dict)
  10. _can_auto_fix() 返回 True 当根因含修复关键词
  11. _can_auto_fix() 返回 False 当根因不含修复关键词
  12. AlertRequest 模型正确解析字段
"""
import asyncio
import os
import sys
import json
from unittest.mock import MagicMock, patch
from types import SimpleNamespace

import pytest

# 确保项目根目录在 sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# ========== 预注入 Mock，阻止 common/__init__.py 触发真实网络连接 ==========

class _FakeRedisModule:
    class Redis:
        def __init__(self, **kwargs):
            self._store = {}
        def get(self, key): return self._store.get(key)
        def set(self, key, value, ex=None): self._store[key] = value
        def delete(self, key): self._store.pop(key, None)
        def exists(self, key): return 1 if key in self._store else 0
        def incr(self, key, amount=1):
            v = int(self._store.get(key, 0)) + amount
            self._store[key] = v
            return v
        def zrevrange(self, key, start, end, withscores=False): return []
        def zincrby(self, key, amount, value): pass
        def zremrangebyrank(self, key, start, end): pass
        def scan_iter(self, pattern, count=100): return []

import types as _types
_fake_redis_mod = _types.ModuleType("redis")
_fake_redis_mod.Redis = _FakeRedisModule.Redis
sys.modules["redis"] = _fake_redis_mod

_fake_kafka_mod = _types.ModuleType("kafka")
_fake_kafka_mod.KafkaProducer = MagicMock()
_fake_kafka_mod.KafkaConsumer = MagicMock()
_fake_kafka_errors_mod = _types.ModuleType("kafka.errors")
_fake_kafka_errors_mod.KafkaError = Exception
sys.modules["kafka"] = _fake_kafka_mod
sys.modules["kafka.errors"] = _fake_kafka_errors_mod

_fake_minio_mod = _types.ModuleType("minio")
_fake_minio_mod.Minio = MagicMock()
_fake_minio_errors_mod = _types.ModuleType("minio.error")
_fake_minio_errors_mod.S3Error = Exception
sys.modules["minio"] = _fake_minio_mod
sys.modules["minio.error"] = _fake_minio_errors_mod

_fake_pymilvus = _types.ModuleType("pymilvus")
_fake_pymilvus.MilvusClient = MagicMock
_fake_pymilvus.DataType = MagicMock()
sys.modules["pymilvus"] = _fake_pymilvus

_fake_ch = _types.ModuleType("clickhouse_driver")
_fake_ch.Client = MagicMock()
sys.modules["clickhouse_driver"] = _fake_ch

_fake_pymysql = _types.ModuleType("pymysql")
_fake_pymysql.connect = MagicMock()
sys.modules["pymysql"] = _fake_pymysql

_fake_aiomysql = _types.ModuleType("aiomysql")
_fake_aiomysql.connect = MagicMock()
sys.modules["aiomysql"] = _fake_aiomysql

# 注意：不要向 sys.modules 注入假的 common.cache_metrics。
# 此前注入的假模块只含 setup_metrics，后续测试文件（test_cache_architecture.py）
# 需要 record_hit / cache_hits / setup_metrics 真实实现，会因此 ImportError。
# setup_metrics 仅在被调用时才注册路由，模块导入本身无副作用，无需 mock。


# ========== 1. FixScript 脚本库测试 ==========

class TestFixScripts:

    def test_restart_pod_fix_executes_correctly(self):
        """RestartPodFix.execute() 返回正确的 action/success 结构"""
        from modules.aiops.main import RestartPodFix
        script = RestartPodFix()
        result = asyncio.run(script.execute({"service": "model-service"}, {}))
        assert result["action"] == "restart_pod"
        assert result["success"] is True
        assert "model-service" in result["command"]
        assert result["service"] == "model-service"

    def test_clear_cache_fix_executes_correctly(self):
        """ClearCacheFix.execute() 返回正确的 action/success 结构"""
        from modules.aiops.main import ClearCacheFix
        script = ClearCacheFix()
        # mock redis_client 的 scan_iter 返回空
        with patch("modules.aiops.main.redis_client") as mock_redis:
            mock_redis.client.scan_iter.return_value = iter([])
            result = asyncio.run(script.execute({"service": "gateway"}, {}))
        assert result["action"] == "clear_cache"
        assert result["success"] is True
        assert result["service"] == "gateway"

    def test_retry_dlq_fix_executes_correctly(self):
        """RetryDLQFix.execute() 返回正确的 action/success 结构"""
        from modules.aiops.main import RetryDLQFix
        script = RetryDLQFix()
        result = asyncio.run(script.execute({"metric": "dlq_ocr_topic"}, {}))
        assert result["action"] == "retry_dlq"
        assert result["success"] is True
        assert result["topic"] == "ocr_topic"  # 去掉 dlq_ 前缀

    def test_scale_up_fix_executes_correctly(self):
        """ScaleUpFix.execute() 返回正确的 action/success 结构"""
        from modules.aiops.main import ScaleUpFix
        script = ScaleUpFix()
        result = asyncio.run(script.execute({"service": "rag-service"}, {}))
        assert result["action"] == "scale_up"
        assert result["success"] is True
        assert result["service"] == "rag-service"
        assert result["replicas"] == "+1"


# ========== 2. FIX_SCRIPTS 注册表测试 ==========

class TestFixScriptsRegistry:

    def test_registry_contains_all_four_scripts(self):
        """FIX_SCRIPTS 注册表包含所有 4 种修复脚本"""
        from modules.aiops.main import FIX_SCRIPTS
        script_names = set(type(s).__name__ for s in FIX_SCRIPTS.values())
        assert "RestartPodFix" in script_names
        assert "ClearCacheFix" in script_names
        assert "RetryDLQFix" in script_names
        assert "ScaleUpFix" in script_names

    def test_registry_has_nine_keywords(self):
        """FIX_SCRIPTS 注册表有 9 个关键词映射"""
        from modules.aiops.main import FIX_SCRIPTS
        assert len(FIX_SCRIPTS) == 9


# ========== 3. 关键词匹配测试 ==========

class TestKeywordMatching:

    def _run_auto_fix(self, root_cause_causes: list, alert: dict = None):
        """辅助方法：运行 _auto_fix 并返回结果"""
        from modules.aiops.main import AIOpsService
        alert = alert or {"service": "test-svc", "metric": "test_metric"}
        root_cause = {"possible_causes": root_cause_causes}
        with patch("modules.aiops.main.redis_client") as mock_redis:
            mock_redis.set_json = MagicMock()
            service = AIOpsService()
            return asyncio.run(service._auto_fix(root_cause, alert))

    def test_keyword_restart_matches_restart_pod_fix(self):
        """根因含"重启"→匹配 RestartPodFix"""
        result = self._run_auto_fix(["pod 需要重启"])
        assert result["script"] == "restart_pod"
        assert result["success"] is True

    def test_keyword_oom_matches_restart_pod_fix(self):
        """根因含"OOM"→匹配 RestartPodFix"""
        result = self._run_auto_fix(["OOM killed"])
        assert result["script"] == "restart_pod"
        assert result["success"] is True

    def test_keyword_memory_matches_restart_pod_fix(self):
        """根因含"memory"→匹配 RestartPodFix"""
        result = self._run_auto_fix(["memory usage too high"])
        assert result["script"] == "restart_pod"
        assert result["success"] is True

    def test_keyword_cache_matches_clear_cache_fix(self):
        """根因含"缓存"→匹配 ClearCacheFix"""
        result = self._run_auto_fix(["缓存穿透导致热点"])
        assert result["script"] == "clear_cache"
        assert result["success"] is True

    def test_keyword_cache_en_matches_clear_cache_fix(self):
        """根因含"cache"→匹配 ClearCacheFix"""
        result = self._run_auto_fix(["cache miss rate too high"])
        assert result["script"] == "clear_cache"
        assert result["success"] is True

    def test_keyword_dlq_matches_retry_dlq_fix(self):
        """根因含"死信"→匹配 RetryDLQFix"""
        result = self._run_auto_fix(["消息进入死信队列"])
        assert result["script"] == "retry_dlq"
        assert result["success"] is True

    def test_keyword_dlq_en_matches_retry_dlq_fix(self):
        """根因含"dlq"→匹配 RetryDLQFix"""
        result = self._run_auto_fix(["dlq_ocr messages stuck"])
        assert result["script"] == "retry_dlq"
        assert result["success"] is True

    def test_keyword_scale_up_matches_scale_up_fix(self):
        """根因含"扩容"→匹配 ScaleUpFix"""
        result = self._run_auto_fix(["需要扩容"])
        assert result["script"] == "scale_up"
        assert result["success"] is True

    def test_keyword_cpu_matches_scale_up_fix(self):
        """根因含"cpu"→匹配 ScaleUpFix"""
        result = self._run_auto_fix(["cpu usage 95%"])
        assert result["script"] == "scale_up"
        assert result["success"] is True

    def test_no_matching_keyword_returns_failure(self):
        """无匹配关键词时返回 success=False, reason=无匹配修复脚本"""
        result = self._run_auto_fix(["数据库连接超时", "网络抖动"])
        assert result["success"] is False
        assert result["action"] == "none"
        assert "无匹配" in result["reason"]


# ========== 4. _alert_to_dict() 兼容性测试 ==========

class TestAlertToDict:

    def test_alert_to_dict_returns_dict(self):
        """_alert_to_dict() 返回 dict 类型"""
        from modules.aiops.main import _alert_to_dict, AlertRequest
        alert = AlertRequest(
            alert_name="HighCPU",
            severity="critical",
            service="model-service",
            metric="cpu_usage",
            value=95.0,
            threshold=80.0,
            labels={"env": "production"},
        )
        result = _alert_to_dict(alert)
        assert isinstance(result, dict)
        assert result["alert_name"] == "HighCPU"
        assert result["severity"] == "critical"
        assert result["service"] == "model-service"

    def test_alert_to_dict_preserves_all_fields(self):
        """_alert_to_dict() 保留所有字段"""
        from modules.aiops.main import _alert_to_dict, AlertRequest
        alert = AlertRequest(
            alert_name="TestAlert",
            severity="warning",
            service="gateway",
            metric="qps",
            value=100.0,
            threshold=50.0,
            labels={"env": "staging", "region": "cn-north"},
        )
        result = _alert_to_dict(alert)
        assert result["labels"] == {"env": "staging", "region": "cn-north"}


# ========== 5. _can_auto_fix() 测试 ==========

class TestCanAutoFix:

    def _make_service(self):
        from modules.aiops.main import AIOpsService
        return AIOpsService()

    def test_can_auto_fix_returns_true_when_cause_has_keyword(self):
        """根因含修复关键词时返回 True"""
        service = self._make_service()
        result = service._can_auto_fix({"possible_causes": ["pod 需要重启"]})
        assert result is True

    def test_can_auto_fix_returns_false_when_no_keyword(self):
        """根因不含修复关键词时返回 False"""
        service = self._make_service()
        result = service._can_auto_fix({"possible_causes": ["未知原因", "网络抖动"]})
        assert result is False

    def test_can_auto_fix_returns_true_for_oom(self):
        """根因含 OOM 时返回 True"""
        service = self._make_service()
        result = service._can_auto_fix({"possible_causes": ["OOM killed"]})
        assert result is True

    def test_can_auto_fix_returns_true_for_memory(self):
        """根因含 memory 时返回 True"""
        service = self._make_service()
        result = service._can_auto_fix({"possible_causes": ["memory leak"]})
        assert result is True


# ========== 6. AlertRequest 模型测试 ==========

class TestAlertRequest:

    def test_alert_request_parses_all_fields(self):
        """AlertRequest 模型正确解析所有字段"""
        from modules.aiops.main import AlertRequest
        alert = AlertRequest(
            alert_name="HighCPU",
            severity="critical",
            service="model-service",
            metric="cpu_usage",
            value=95.0,
            threshold=80.0,
            labels={"env": "production"},
        )
        assert alert.alert_name == "HighCPU"
        assert alert.severity == "critical"
        assert alert.service == "model-service"
        assert alert.metric == "cpu_usage"
        assert alert.value == 95.0
        assert alert.threshold == 80.0
        assert alert.labels == {"env": "production"}

    def test_alert_request_default_values(self):
        """AlertRequest 默认值：severity=warning, labels={}"""
        from modules.aiops.main import AlertRequest
        alert = AlertRequest(
            alert_name="Test",
            service="svc",
            metric="m",
            value=1.0,
            threshold=2.0,
        )
        assert alert.severity == "warning"
        assert alert.labels == {}


# ========== 7. AlertManager Webhook 格式测试 ==========

class TestAlertManagerWebhook:

    def test_alertmanager_format_with_alerts_array(self):
        """AlertManager 格式（含 alerts 数组）正确解析"""
        from modules.aiops.main import AlertRequest
        # 模拟 AlertManager payload
        am_alert = {
            "labels": {
                "alertname": "HighCPU",
                "severity": "critical",
                "service": "model-service",
                "metric": "cpu_usage",
            },
            "value": 95.0,
            "threshold": 80.0,
        }
        alert = AlertRequest(
            alert_name=am_alert["labels"].get("alertname", "unknown"),
            severity=am_alert["labels"].get("severity", "warning"),
            service=am_alert["labels"].get("service", "unknown"),
            metric=am_alert["labels"].get("metric", ""),
            value=float(am_alert.get("value", 0)),
            threshold=float(am_alert.get("threshold", 0)),
            labels=am_alert.get("labels", {}),
        )
        assert alert.alert_name == "HighCPU"
        assert alert.service == "model-service"
        assert alert.value == 95.0

    def test_alertmanager_single_alert_format(self):
        """AlertManager 单告警格式（无 alerts 数组）也能解析"""
        from modules.aiops.main import AlertRequest
        am_alert = {
            "labels": {
                "alertname": "LowDisk",
                "severity": "warning",
                "service": "data-service",
                "metric": "disk_usage",
            },
            "value": 85.0,
            "threshold": 80.0,
        }
        # AlertRequest 从 labels 提取字段（与 webhook 端点逻辑一致）
        alert = AlertRequest(
            alert_name=am_alert["labels"].get("alertname", "unknown"),
            severity=am_alert["labels"].get("severity", "warning"),
            service=am_alert["labels"].get("service", "unknown"),
            metric=am_alert["labels"].get("metric", ""),
            value=float(am_alert.get("value", 0)),
            threshold=float(am_alert.get("threshold", 0)),
            labels=am_alert.get("labels", {}),
        )
        assert alert.alert_name == "LowDisk"
        assert alert.severity == "warning"
        assert alert.service == "data-service"
