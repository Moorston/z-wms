package com.xwms.core.plugin.industry.gsp;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.xwms.common.plugin.extension.ExtensionResult;
import com.xwms.common.plugin.extension.InventoryExtension;
import com.xwms.core.plugin.industry.gsp.service.GspBatchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 医药GSP库存扩展插件 核心管控：效期预警、特殊药品库存管控、近效期锁定 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GspInventoryExtension implements InventoryExtension {

    private final GspBatchService batchService;

    @Override
    public String getPluginId() {
        return "gsp-inventory";
    }

    @Override
    public String getPluginName() {
        return "医药GSP库存插件";
    }

    @Override
    public int getPriority() {
        return 200;
    }

    /** 库存扣减前校验：禁止扣减已过期/近效期锁定库存 */
    @Override
    public ExtensionResult beforeDeduct(Map<String, Object> context) {
        String expireDate = (String) context.get("expireDate");
        if (expireDate != null) {
            String alert = batchService.checkExpireAlert(LocalDate.parse(expireDate));
            if ("EXPIRED".equals(alert)) {
                return ExtensionResult.reject("药品已过期，禁止出库扣减: batch=" + context.get("batchNo"));
            }
        }
        return ExtensionResult.pass();
    }

    /** 库存预警：近效期自动预警 */
    @Override
    public void onInventoryAlert(Map<String, Object> context) {
        String expireDate = (String) context.get("expireDate");
        if (expireDate == null) return;
        String alert = batchService.checkExpireAlert(LocalDate.parse(expireDate));
        switch (alert) {
            case "EXPIRED" ->
                    log.error(
                            "GSP药品已过期，请立即隔离: sku={}, batch={}",
                            context.get("sku"),
                            context.get("batchNo"));
            case "NEAR" ->
                    log.error(
                            "GSP药品近效期(30天内)，请优先促销或退回: sku={}, batch={}",
                            context.get("sku"),
                            context.get("batchNo"));
            case "WARNING" ->
                    log.warn(
                            "GSP药品效期预警(60天内): sku={}, batch={}",
                            context.get("sku"),
                            context.get("batchNo"));
            case "ATTENTION" ->
                    log.info(
                            "GSP药品效期关注(90天内): sku={}, batch={}",
                            context.get("sku"),
                            context.get("batchNo"));
            default -> {
                // 未知效期类型忽略
            }
        }
        // TODO: 近效期库存自动锁定，禁止分配，需质量部审批后才能出库
    }

    /** 盘点校验：特殊药品双人盘点 */
    @Override
    public ExtensionResult beforeStocktake(Map<String, Object> context) {
        String drugType = (String) context.get("drugType");
        if (batchService.isSpecialDrug(drugType)) {
            String secondCounter = (String) context.get("secondCounter");
            if (secondCounter == null) {
                return ExtensionResult.reject("特殊药品必须双人盘点，请指定第二盘点人");
            }
        }
        return ExtensionResult.pass();
    }
}
