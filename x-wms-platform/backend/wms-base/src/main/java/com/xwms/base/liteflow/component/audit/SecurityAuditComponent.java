package com.xwms.base.liteflow.component.audit;

import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.base.audit.service.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow安全审计组件 - 业务流程中记录安全审计 敏感操作(数据导出/配置变更/权限变更)自动记录审计 */
@Slf4j
@LiteflowComponent("securityAudit")
@RequiredArgsConstructor
public class SecurityAuditComponent extends NodeComponent {

    private final AuditService auditService;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null || context.get("auditType") == null) {
            log.info("无审计上下文, 跳过");
            return;
        }
        try {
            String auditType = context.get("auditType").toString();
            String userId =
                    context.get("userId") != null ? context.get("userId").toString() : "SYSTEM";
            String userName =
                    context.get("userName") != null ? context.get("userName").toString() : "系统";
            String riskLevel =
                    context.get("riskLevel") != null
                            ? context.get("riskLevel").toString()
                            : "MEDIUM";
            String description =
                    context.get("description") != null ? context.get("description").toString() : "";
            String resourceType =
                    context.get("resourceType") != null
                            ? context.get("resourceType").toString()
                            : null;
            String resourceId =
                    context.get("resourceId") != null ? context.get("resourceId").toString() : null;
            String action = context.get("action") != null ? context.get("action").toString() : null;
            String ipAddress =
                    context.get("ipAddress") != null ? context.get("ipAddress").toString() : null;

            auditService.createSecurityAudit(
                    auditType,
                    userId,
                    userName,
                    riskLevel,
                    description,
                    resourceType,
                    resourceId,
                    action,
                    null,
                    null,
                    ipAddress);
            log.info("流程安全审计: 类型={}, 用户={}", auditType, userName);
        } catch (Exception e) {
            log.error("流程安全审计失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
