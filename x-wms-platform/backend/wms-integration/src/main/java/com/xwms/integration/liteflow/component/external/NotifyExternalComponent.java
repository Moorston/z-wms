package com.xwms.integration.liteflow.component.external;

import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.integration.external.service.IntegrationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow外部系统通知组件 - 业务流程中通知外部系统 出库完成后通知TMS/ERP, 库存变更后同步ERP */
@Slf4j
@LiteflowComponent("notifyExternal")
@RequiredArgsConstructor
public class NotifyExternalComponent extends NodeComponent {

    private final IntegrationService integrationService;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null || context.get("systemCode") == null) {
            log.info("无外部系统上下文, 跳过");
            return;
        }
        try {
            String systemCode = context.get("systemCode").toString();
            String messageType =
                    context.get("messageType") != null
                            ? context.get("messageType").toString()
                            : "NOTIFY";
            String topic = context.get("topic") != null ? context.get("topic").toString() : null;
            String payload =
                    context.get("payload") != null ? context.get("payload").toString() : null;
            String businessType =
                    context.get("businessType") != null
                            ? context.get("businessType").toString()
                            : null;
            String businessNo =
                    context.get("businessNo") != null ? context.get("businessNo").toString() : null;

            integrationService.createMessage(
                    systemCode, messageType, topic, payload, businessType, businessNo, "SEND");
            log.info("流程通知外部系统: {}, 类型={}", systemCode, messageType);
        } catch (Exception e) {
            log.error("流程通知外部系统失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
