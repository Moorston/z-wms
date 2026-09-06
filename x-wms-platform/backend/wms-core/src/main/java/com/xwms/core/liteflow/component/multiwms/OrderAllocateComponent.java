package com.xwms.core.liteflow.component.multiwms;

import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.multiwms.service.MultiWarehouseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow多仓协同组件 - 订单创建后自动分配仓库 订单接入时, 根据库存和收货地址智能分配到最优仓库 */
@Slf4j
@LiteflowComponent("orderAllocate")
@RequiredArgsConstructor
public class OrderAllocateComponent extends NodeComponent {

    private final MultiWarehouseService multiWarehouseService;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null || context.get("orderNo") == null) {
            log.info("无订单上下文, 跳过分配");
            return;
        }
        try {
            String orderNo = context.get("orderNo").toString();
            String sku = context.get("sku") != null ? context.get("sku").toString() : null;
            String productName =
                    context.get("productName") != null
                            ? context.get("productName").toString()
                            : null;
            java.math.BigDecimal requiredQty =
                    context.get("requiredQty") != null
                            ? new java.math.BigDecimal(context.get("requiredQty").toString())
                            : java.math.BigDecimal.ZERO;
            String customerAddress =
                    context.get("customerAddress") != null
                            ? context.get("customerAddress").toString()
                            : null;
            String ownerCode =
                    context.get("ownerCode") != null ? context.get("ownerCode").toString() : null;

            multiWarehouseService.allocateOrder(
                    orderNo,
                    "OUTBOUND",
                    sku,
                    productName,
                    requiredQty,
                    customerAddress,
                    "NORMAL",
                    ownerCode);
            log.info("订单自动分配: {}", orderNo);
        } catch (Exception e) {
            log.error("订单自动分配失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
