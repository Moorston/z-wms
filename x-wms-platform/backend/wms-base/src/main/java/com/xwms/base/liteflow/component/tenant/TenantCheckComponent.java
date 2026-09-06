package com.xwms.base.liteflow.component.tenant;

import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.base.tenant.service.TenantService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow租户校验组件 - 业务流程中校验租户可用性和配额 业务操作前校验租户状态和资源配额 */
@Slf4j
@LiteflowComponent("tenantCheck")
@RequiredArgsConstructor
public class TenantCheckComponent extends NodeComponent {

    private final TenantService tenantService;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null || context.get("tenantCode") == null) {
            log.info("无租户上下文, 跳过校验");
            return;
        }
        String tenantCode = context.get("tenantCode").toString();

        // 校验租户可用性
        if (!tenantService.isTenantAvailable(tenantCode)) {
            throw new RuntimeException("租户不可用: " + tenantCode);
        }

        // 校验资源配额
        if (context.get("resourceType") != null && context.get("quotaAmount") != null) {
            String resourceType = context.get("resourceType").toString();
            long amount = Long.parseLong(context.get("quotaAmount").toString());
            if (!tenantService.increaseQuotaUsed(tenantCode, resourceType, amount)) {
                throw new RuntimeException("租户配额不足: " + tenantCode + " " + resourceType);
            }
        }
        log.info("租户校验通过: {}", tenantCode);
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
