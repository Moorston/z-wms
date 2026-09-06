package com.xwms.core.liteflow.component.wave;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.WaveExecuteContext;

import lombok.extern.slf4j.Slf4j;

/** 发运组件 最终发运确认，扣减库存，更新订单状态 */
@Slf4j
@LiteflowComponent("ship")
public class ShipComponent extends NodeComponent {

    @Override
    public void process() {
        WaveExecuteContext context = this.getContextBean(WaveExecuteContext.class);

        log.info("[波次流程] 发运开始, 订单数={}", context.getOrders().size());

        // TODO: 发运逻辑
        // 1. Oracle直接扣减库存（原子操作）
        // 2. 更新出库单状态为SHIPPED
        // 3. 发布发运事件到Kafka
        // 4. 通知TMS/ERP

        context.setSuccess(true);
        log.info("[波次流程] 发运完成");
    }
}
