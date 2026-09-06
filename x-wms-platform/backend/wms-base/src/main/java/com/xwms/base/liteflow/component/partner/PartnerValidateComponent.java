package com.xwms.base.liteflow.component.partner;

import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.base.partner.entity.Owner;
import com.xwms.base.partner.service.PartnerManagementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow合作伙伴校验组件 - 业务流程中校验货主/客户状态 适用于入库/出库/调拨等操作前的合作伙伴状态校验 */
@Slf4j
@LiteflowComponent("partnerValidate")
@RequiredArgsConstructor
public class PartnerValidateComponent extends NodeComponent {

    private final PartnerManagementService partnerManagementService;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null || context.get("ownerCode") == null) {
            log.info("无合作伙伴校验上下文, 跳过");
            return;
        }
        try {
            String ownerCode = context.get("ownerCode").toString();
            Owner owner = partnerManagementService.getOwnerByCode(ownerCode);

            if (owner == null) {
                context.put("validateError", "货主不存在: " + ownerCode);
                log.warn("货主校验失败: 不存在 {}", ownerCode);
                return;
            }

            if (!"ACTIVE".equals(owner.getStatus())) {
                context.put("validateError", "货主状态异常: " + owner.getStatus());
                log.warn("货主校验失败: 状态异常 {}={}", ownerCode, owner.getStatus());
                return;
            }

            // 信用额度检查
            if (owner.getCreditLimit() != null
                    && owner.getCreditUsed() != null
                    && owner.getCreditUsed().compareTo(owner.getCreditLimit()) > 0) {
                context.put("validateWarning", "货主信用额度超限: " + ownerCode);
                log.warn(
                        "货主信用额度超限: {} used={} limit={}",
                        ownerCode,
                        owner.getCreditUsed(),
                        owner.getCreditLimit());
            }

            context.put("ownerValidated", true);
            log.info("货主校验通过: {}", ownerCode);
        } catch (Exception e) {
            log.error("合作伙伴校验失败: {}", e.getMessage());
            context.put("validateError", "校验异常: " + e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
