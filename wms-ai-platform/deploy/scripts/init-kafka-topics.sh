#!/bin/bash
# WMS AI Platform - Kafka Topic 初始化脚本

KAFKA_BROKER="${KAFKA_BROKER:-localhost:9092}"

echo "初始化Kafka Topics..."

# 定义所有Topic
TOPICS=(
    "ocr-batch:3:1"
    "ocr-confirmed:3:1"
    "forecast-done:1:1"
    "report-push:1:1"
    "aiops-alert:3:1"
    "aiops-notify:1:1"
    "pick-complete:3:1"
    "pick-exception:3:1"
    "drone-command:1:1"
    "drone-result:3:1"
    "video-alert:3:1"
    "wms-event:6:1"
)

for topic_info in "${TOPICS[@]}"; do
    IFS=':' read -r topic partitions replicas <<< "$topic_info"
    echo "创建Topic: $topic (partitions=$partitions, replicas=$replicas)"
    docker exec -i $(docker ps --filter "name=kafka" -q) kafka-topics \
        --bootstrap-server localhost:9092 \
        --create --if-not-exists \
        --topic "$topic" \
        --partitions "$partitions" \
        --replication-factor "$replicas" 2>/dev/null || \
    kafka-topics --bootstrap-server "$KAFKA_BROKER" \
        --create --if-not-exists \
        --topic "$topic" \
        --partitions "$partitions" \
        --replication-factor "$replicas"
done

echo "Kafka Topics初始化完成！"
echo "Topic列表："
docker exec -i $(docker ps --filter "name=kafka" -q) kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null || \
kafka-topics --bootstrap-server "$KAFKA_BROKER" --list
