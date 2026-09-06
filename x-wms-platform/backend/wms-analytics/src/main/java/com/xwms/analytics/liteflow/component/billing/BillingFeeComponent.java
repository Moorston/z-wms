package com.xwms.analytics.liteflow.component.billing;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.analytics.billing.service.BillingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow费收组件 - 出入库完成后自动计费 入库完成: 入库费 + 仓储费(按天) 出库完成: 出库费 + 操作费 VAS完成: 增值服务费 */
@Slf4j
@LiteflowComponent("billingFee")
@RequiredArgsConstructor
public class BillingFeeComponent extends NodeComponent {

    private final BillingService billingService;

    @Override
    public void process() {
        // 从上下文获取计费信息
        String feeType = this.getContextBean(String.class);
        if (feeType == null) {
            log.info("无计费类型, 跳过");
            return;
        }
        log.info("触发计费: feeType={}", feeType);
        // TODO: 从业务上下文获取货主/客户/数量等信息, 调用calculateAndCreateFee
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
