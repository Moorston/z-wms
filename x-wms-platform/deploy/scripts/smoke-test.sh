#!/bin/bash
# X WMS 冒烟测试脚本
# 用法: ./smoke-test.sh <API_BASE_URL>
set -e

API_URL=${1:-"http://localhost:8081"}
TOKEN=""
PASS=0
FAIL=0

echo "========================================="
echo "  X WMS 冒烟测试"
echo "  目标: $API_URL"
echo "========================================="

# 测试用例执行函数
test_case() {
    local name=$1
    local method=$2
    local path=$3
    local expected_code=${4:-200}
    local data=$5

    echo -n "  [$name] "
    if [ -n "$data" ]; then
        code=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $TOKEN" \
            -d "$data" \
            "$API_URL$path")
    else
        code=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" \
            -H "Authorization: Bearer $TOKEN" \
            "$API_URL$path")
    fi

    if [ "$code" = "$expected_code" ]; then
        echo "PASS (HTTP $code)"
        PASS=$((PASS + 1))
    else
        echo "FAIL (期望 $expected_code, 实际 $code)"
        FAIL=$((FAIL + 1))
    fi
}

# 1. 健康检查
echo ""
echo "[1/6] 服务健康检查"
test_case "wms-core健康" GET "/actuator/health" 200
test_case "wms-base健康" GET "http://localhost:8082/actuator/health" 200

# 2. 认证
echo ""
echo "[2/6] 认证接口"
LOGIN_RESP=$(curl -s -X POST "$API_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}')
TOKEN=$(echo "$LOGIN_RESP" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
if [ -n "$TOKEN" ]; then
    echo "  [登录] PASS"
    PASS=$((PASS + 1))
else
    echo "  [登录] FAIL"
    FAIL=$((FAIL + 1))
fi

# 3. 基础数据
echo ""
echo "[3/6] 基础数据接口"
test_case "商品列表" GET "/api/products?page=1&size=10" 200
test_case "库位列表" GET "/api/locations?page=1&size=10" 200
test_case "货主列表" GET "/api/owners?page=1&size=10" 200

# 4. 库存查询
echo ""
echo "[4/6] 库存接口"
test_case "库存查询" GET "/api/inventory?page=1&size=10" 200

# 5. 入库流程
echo ""
echo "[5/6] 入库流程"
test_case "入库单列表" GET "/api/inbound/orders?page=1&size=10" 200

# 6. 出库流程
echo ""
echo "[6/6] 出库流程"
test_case "出库单列表" GET "/api/outbound/orders?page=1&size=10" 200

# 结果汇总
echo ""
echo "========================================="
echo "  测试结果: PASS=$PASS, FAIL=$FAIL"
echo "========================================="

if [ $FAIL -gt 0 ]; then
    exit 1
fi
exit 0
