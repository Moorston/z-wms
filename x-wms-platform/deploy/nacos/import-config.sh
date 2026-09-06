#!/bin/bash
# ============================================================
# X WMS Nacos 配置导入脚本
# 用法：./import-nacos-config.sh [nacos-server]
# 默认：http://localhost:8848
# ============================================================

NACOS_SERVER="${1:-http://localhost:8848}"
GROUP="WMS_GROUP"
NAMESPACE="public"
CONFIG_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "============================================"
echo "  X WMS Nacos 配置导入"
echo "  Nacos Server: $NACOS_SERVER"
echo "  Group: $GROUP"
echo "============================================"

# 导入配置函数
import_config() {
    local data_id="$1"
    local file_path="$2"
    local content=$(cat "$file_path")

    echo "导入: $data_id ..."
    curl -s -X POST "$NACOS_SERVER/nacos/v1/cs/configs" \
        -d "dataId=$data_id" \
        -d "group=$GROUP" \
        -d "namespaceId=$NAMESPACE" \
        -d "type=yaml" \
        --data-urlencode "content=$content"

    echo ""
}

# 导入共享配置
import_config "wms-common.yaml" "$CONFIG_DIR/wms-common.yaml"

# 导入各服务配置
import_config "wms-core.yaml" "$CONFIG_DIR/wms-core.yaml"
import_config "wms-base.yaml" "$CONFIG_DIR/wms-base.yaml"
import_config "wms-analytics.yaml" "$CONFIG_DIR/wms-analytics.yaml"
import_config "wms-integration.yaml" "$CONFIG_DIR/wms-integration.yaml"

echo ""
echo "============================================"
echo "  配置导入完成！"
echo "  Nacos控制台: $NACOS_SERVER/nacos"
echo "  用户名/密码: nacos/nacos"
echo "============================================"
