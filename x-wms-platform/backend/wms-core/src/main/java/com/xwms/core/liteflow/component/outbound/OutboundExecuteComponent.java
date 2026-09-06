package com.xwms.core.liteflow.component.outbound;

import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.outbound.entity.OutboundOrder;
import com.xwms.core.outbound.service.OutboundService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow出库执行组件 - 出库流程编排 包含: 分配→拣货→打包→发运 */
@Slf4j
@LiteflowComponent("outboundExecute")
@RequiredArgsConstructor
public class OutboundExecuteComponent extends NodeComponent {

    private final OutboundService outboundService;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null || context.get("outboundNo") == null) {
            log.info("无出库执行上下文, 跳过");
            return;
        }
        try {
            String outboundNo = context.get("outboundNo").toString();
            String action =
                    context.get("action") != null ? context.get("action").toString() : "ALLOCATE";

            OutboundOrder order = outboundService.getOutboundOrderByNo(outboundNo);
            if (order == null) {
                context.put("outboundError", "出库单不存在: " + outboundNo);
                log.warn("出库执行失败: 出库单不存在 {}", outboundNo);
                return;
            }

            switch (action) {
                case "ALLOCATE":
                    outboundService.allocate(outboundNo, "system");
                    context.put("outboundStatus", "ALLOCATED");
                    log.info("出库分配: {}", outboundNo);
                    break;
                case "PICK":
                    context.put("outboundStatus", order.getStatus());
                    log.info("出库拣货: {}", outboundNo);
                    break;
                case "PACK":
                    context.put("outboundStatus", order.getStatus());
                    log.info("出库打包: {}", outboundNo);
                    break;
                case "SHIP":
                    context.put("outboundStatus", order.getStatus());
                    log.info("出库发运: {}", outboundNo);
                    break;
                default:
                    context.put("outboundStatus", order.getStatus());
                    break;
            }

            context.put("outboundExecuted", true);
        } catch (Exception e) {
            log.error("出库执行异常: {}", e.getMessage());
            context.put("outboundError", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
