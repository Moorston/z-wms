package com.xwms.core.liteflow.component.allocation;

import java.math.BigDecimal;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.inventory.entity.Inventory;
import com.xwms.core.inventory.service.InventoryService;
import com.xwms.core.liteflow.context.AllocationContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库存可用性检查组件 校验可用库存是否满足需求 */
@Slf4j
@LiteflowComponent("availabilityCheck")
@RequiredArgsConstructor
public class AvailabilityCheckComponent extends NodeComponent {

    private final InventoryService inventoryService;

    @Override
    public void process() {
        AllocationContext context = this.getContextBean(AllocationContext.class);

        log.info("[库存分配] 可用性检查: sku={}, required={}", context.getSku(), context.getRequiredQty());

        // 按 SKU+货主跨库位汇总可用库存
        BigDecimal available =
                inventoryService
                        .getInventoryBySku(context.getSku(), context.getOwnerCode())
                        .stream()
                        .map(Inventory::getAvailableQty)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (available.compareTo(context.getRequiredQty()) < 0) {
            context.markFailed(
                    "库存不足: required=" + context.getRequiredQty() + ", available=" + available);
            log.warn("[库存分配] 库存不足: required={}, available={}", context.getRequiredQty(), available);
            return;
        }

        // TODO: 按排序后的候选列表分配，直到满足需求
        // for (candidate : candidates)
        // {
        //     if (allocated >= required) break;
        //     context.addAllocation(location, batch, qty);
        // }

        log.info("[库存分配] 可用性检查通过, 已分配={}", context.getAllocatedQty());
    }

    @Override
    public boolean isContinueOnError() {
        return false;
    }
}
