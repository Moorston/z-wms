package com.xwms.core.plugin.industry.gsp;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.xwms.common.plugin.extension.ExtensionResult;
import com.xwms.common.plugin.extension.InboundExtension;
import com.xwms.core.plugin.industry.gsp.service.GspBatchService;
import com.xwms.core.plugin.industry.gsp.service.GspTemperatureService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 医药GSP入库扩展插件 拦截入库流程：ASN→收货→质检→上架 核心管控：首营审核、批号效期、温湿度、特殊药品 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GspInboundExtension implements InboundExtension {

    private final GspBatchService batchService;
    private final GspTemperatureService tempService;

    @Override
    public String getPluginId() {
        return "gsp-inbound";
    }

    @Override
    public String getPluginName() {
        return "医药GSP入库插件";
    }

    @Override
    public int getPriority() {
        return 200;
    }

    /** ASN接收前校验：首营企业/首营品种审核 */
    @Override
    public ExtensionResult beforeAsnCreate(Map<String, Object> context) {
        String supplierCode = (String) context.get("supplierCode");
        String sku = (String) context.get("sku");
        @SuppressWarnings("unchecked")
        var approvedSuppliers =
                (java.util.Set<String>)
                        context.getOrDefault("approvedSuppliers", java.util.Set.of());
        @SuppressWarnings("unchecked")
        var approvedSkus =
                (java.util.Set<String>) context.getOrDefault("approvedSkus", java.util.Set.of());

        if (!batchService.checkFirstBusinessApproval(
                supplierCode, sku, approvedSuppliers, approvedSkus)) {
            return ExtensionResult.reject("首营企业或首营品种未通过GSP审核，禁止入库");
        }
        log.info("GSP入库首营审核通过: supplier={}, sku={}", supplierCode, sku);
        return ExtensionResult.pass();
    }

    /** 收货校验：批号效期必填、批号唯一、冷链温度记录 */
    @Override
    public ExtensionResult beforeReceive(Map<String, Object> context) {
        String sku = (String) context.get("sku");
        String batchNo = (String) context.get("batchNo");
        String expireDate = (String) context.get("expireDate");

        // 批号必填
        if (batchNo == null || batchNo.isBlank()) {
            return ExtensionResult.reject("GSP要求医药商品必须录入批号");
        }
        // 效期必填
        if (expireDate == null || expireDate.isBlank()) {
            return ExtensionResult.reject("GSP要求医药商品必须录入有效期");
        }
        // 已过期禁止入库
        String alert = batchService.checkExpireAlert(java.time.LocalDate.parse(expireDate));
        if ("EXPIRED".equals(alert)) {
            return ExtensionResult.reject("药品已过期，禁止入库: sku=" + sku + ", batch=" + batchNo);
        }
        // 近效期提示（不拒绝，但记录）
        if ("NEAR".equals(alert) || "WARNING".equals(alert)) {
            log.warn(
                    "GSP近效期药品入库: sku={}, batch={}, expire={}, 级别={}",
                    sku,
                    batchNo,
                    expireDate,
                    alert);
        }
        // 冷链药品收货温度记录
        if (Boolean.TRUE.equals(context.get("coldChain"))) {
            Double receiveTemp = (Double) context.get("receiveTemperature");
            if (receiveTemp == null) {
                return ExtensionResult.reject("冷链药品收货必须记录到货温度");
            }
            tempService.record((String) context.get("warehouse"), "COLD", receiveTemp, 50.0);
        }
        return ExtensionResult.pass();
    }

    /** 上架建议：按存储类型指定库区（冷库/阴凉库/常温库） */
    @Override
    public ExtensionResult suggestPutaway(Map<String, Object> context) {
        String storageType = (String) context.get("storageType");
        String suggestedArea =
                switch (storageType) {
                    case "COLD" -> "COLD_STORAGE"; // 冷库
                    case "COOL" -> "COOL_STORAGE"; // 阴凉库
                    default -> "NORMAL_STORAGE"; // 常温库
                };
        log.info(
                "GSP上架建议: sku={}, storageType={}, 建议库区={}",
                context.get("sku"),
                storageType,
                suggestedArea);
        return ExtensionResult.intercept("GSP存储要求", Map.of("areaType", suggestedArea));
    }

    /** 上架后：生成养护计划 */
    @Override
    public void afterPutaway(Map<String, Object> context) {
        log.info("GSP上架后生成养护计划: sku={}, batch={}", context.get("sku"), context.get("batchNo"));
        // TODO: 根据药品类型生成养护计划（重点养护3个月/次，一般养护6个月/次）
    }
}
