package com.xwms.core.liteflow.component.inventory;

import java.math.BigDecimal;
import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.inventory.service.InventoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow库存扣减组件 - 业务流程中调用库存扣减 Oracle直接扣减库存(原子SQL+乐观锁)，Redis仅做库存检查 */
@Slf4j
@LiteflowComponent("inventoryDeduct")
@RequiredArgsConstructor
public class InventoryDeductComponent extends NodeComponent {

    private final InventoryService inventoryService;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null
                || context.get("warehouseCode") == null
                || context.get("skuCode") == null) {
            log.info("无库存扣减上下文, 跳过");
            return;
        }
        try {
            String warehouseCode = context.get("warehouseCode").toString();
            String locationCode =
                    context.get("locationCode") != null
                            ? context.get("locationCode").toString()
                            : null;
            String skuCode = context.get("skuCode").toString();
            String batchNo =
                    context.get("batchNo") != null ? context.get("batchNo").toString() : null;
            String ownerCode =
                    context.get("ownerCode") != null ? context.get("ownerCode").toString() : null;
            BigDecimal qty = new BigDecimal(context.get("qty").toString());
            String refType =
                    context.get("refType") != null ? context.get("refType").toString() : "OUTBOUND";
            String refNo = context.get("refNo") != null ? context.get("refNo").toString() : null;
            String operator =
                    context.get("operator") != null ? context.get("operator").toString() : "system";

            inventoryService.deductInventory(
                    warehouseCode,
                    locationCode,
                    skuCode,
                    batchNo,
                    ownerCode,
                    qty,
                    refType,
                    refNo,
                    operator);

            context.put("inventoryDeducted", true);
            log.info("库存扣减完成: sku={}, qty={}", skuCode, qty);
        } catch (Exception e) {
            log.error("库存扣减失败: {}", e.getMessage());
            context.put("inventoryError", e.getMessage());
            context.put("inventoryDeducted", false);
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
