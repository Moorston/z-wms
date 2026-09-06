package com.xwms.core.plugin.industry.coldchain;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.xwms.common.plugin.extension.ExtensionResult;
import com.xwms.common.plugin.extension.InboundExtension;
import com.xwms.core.plugin.industry.coldchain.service.ColdChainTemperatureService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 食品冷链入库扩展插件 核心管控：到货温度检测、温区校验、保质期登记、断链记录 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ColdChainInboundExtension implements InboundExtension {

    private final ColdChainTemperatureService tempService;

    @Override
    public String getPluginId() {
        return "coldchain-inbound";
    }

    @Override
    public String getPluginName() {
        return "食品冷链入库插件";
    }

    @Override
    public int getPriority() {
        return 180;
    }

    /** 收货校验：冷链商品必须检测到货温度 */
    @Override
    public ExtensionResult beforeReceive(Map<String, Object> context) {
        boolean isColdChain = Boolean.TRUE.equals(context.get("coldChain"));
        if (!isColdChain) return ExtensionResult.pass();

        String zone = (String) context.get("temperatureZone");
        Double arriveTemp = (Double) context.get("arriveTemperature");
        if (arriveTemp == null) {
            return ExtensionResult.reject("冷链商品收货必须检测到货温度");
        }
        // 记录到货温度
        tempService.record("INBOUND", (String) context.get("asnNo"), zone, arriveTemp);
        // 温度超标拒收
        double[] range = getZoneRange(zone);
        if (arriveTemp < range[0] || arriveTemp > range[1]) {
            log.error(
                    "冷链到货温度超标拒收: asn={}, zone={}, temp={}℃",
                    context.get("asnNo"),
                    zone,
                    arriveTemp);
            return ExtensionResult.reject(
                    "冷链到货温度超标（" + arriveTemp + "℃），标准" + range[0] + "~" + range[1] + "℃，建议拒收或隔离评估");
        }
        // 保质期必填
        if (context.get("produceDate") == null || context.get("shelfLifeDays") == null) {
            return ExtensionResult.reject("食品必须录入生产日期和保质期");
        }
        return ExtensionResult.pass();
    }

    /** 上架建议：按温区指定库位 */
    @Override
    public ExtensionResult suggestPutaway(Map<String, Object> context) {
        String zone = (String) context.get("temperatureZone");
        if (zone == null) return ExtensionResult.pass();
        String areaType =
                switch (zone) {
                    case "FROZEN" -> "FROZEN_AREA";
                    case "CHILLED" -> "CHILLED_AREA";
                    case "CONSTANT" -> "CONSTANT_AREA";
                    default -> "NORMAL_AREA";
                };
        log.info("冷链上架建议: sku={}, zone={}, 建议库区={}", context.get("sku"), zone, areaType);
        return ExtensionResult.intercept("冷链温区要求", Map.of("areaType", areaType));
    }

    private double[] getZoneRange(String zone) {
        return switch (zone) {
            case "FROZEN" -> new double[] {-30.0, -18.0};
            case "CHILLED" -> new double[] {0.0, 4.0};
            case "CONSTANT" -> new double[] {10.0, 15.0};
            default -> new double[] {15.0, 25.0};
        };
    }
}
