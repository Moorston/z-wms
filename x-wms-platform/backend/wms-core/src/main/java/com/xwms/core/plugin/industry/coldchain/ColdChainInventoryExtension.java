package com.xwms.core.plugin.industry.coldchain;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.xwms.common.plugin.extension.ExtensionResult;
import com.xwms.common.plugin.extension.InventoryExtension;
import com.xwms.core.plugin.industry.coldchain.service.ColdChainTemperatureService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 食品冷链库存扩展插件 核心管控：在库温度实时监控、断链自动预警、温区库存隔离 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ColdChainInventoryExtension implements InventoryExtension {

    private final ColdChainTemperatureService tempService;

    @Override
    public String getPluginId() {
        return "coldchain-inventory";
    }

    @Override
    public String getPluginName() {
        return "食品冷链库存插件";
    }

    @Override
    public int getPriority() {
        return 180;
    }

    /** 库存预警：在库温度异常/断链预警 */
    @Override
    public void onInventoryAlert(Map<String, Object> context) {
        String zone = (String) context.get("temperatureZone");
        Double currentTemp = (Double) context.get("currentTemperature");
        if (currentTemp == null) return;
        tempService.checkTemperature(zone, currentTemp, (String) context.get("warehouse"));
    }

    /** 调拨校验：跨温区调拨必须评估 */
    @Override
    public ExtensionResult beforeTransfer(Map<String, Object> context) {
        String fromZone = (String) context.get("fromZone");
        String toZone = (String) context.get("toZone");
        if (fromZone != null && toZone != null && !fromZone.equals(toZone)) {
            // 冷冻→冷藏：需要解冻评估
            // 冷藏→常温：需要品质评估
            log.warn("冷链跨温区调拨: from={}, to={}, 需要品质部评估", fromZone, toZone);
            return ExtensionResult.intercept(
                    "跨温区调拨需评估", Map.of("needApproval", true, "approvalType", "QUALITY"));
        }
        return ExtensionResult.pass();
    }
}
