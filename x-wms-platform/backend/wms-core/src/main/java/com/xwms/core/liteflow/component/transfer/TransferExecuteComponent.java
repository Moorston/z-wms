package com.xwms.core.liteflow.component.transfer;

import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.transfer.entity.Transfer;
import com.xwms.core.transfer.service.TransferService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow调拨执行组件 - 调拨流程编排 包含: 审批→发运→在途→收货 */
@Slf4j
@LiteflowComponent("transferExecute")
@RequiredArgsConstructor
public class TransferExecuteComponent extends NodeComponent {

    private final TransferService transferService;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null || context.get("transferNo") == null) {
            log.info("无调拨执行上下文, 跳过");
            return;
        }
        try {
            String transferNo = context.get("transferNo").toString();
            String action =
                    context.get("action") != null ? context.get("action").toString() : "APPROVE";

            Transfer transfer = transferService.getTransferByNo(transferNo);
            if (transfer == null) {
                context.put("transferError", "调拨单不存在: " + transferNo);
                log.warn("调拨执行失败: 调拨单不存在 {}", transferNo);
                return;
            }

            switch (action) {
                case "APPROVE":
                    String approver =
                            context.get("approver") != null
                                    ? context.get("approver").toString()
                                    : "system";
                    transferService.approveTransfer(transferNo, approver);
                    context.put("transferStatus", "APPROVED");
                    log.info("调拨审批: {}", transferNo);
                    break;
                case "SHIP":
                    context.put("transferStatus", transfer.getStatus());
                    log.info("调拨发运: {}", transferNo);
                    break;
                case "RECEIVE":
                    context.put("transferStatus", transfer.getStatus());
                    log.info("调拨收货: {}", transferNo);
                    break;
                default:
                    context.put("transferStatus", transfer.getStatus());
                    break;
            }

            context.put("transferExecuted", true);
        } catch (Exception e) {
            log.error("调拨执行异常: {}", e.getMessage());
            context.put("transferError", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
