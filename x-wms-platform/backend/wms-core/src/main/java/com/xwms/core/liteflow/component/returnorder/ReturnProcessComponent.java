package com.xwms.core.liteflow.component.returnorder;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.returnorder.service.ReturnOrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow退货组件 - 退货全流程编排 退货收货 → 质检 → 上架 → 退款 */
@Slf4j
@LiteflowComponent("returnProcess")
@RequiredArgsConstructor
public class ReturnProcessComponent extends NodeComponent {

    private final ReturnOrderService returnOrderService;

    @Override
    public void process() {
        Long returnId = this.getContextBean(Long.class);
        if (returnId == null) {
            log.info("无退货单ID, 跳过");
            return;
        }
        log.info("退货流程编排: returnId={}", returnId);
        // 退货流程由各步骤独立触发, 此处仅做编排入口
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
