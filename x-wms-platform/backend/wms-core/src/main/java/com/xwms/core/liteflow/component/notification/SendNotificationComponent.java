package com.xwms.core.liteflow.component.notification;

import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow通知组件 - 业务流程中发送通知 入库/出库/质检等流程节点完成后, 自动发送通知 */
@Slf4j
@LiteflowComponent("sendNotification")
@RequiredArgsConstructor
public class SendNotificationComponent extends NodeComponent {

    private final NotificationService notificationService;

    @Override
    public void process() {
        String eventType = this.getContextBean(String.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> eventData = this.getContextBean(Map.class);
        if (eventType == null || eventData == null) {
            log.info("无事件类型或数据, 跳过通知");
            return;
        }
        try {
            String receiverId =
                    eventData.get("receiverId") != null
                            ? eventData.get("receiverId").toString()
                            : "SYSTEM";
            String receiverName =
                    eventData.get("receiverName") != null
                            ? eventData.get("receiverName").toString()
                            : "系统管理员";
            notificationService.triggerEvent(
                    eventType, eventData, "USER", receiverId, receiverName);
            log.info("流程通知发送: 事件={}", eventType);
        } catch (Exception e) {
            log.error("流程通知发送失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
