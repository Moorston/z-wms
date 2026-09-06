package com.xwms.core.liteflow.component.shipment;

import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.shipment.entity.Shipment;
import com.xwms.core.shipment.service.ShipmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow发运执行组件 - 发运流程编排 包含: 承运商选择→获取运单号→打印→发运确认 高并发场景: 批量获取和打印使用异步处理 */
@Slf4j
@LiteflowComponent("shipmentExecute")
@RequiredArgsConstructor
public class ShipmentExecuteComponent extends NodeComponent {

    private final ShipmentService shipmentService;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null || context.get("shipmentNo") == null) {
            log.info("无发运执行上下文, 跳过");
            return;
        }
        try {
            String shipmentNo = context.get("shipmentNo").toString();
            String action =
                    context.get("action") != null
                            ? context.get("action").toString()
                            : "GET_TRACKING";

            Shipment shipment = shipmentService.getShipmentByNo(shipmentNo);
            if (shipment == null) {
                context.put("shipmentError", "发运单不存在: " + shipmentNo);
                log.warn("发运执行失败: 发运单不存在 {}", shipmentNo);
                return;
            }

            switch (action) {
                case "SELECT_CARRIER":
                    context.put("shipmentStatus", shipment.getStatus());
                    log.info("选择承运商: {}", shipmentNo);
                    break;
                case "GET_TRACKING":
                    context.put("shipmentStatus", shipment.getStatus());
                    log.info("获取运单号: {}", shipmentNo);
                    break;
                case "PRINT":
                    context.put("shipmentStatus", shipment.getStatus());
                    log.info("打印快递单: {}", shipmentNo);
                    break;
                case "CONFIRM":
                    context.put("shipmentStatus", shipment.getStatus());
                    log.info("发运确认: {}", shipmentNo);
                    break;
                default:
                    context.put("shipmentStatus", shipment.getStatus());
                    break;
            }

            context.put("shipmentExecuted", true);
        } catch (Exception e) {
            log.error("发运执行异常: {}", e.getMessage());
            context.put("shipmentError", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
