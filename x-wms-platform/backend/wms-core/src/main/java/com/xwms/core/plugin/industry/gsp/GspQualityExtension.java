package com.xwms.core.plugin.industry.gsp;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.xwms.common.plugin.extension.ExtensionResult;
import com.xwms.common.plugin.extension.QualityExtension;

import lombok.extern.slf4j.Slf4j;

/** 医药GSP质检扩展插件 核心管控：入库全检/抽检、特殊药品双人验收、不合格品隔离 */
@Slf4j
@Component
public class GspQualityExtension implements QualityExtension {

    @Override
    public String getPluginId() {
        return "gsp-quality";
    }

    @Override
    public String getPluginName() {
        return "医药GSP质检插件";
    }

    @Override
    public int getPriority() {
        return 200;
    }

    /** 医药入库必须质检（GSP要求） 首营品种全检，常规品种抽检 */
    @Override
    public boolean needQualityCheck(Map<String, Object> context) {
        return true; // 医药行业入库必须质检
    }

    @Override
    public String getQualityType(Map<String, Object> context) {
        return "IQC"; // 来料检验
    }

    /** 抽样方案：首营品种全检，常规品种按AQL抽样 */
    @Override
    public ExtensionResult getSamplingPlan(Map<String, Object> context) {
        boolean isFirstBusiness = Boolean.TRUE.equals(context.get("firstBusiness"));
        int totalQty = (int) context.getOrDefault("totalQty", 0);
        if (isFirstBusiness) {
            // 首营品种全检
            return ExtensionResult.intercept(
                    "首营品种全检", Map.of("sampleType", "FULL", "sampleQty", totalQty));
        }
        // 常规品种AQL抽样（一般检验水平II，AQL=2.5）
        int sampleQty =
                switch (totalQty) {
                    case 0, 1, 2, 3, 4, 5, 6, 7, 8 -> totalQty;
                    case 9, 10, 11, 12, 13, 14, 15 -> 5;
                    case 16, 17, 18, 19, 20, 21, 22, 23, 24, 25 -> 8;
                    default -> 13;
                };
        return ExtensionResult.intercept(
                "AQL抽样", Map.of("sampleType", "AQL", "sampleQty", sampleQty, "aql", "2.5"));
    }

    /** 质检项目：性状/鉴别/含量/包装/标签 */
    @Override
    public ExtensionResult getQualityItems(Map<String, Object> context) {
        return ExtensionResult.intercept(
                "GSP质检项目",
                Map.of(
                        "items",
                        java.util.List.of(
                                "APPEARANCE", "IDENTIFICATION", "CONTENT", "PACKAGING", "LABEL"),
                        "needDoubleCheck",
                        context.get("drugType") != null // 特殊药品双人验收
                        ));
    }

    /** 质检结果处理：不合格品隔离 */
    @Override
    public void onQualityResult(Map<String, Object> context) {
        boolean qualified = Boolean.TRUE.equals(context.get("qualified"));
        if (!qualified) {
            log.error(
                    "GSP质检不合格，隔离处理: sku={}, batch={}, reason={}",
                    context.get("sku"),
                    context.get("batchNo"),
                    context.get("reason"));
            // TODO: 库存移入不合格品区，锁定状态，通知质量部
        }
    }
}
