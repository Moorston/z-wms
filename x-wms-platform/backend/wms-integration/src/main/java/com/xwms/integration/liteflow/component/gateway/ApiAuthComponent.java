package com.xwms.integration.liteflow.component.gateway;

import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.integration.gateway.entity.ApiKey;
import com.xwms.integration.gateway.service.ApiGatewayService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow API鉴权组件 - API调用前的鉴权校验 校验appKey/appSecret/IP白名单/状态 */
@Slf4j
@LiteflowComponent("apiAuth")
@RequiredArgsConstructor
public class ApiAuthComponent extends NodeComponent {

    private final ApiGatewayService apiGatewayService;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null || context.get("appKey") == null) {
            context.put("authError", "缺少appKey");
            log.warn("API鉴权失败: 缺少appKey");
            return;
        }
        try {
            String appKey = context.get("appKey").toString();
            String appSecret =
                    context.get("appSecret") != null ? context.get("appSecret").toString() : null;
            String requestIp =
                    context.get("requestIp") != null ? context.get("requestIp").toString() : null;

            ApiKey apiKey = apiGatewayService.getApiKeyByAppKey(appKey);

            if (apiKey == null) {
                context.put("authError", "appKey不存在: " + appKey);
                log.warn("API鉴权失败: appKey不存在 {}", appKey);
                return;
            }

            if (!"ACTIVE".equals(apiKey.getStatus())) {
                context.put("authError", "API密钥状态异常: " + apiKey.getStatus());
                log.warn("API鉴权失败: 状态异常 {}={}", appKey, apiKey.getStatus());
                return;
            }

            // 校验appSecret
            if (appSecret != null && !appSecret.equals(apiKey.getAppSecret())) {
                context.put("authError", "appSecret不匹配");
                log.warn("API鉴权失败: appSecret不匹配 {}", appKey);
                return;
            }

            // IP白名单校验
            if (apiKey.getIpWhitelist() != null
                    && !apiKey.getIpWhitelist().isEmpty()
                    && requestIp != null
                    && !apiKey.getIpWhitelist().contains(requestIp)) {
                context.put("authError", "IP不在白名单: " + requestIp);
                log.warn("API鉴权失败: IP不在白名单 {}={}", appKey, requestIp);
                return;
            }

            context.put("authPassed", true);
            context.put("apiKeyInfo", apiKey);
            log.info("API鉴权通过: {}", appKey);
        } catch (Exception e) {
            log.error("API鉴权异常: {}", e.getMessage());
            context.put("authError", "鉴权异常: " + e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
