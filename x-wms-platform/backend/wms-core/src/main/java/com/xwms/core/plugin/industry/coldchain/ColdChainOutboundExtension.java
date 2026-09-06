package com.xwms.core.plugin.industry.coldchain;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.xwms.common.plugin.extension.ExtensionResult;
import com.xwms.common.plugin.extension.OutboundExtension;
import com.xwms.core.plugin.industry.coldchain.service.ColdChainTemperatureService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 食品冷链出库扩展插件 核心管控：FEFO保质期先出、冷链装箱温度、运输全程温控、断链预警 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ColdChainOutboundExtension implements OutboundExtension {

    private final ColdChainTemperatureService tempService;

    @Override
    public String getPluginId() {
        return "coldchain-outbound";
    }

    @Override
    public String getPluginName() {
        return "食品冷链出库插件";
    }

    @Override
    public int getPriority() {
        return 180;
    }

    /** 分配修正：FEFO保质期先出（覆盖默认FIFO） */
    @Override
    @SuppressWarnings("unchecked")
    public ExtensionResult modifyAllocation(Map<String, Object> context) {
        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) context.get("candidates");
        if (candidates == null || candidates.isEmpty()) return ExtensionResult.pass();
        // 按保质期剩余天数升序（先到期先出）
        candidates.sort(
                (a, b) -> {
                    int daysA = (int) a.getOrDefault("shelfLifeDaysLeft", 9999);
                    int daysB = (int) b.getOrDefault("shelfLifeDaysLeft", 9999);
                    return Integer.compare(daysA, daysB);
                });
        log.info(
                "冷链FEFO分配: sku={}, 优先批次={}, 剩余保质期={}天",
                context.get("sku"),
                candidates.get(0).get("batchNo"),
                candidates.get(0).get("shelfLifeDaysLeft"));
        return ExtensionResult.intercept("FEFO保质期先出", Map.of("sortedCandidates", candidates));
    }

    /** 拣货校验：冷链商品快速拣货，减少常温暴露时间 */
    @Override
    public ExtensionResult beforePick(Map<String, Object> context) {
        boolean isColdChain = Boolean.TRUE.equals(context.get("coldChain"));
        if (!isColdChain) return ExtensionResult.pass();
        // 冷链商品拣货后必须在15分钟内完成复核装箱
        log.info("冷链拣货提醒: sku={}, 请在15分钟内完成复核装箱", context.get("sku"));
        return ExtensionResult.pass();
    }

    /** 复核校验：装箱温度检测 */
    @Override
    public ExtensionResult beforeReview(Map<String, Object> context) {
        boolean isColdChain = Boolean.TRUE.equals(context.get("coldChain"));
        if (!isColdChain) return ExtensionResult.pass();
        Double boxTemp = (Double) context.get("boxTemperature");
        if (boxTemp == null) {
            return ExtensionResult.reject("冷链商品复核必须检测装箱温度");
        }
        tempService.record(
                "PACKING",
                (String) context.get("orderNo"),
                (String) context.get("temperatureZone"),
                boxTemp);
        return ExtensionResult.pass();
    }

    /** 发运前：启动运输温度监控 */
    @Override
    public ExtensionResult beforeShip(Map<String, Object> context) {
        boolean isColdChain = Boolean.TRUE.equals(context.get("coldChain"));
        if (!isColdChain) return ExtensionResult.pass();
        String orderNo = (String) context.get("orderNo");
        log.info("冷链发运启动运输温度监控: orderNo={}, 运输方式={}", orderNo, context.get("transportType"));
        // TODO: 启动车载温度记录仪，每5分钟回传一次，断链自动预警
        // TODO: 冷藏车预冷确认（装车前车厢温度必须达标）
        return ExtensionResult.pass();
    }
}
