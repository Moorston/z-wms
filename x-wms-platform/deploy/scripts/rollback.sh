#!/bin/bash
set -e
# X WMS 回滚脚本
# 用法: ./rollback.sh <环境> <服务名> <版本>
# 示例: ./rollback.sh prod wms-core v1.0.0

ENV=${1:?"环境参数必填 (dev/staging/prod)"}
SERVICE=${2:?"服务名必填"}
VERSION=${3:?"版本号必填"}

NAMESPACE="xwms-${ENV}"
REGISTRY="registry.cn-hangzhou.aliyuncs.com/xwms"

echo "========================================="
echo "  X WMS 回滚"
echo "  环境: $ENV"
echo "  服务: $SERVICE"
echo "  目标版本: $VERSION"
echo "  命名空间: $NAMESPACE"
echo "========================================="

# 1. 确认当前版本
CURRENT_IMAGE=$(kubectl get deployment $SERVICE -n $NAMESPACE \
    -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null)
echo "当前镜像: $CURRENT_IMAGE"

# 2. 执行回滚
echo "执行回滚到 $REGISTRY/$SERVICE:$VERSION ..."
kubectl set image deployment/$SERVICE $SERVICE=$REGISTRY/$SERVICE:$VERSION -n $NAMESPACE

# 3. 等待回滚完成
echo "等待回滚完成..."
kubectl rollout status deployment/$SERVICE -n $NAMESPACE --timeout=300s

# 4. 验证
ROLLBACK_IMAGE=$(kubectl get deployment $SERVICE -n $NAMESPACE \
    -o jsonpath='{.spec.template.spec.containers[0].image}')
echo "回滚后镜像: $ROLLBACK_IMAGE"

if echo "$ROLLBACK_IMAGE" | grep -q "$VERSION"; then
    echo "回滚成功"
else
    echo "回滚失败, 镜像不匹配"
    exit 1
fi

# 5. 健康检查
echo "健康检查..."
for i in {1..30}; do
    POD=$(kubectl get pods -n $NAMESPACE -l app=$SERVICE -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
    READY=$(kubectl get pod $POD -n $NAMESPACE -o jsonpath='{.status.containerStatuses[0].ready}' 2>/dev/null)
    if [ "$READY" = "true" ]; then
        echo "健康检查通过"
        exit 0
    fi
    sleep 10
done

echo "健康检查失败"
exit 1
