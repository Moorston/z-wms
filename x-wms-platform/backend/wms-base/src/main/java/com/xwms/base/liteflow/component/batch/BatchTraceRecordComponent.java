package com.xwms.base.liteflow.component.batch;

import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.base.batch.service.BatchAttributeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow批次追踪组件 - 业务流程中自动记录批次追踪日志 适用于入库/出库/调拨/调整等操作 */
@Slf4j
@LiteflowComponent("batchTraceRecord")
@RequiredArgsConstructor
public class BatchTraceRecordComponent extends NodeComponent {

    private final BatchAttributeService batchAttributeService;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null || context.get("batchNo") == null) {
            log.info("无批次追踪上下文, 跳过");
            return;
        }
        try {
            String batchNo = context.get("batchNo").toString();
            String skuCode =
                    context.get("skuCode") != null ? context.get("skuCode").toString() : null;
            String operationType =
                    context.get("operationType") != null
                            ? context.get("operationType").toString()
                            : "UNKNOWN";
            String operationNo =
                    context.get("operationNo") != null
                            ? context.get("operationNo").toString()
                            : null;
            String fromLocation =
                    context.get("fromLocation") != null
                            ? context.get("fromLocation").toString()
                            : null;
            String toLocation =
                    context.get("toLocation") != null ? context.get("toLocation").toString() : null;
            String operator =
                    context.get("operator") != null ? context.get("operator").toString() : "system";
            String traceId =
                    context.get("traceId") != null ? context.get("traceId").toString() : null;

            java.math.BigDecimal quantity =
                    context.get("quantity") != null
                            ? new java.math.BigDecimal(context.get("quantity").toString())
                            : null;

            String traceData =
                    context.get("traceData") != null ? context.get("traceData").toString() : null;

            batchAttributeService.recordTraceLog(
                    batchNo,
                    skuCode,
                    operationType,
                    operationNo,
                    fromLocation,
                    toLocation,
                    quantity,
                    operator,
                    traceData,
                    traceId);

            log.info("批次追踪记录: {} {} {}", batchNo, operationType, operationNo);
        } catch (Exception e) {
            log.error("批次追踪记录失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
