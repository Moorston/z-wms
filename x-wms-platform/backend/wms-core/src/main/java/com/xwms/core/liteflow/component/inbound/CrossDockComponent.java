package com.xwms.core.liteflow.component.inbound;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.InboundContext;

import lombok.extern.slf4j.Slf4j;

/** 越库组件（Cross-Dock） 越库入库不经过存储，直接转到出库发货区 */
@Slf4j
@LiteflowComponent("crossDock")
public class CrossDockComponent extends NodeComponent {

    @Override
    public void process() {
        InboundContext context = this.getContextBean(InboundContext.class);

        log.info("[入库流程-越库] 越库处理开始");

        // TODO: 越库逻辑
        // 1. 匹配对应的出库单
        // 2. 收货后直接转到发货区
        // 3. 不增加库存（直接扣减+发运）
        // 4. 关联入库单和出库单

        context.setCrossDockOutboundNo("OUT-CROSSDOCK-001");
        log.info("[入库流程-越库] 越库处理完成, 关联出库单={}", context.getCrossDockOutboundNo());
    }
}
