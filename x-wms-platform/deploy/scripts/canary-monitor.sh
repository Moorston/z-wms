#!/bin/bash
# X WMS 金丝雀发布监控脚本
# 用法: ./canary-monitor.sh <监控时长秒>
set -e

DURATION=${1:-300}
INTERVAL=10
ERROR_THRESHOLD=1
LATENCY_THRESHOLD=3000
PROMETHEUS_URL=${PROMETHEUS_URL:-"http://localhost:9090"}

echo "========================================="
echo "  金丝雀监控启动"
echo "  时长: ${DURATION}s, 间隔: ${INTERVAL}s"
echo "  错误率阈值: ${ERROR_THRESHOLD}%"
echo "  延迟阈值: ${LATENCY_THRESHOLD}ms"
echo "========================================="

START=$(date +%s)
ERROR_COUNT=0
TOTAL_COUNT=0
MAX_LATENCY=0

while true; do
    NOW=$(date +%s)
    ELAPSED=$((NOW - START))
    if [ $ELAPSED -ge $DURATION ]; then
        break
    fi

    # 检查金丝雀Pod健康
    CANARY_READY=$(kubectl get pods -n xwms-prod -l app=wms-core,tier=canary \
        -o jsonpath='{.items[0].status.containerStatuses[0].ready}' 2>/dev/null || echo "false")

    if [ "$CANARY_READY" != "true" ]; then
        echo "[$(date +%H:%M:%S)] 金丝雀Pod未就绪, 触发回滚"
        kubectl patch deployment wms-core-canary -n xwms-prod -p '{"spec":{"replicas":0}}'
        exit 1
    fi

    # 采样API错误率 (从actuator metrics)
    ERROR_RATE=$(curl -s "http://localhost:8081/actuator/metrics/http.server.requests" \
        | grep -o '"value":"5[0-9][0-9]"' | wc -l 2>/dev/null || echo "0")

    # 采样延迟
    LATENCY=$(curl -s -o /dev/null -w "%{time_total}" http://localhost:8081/actuator/health 2>/dev/null || echo "0")
    LATENCY_MS=$(echo "$LATENCY * 1000" | bc 2>/dev/null || echo "0")
    LATENCY_INT=${LATENCY_MS%.*}

    if [ "$LATENCY_INT" -gt "$MAX_LATENCY" ]; then
        MAX_LATENCY=$LATENCY_INT
    fi

    echo "[$(date +%H:%M:%S)] 已运行${ELAPSED}s, 金丝雀就绪, 延迟=${LATENCY_INT}ms, 最大延迟=${MAX_LATENCY}ms"

    # 错误率检查
    if [ "$ERROR_RATE" -gt "$ERROR_THRESHOLD" ]; then
        echo "[$(date +%H:%M:%S)] 错误率超过阈值(${ERROR_RATE}% > ${ERROR_THRESHOLD}%), 触发回滚"
        kubectl patch deployment wms-core-canary -n xwms-prod -p '{"spec":{"replicas":0}}'
        exit 1
    fi

    # 延迟检查
    if [ "$LATENCY_INT" -gt "$LATENCY_THRESHOLD" ]; then
        echo "[$(date +%H:%M:%S)] 延迟超过阈值(${LATENCY_INT}ms > ${LATENCY_THRESHOLD}ms), 触发回滚"
        kubectl patch deployment wms-core-canary -n xwms-prod -p '{"spec":{"replicas":0}}'
        exit 1
    fi

    sleep $INTERVAL
done

echo "========================================="
echo "  金丝雀监控通过"
echo "  最大延迟: ${MAX_LATENCY}ms"
echo "========================================="
exit 0
