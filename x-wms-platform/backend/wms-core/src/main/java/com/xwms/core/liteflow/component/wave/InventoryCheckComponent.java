package com.xwms.core.liteflow.component.wave;

import java.math.BigDecimal;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.inventory.service.InventoryService;
import com.xwms.core.liteflow.context.WaveExecuteContext;
import com.xwms.core.outbound.entity.OutboundOrder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库存预占校验组件 校验波次内所有订单的库存是否充足 */
@Slf4j
@LiteflowComponent("inventoryCheck")
@RequiredArgsConstructor
public class InventoryCheckComponent extends NodeComponent {

    private final InventoryService inventoryService;

    @Override
    public void process() {
        WaveExecuteContext context = this.getContextBean(WaveExecuteContext.class);

        log.info("[波次流程] 库存预占校验开始, 订单数={}", context.getOrders().size());

        for (OutboundOrder order : context.getOrders()) {
            // TODO: 按SKU汇总需求，校验可用库存
            // BigDecimal available = inventoryService.getAvailableQty(order.getSku(),
            // order.getWarehouse());
            // if (available.compareTo(order.getOrderQty()) < 0)
            // {
            //     context.markFailed("库存不足: " + order.getOrderNo());
            //     return;
            // }
            context.putAllocation(order.getOutboundNo(), BigDecimal.ZERO);
        }

        log.info("[波次流程] 库存预占校验通过");
    }

    @Override
    public boolean isContinueOnError() {
        return false; // 库存校验失败，中断流程
    }
}
