package com.xwms.core.plugin.industry.ecommerce.service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 电商大促波次优化服务 大促场景特点：订单量暴增（日常10倍+），SKU集中，快递单一 优化策略： 1. 按快递聚合（同一快递同一波次，减少交接） 2.
 * 按区域聚合（同一区域同一波次，优化拣货路径） 3. 按爆款SKU聚合（爆款单独波次，批量拣货） 4. 极速出库模式（拣货即打单即发运，跳过复核） 5. 波次容量动态调整（大促期间增大波次容量）
 */
@Slf4j
@Service
public class EcommerceWaveService {

    /** 大促波次容量（日常100，大促500） */
    private static final int PROMO_WAVE_SIZE = 500;

    /** 日常波次容量 */
    private static final int NORMAL_WAVE_SIZE = 100;

    /** 大促波次分组 优先级：快递 > 区域 > 爆款SKU */
    public List<Map<String, Object>> groupPromoWaves(
            List<Map<String, Object>> orders, boolean isPromo) {
        int waveSize = isPromo ? PROMO_WAVE_SIZE : NORMAL_WAVE_SIZE;
        log.info("大促波次分组: 订单数={}, 波次容量={}", orders.size(), waveSize);

        // 1. 按快递分组
        Map<String, List<Map<String, Object>>> byExpress =
                orders.stream()
                        .collect(
                                Collectors.groupingBy(
                                        o -> (String) o.getOrDefault("expressCode", "DEFAULT")));

        List<Map<String, Object>> waves = new ArrayList<>();
        int waveSeq = 1;

        for (Map.Entry<String, List<Map<String, Object>>> entry : byExpress.entrySet()) {
            String express = entry.getKey();
            List<Map<String, Object>> expressOrders = entry.getValue();

            // 2. 按区域分组
            Map<String, List<Map<String, Object>>> byRegion =
                    expressOrders.stream()
                            .collect(
                                    Collectors.groupingBy(
                                            o -> (String) o.getOrDefault("region", "DEFAULT")));

            for (Map.Entry<String, List<Map<String, Object>>> regionEntry : byRegion.entrySet()) {
                List<Map<String, Object>> regionOrders = regionEntry.getValue();

                // 3. 按容量拆波次
                for (int i = 0; i < regionOrders.size(); i += waveSize) {
                    List<Map<String, Object>> subList =
                            regionOrders.subList(i, Math.min(i + waveSize, regionOrders.size()));
                    Map<String, Object> wave = new HashMap<>();
                    wave.put("waveNo", "PROMO-" + System.currentTimeMillis() + "-" + waveSeq++);
                    wave.put("expressCode", express);
                    wave.put("region", regionEntry.getKey());
                    wave.put("orderCount", subList.size());
                    wave.put("orders", subList);
                    wave.put("waveType", "PROMO");
                    waves.add(wave);
                }
            }
        }
        log.info("大促波次分组完成: 生成波次数={}", waves.size());
        return waves;
    }

    /** 爆款SKU识别（订单中出现频率Top10） */
    public List<String> identifyHotSkus(List<Map<String, Object>> orders, int topN) {
        Map<String, Long> skuCount =
                orders.stream()
                        .flatMap(o -> ((List<String>) o.getOrDefault("skus", List.of())).stream())
                        .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        return skuCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /** 极速出库模式判断 条件：单一SKU + 已预占 + 快递已确定 + 非贵重商品 */
    public boolean isFastShipMode(Map<String, Object> order) {
        List<?> skus = (List<?>) order.getOrDefault("skus", List.of());
        boolean singleSku = skus.size() == 1;
        boolean preAllocated = Boolean.TRUE.equals(order.get("preAllocated"));
        boolean expressConfirmed = order.get("expressCode") != null;
        boolean notValuable = !Boolean.TRUE.equals(order.get("valuable"));
        return singleSku && preAllocated && expressConfirmed && notValuable;
    }
}
