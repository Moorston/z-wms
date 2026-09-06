#!/bin/bash
# X WMS 部署通知脚本
# 用法: ./notify.sh <消息内容>
set -e

MESSAGE=${1:-"X WMS 部署通知"}
WEBHOOK_URL=${WECHAT_WEBHOOK_URL:-""}
DINGTALK_URL=${DINGTALK_WEBHOOK_URL:-""}
EMAIL_TO=${NOTIFY_EMAIL:-""}

TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")
GIT_BRANCH=${GITHUB_REF_NAME:-"unknown"}
GIT_COMMIT=${GITHUB_SHA::7:-"unknown"}
ACTOR=${GITHUB_ACTOR:-"unknown"}

FULL_MESSAGE="【X WMS 部署通知】
时间: $TIMESTAMP
分支: $GIT_BRANCH
提交: $GIT_COMMIT
操作人: $ACTOR
状态: $MESSAGE"

echo "$FULL_MESSAGE"

# 企业微信通知
if [ -n "$WEBHOOK_URL" ]; then
    echo "发送企业微信通知..."
    curl -s -X POST "$WEBHOOK_URL" \
        -H "Content-Type: application/json" \
        -d "{\"msgtype\":\"text\",\"text\":{\"content\":\"$FULL_MESSAGE\"}}" > /dev/null
fi

# 钉钉通知
if [ -n "$DINGTALK_URL" ]; then
    echo "发送钉钉通知..."
    curl -s -X POST "$DINGTALK_URL" \
        -H "Content-Type: application/json" \
        -d "{\"msgtype\":\"text\",\"text\":{\"content\":\"$FULL_MESSAGE\"}}" > /dev/null
fi

# 邮件通知
if [ -n "$EMAIL_TO" ]; then
    echo "发送邮件通知..."
    echo "$FULL_MESSAGE" | mail -s "X WMS 部署通知 - $MESSAGE" "$EMAIL_TO"
fi

echo "通知发送完成"
