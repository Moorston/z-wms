package com.xwms.core.liteflow.component.alert;

import java.math.BigDecimal;
import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.alert.service.AlertService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow预警触发组件 - 业务流程中触发预警 库存不足/作业延误/质量异常等场景自动触发预警 */
@Slf4j
@LiteflowComponent("triggerAlert")
@RequiredArgsConstructor
public class TriggerAlertComponent extends NodeComponent {

    private final AlertService alertService;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null || context.get("ruleCode") == null) {
            log.info("无预警上下文, 跳过");
            return;
        }
        try {
            String ruleCode = context.get("ruleCode").toString();
            String title = context.get("title") != null ? context.get("title").toString() : "系统预警";
            String content =
                    context.get("content") != null ? context.get("content").toString() : null;
            String businessType =
                    context.get("businessType") != null
                            ? context.get("businessType").toString()
                            : null;
            String businessNo =
                    context.get("businessNo") != null ? context.get("businessNo").toString() : null;
            String warehouseCode =
                    context.get("warehouseCode") != null
                            ? context.get("warehouseCode").toString()
                            : null;
            String locationCode =
                    context.get("locationCode") != null
                            ? context.get("locationCode").toString()
                            : null;
            String skuCode =
                    context.get("skuCode") != null ? context.get("skuCode").toString() : null;
            BigDecimal currentValue =
                    context.get("currentValue") != null
                            ? new BigDecimal(context.get("currentValue").toString())
                            : null;
            BigDecimal thresholdValue =
                    context.get("thresholdValue") != null
                            ? new BigDecimal(context.get("thresholdValue").toString())
                            : null;
            String severity =
                    context.get("severity") != null ? context.get("severity").toString() : null;

            alertService.triggerAlert(
                    ruleCode,
                    title,
                    content,
                    businessType,
                    businessNo,
                    warehouseCode,
                    locationCode,
                    skuCode,
                    currentValue,
                    thresholdValue,
                    severity);
            log.info("流程触发预警: 规则={}, 标题={}", ruleCode, title);
        } catch (Exception e) {
            log.error("流程触发预警失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
