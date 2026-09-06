package com.xwms.core.liteflow.component.inbound;

import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.inbound.entity.InboundOrder;
import com.xwms.core.inbound.service.InboundService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow入库执行组件 - 入库流程编排 包含: 收货→质检→上架→完成 */
@Slf4j
@LiteflowComponent("inboundExecute")
@RequiredArgsConstructor
public class InboundExecuteComponent extends NodeComponent {

    private final InboundService inboundService;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null || context.get("inboundNo") == null) {
            log.info("无入库执行上下文, 跳过");
            return;
        }
        try {
            String inboundNo = context.get("inboundNo").toString();
            String action =
                    context.get("action") != null ? context.get("action").toString() : "RECEIVE";

            InboundOrder order = inboundService.getInboundOrderByNo(inboundNo);
            if (order == null) {
                context.put("inboundError", "入库单不存在: " + inboundNo);
                log.warn("入库执行失败: 入库单不存在 {}", inboundNo);
                return;
            }

            switch (action) {
                case "RECEIVE":
                    // 收货动作由receive接口单独调用，这里只做状态校验
                    context.put("inboundStatus", order.getStatus());
                    log.info("入库收货: {}", inboundNo);
                    break;
                case "COMPLETE_RECEIVE":
                    inboundService.completeReceive(inboundNo);
                    context.put("inboundStatus", "RECEIVED");
                    log.info("入库收货完成: {}", inboundNo);
                    break;
                case "PUTAWAY":
                    // 上架动作由putaway接口单独调用
                    context.put("inboundStatus", order.getStatus());
                    log.info("入库上架: {}", inboundNo);
                    break;
                default:
                    context.put("inboundStatus", order.getStatus());
                    break;
            }

            context.put("inboundExecuted", true);
        } catch (Exception e) {
            log.error("入库执行异常: {}", e.getMessage());
            context.put("inboundError", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
