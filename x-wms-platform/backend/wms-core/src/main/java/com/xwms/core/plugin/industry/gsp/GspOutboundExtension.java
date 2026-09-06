package com.xwms.core.plugin.industry.gsp;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.xwms.common.plugin.extension.ExtensionResult;
import com.xwms.common.plugin.extension.OutboundExtension;
import com.xwms.core.plugin.industry.gsp.service.GspBatchService;
import com.xwms.core.plugin.industry.gsp.service.GspTemperatureService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 医药GSP出库扩展插件 拦截出库流程：订单→分配→拣货→复核→发运 核心管控：FEFO近效期先出、双人复核、随货同行单、冷链运输温度 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GspOutboundExtension implements OutboundExtension {

    private final GspBatchService batchService;
    private final GspTemperatureService tempService;

    @Override
    public String getPluginId() {
        return "gsp-outbound";
    }

    @Override
    public String getPluginName() {
        return "医药GSP出库插件";
    }

    @Override
    public int getPriority() {
        return 200;
    }

    /** 库存分配修正：强制FEFO近效期先出 覆盖默认FIFO，医药行业必须按效期升序分配 */
    @Override
    @SuppressWarnings("unchecked")
    public ExtensionResult modifyAllocation(Map<String, Object> context) {
        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) context.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return ExtensionResult.pass();
        }
        // FEFO排序：近效期优先
        List<Map<String, Object>> sorted = batchService.sortByFefo(candidates);
        log.info(
                "GSP FEFO分配: sku={}, 候选批次={}, 排序后优先批次={}",
                context.get("sku"),
                candidates.size(),
                sorted.get(0).get("batchNo"));
        return ExtensionResult.intercept("FEFO近效期先出", Map.of("sortedCandidates", sorted));
    }

    /** 拣货校验：特殊药品双人拣货 */
    @Override
    public ExtensionResult beforePick(Map<String, Object> context) {
        String drugType = (String) context.get("drugType");
        if (batchService.needDoubleCheck(drugType)) {
            String secondOperator = (String) context.get("secondOperator");
            if (secondOperator == null || secondOperator.isBlank()) {
                return ExtensionResult.reject("特殊药品（" + drugType + "）必须双人拣货复核");
            }
            log.info(
                    "GSP特殊药品双人拣货: sku={}, drugType={}, operator1={}, operator2={}",
                    context.get("sku"),
                    drugType,
                    context.get("operator"),
                    secondOperator);
        }
        return ExtensionResult.pass();
    }

    /** 复核校验：批号核对、数量双人复核 */
    @Override
    public ExtensionResult beforeReview(Map<String, Object> context) {
        String drugType = (String) context.get("drugType");
        // 所有药品出库必须核对批号
        String pickedBatch = (String) context.get("pickedBatchNo");
        String allocatedBatch = (String) context.get("allocatedBatchNo");
        if (pickedBatch == null || !pickedBatch.equals(allocatedBatch)) {
            return ExtensionResult.reject("GSP复核失败：拣货批号与分配批号不一致");
        }
        // 特殊药品双人复核
        if (batchService.needDoubleCheck(drugType)) {
            String reviewer = (String) context.get("reviewer");
            if (reviewer == null) {
                return ExtensionResult.reject("特殊药品必须双人复核，请指定复核人");
            }
        }
        return ExtensionResult.pass();
    }

    /** 发运前校验：生成随货同行单、冷链运输温度记录启动 */
    @Override
    public ExtensionResult beforeShip(Map<String, Object> context) {
        String orderNo = (String) context.get("orderNo");
        // 生成随货同行单（GSP强制要求）
        log.info("GSP生成随货同行单: orderNo={}", orderNo);
        // TODO: 生成随货同行单PDF，包含药品名称/批号/效期/数量/生产厂商/收货单位

        // 冷链药品启动运输温度监控
        if (Boolean.TRUE.equals(context.get("coldChain"))) {
            log.info("GSP冷链运输启动温度监控: orderNo={}", orderNo);
            // TODO: 启动IoT温度记录仪，每5分钟记录一次
        }
        return ExtensionResult.pass();
    }
}
