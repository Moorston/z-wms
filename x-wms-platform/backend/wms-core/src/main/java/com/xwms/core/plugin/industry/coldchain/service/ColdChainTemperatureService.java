package com.xwms.core.plugin.industry.coldchain.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 食品冷链温度监控服务 温区标准： - 冷冻区：≤-18℃ - 冷藏区：0-4℃（生鲜/乳制品） - 恒温区：10-15℃（巧克力/葡萄酒） - 常温区：15-25℃
 * 核心能力：全程温度记录、断链检测、温区校验、异常预警
 */
@Slf4j
@Service
public class ColdChainTemperatureService {

    /** 温区温度范围 */
    private static final Map<String, double[]> ZONE_RANGE =
            Map.of(
                    "FROZEN", new double[] {-30.0, -18.0}, // 冷冻
                    "CHILLED", new double[] {0.0, 4.0}, // 冷藏
                    "CONSTANT", new double[] {10.0, 15.0}, // 恒温
                    "NORMAL", new double[] {15.0, 25.0} // 常温
                    );

    /** 断链判定：温度超标持续超过5分钟视为断链 */
    private static final long BROKEN_THRESHOLD_MINUTES = 5;

    /** 记录温度（入库/在库/出库/运输各环节） */
    public void record(String bizType, String bizNo, String zone, double temperature) {
        log.info(
                "冷链温度记录: type={}, no={}, zone={}, temp={}℃, time={}",
                bizType,
                bizNo,
                zone,
                temperature,
                LocalDateTime.now());
        // TODO: 持久化到wms_coldchain_temp_record
        checkTemperature(zone, temperature, bizNo);
    }

    /** 温度检查 */
    public void checkTemperature(String zone, double temperature, String bizNo) {
        double[] range = ZONE_RANGE.getOrDefault(zone, ZONE_RANGE.get("NORMAL"));
        if (temperature < range[0] || temperature > range[1]) {
            log.error(
                    "冷链温度超标: bizNo={}, zone={}, temp={}℃, 标准={}-{}℃",
                    bizNo,
                    zone,
                    temperature,
                    range[0],
                    range[1]);
            // TODO: 触发预警，记录断链开始时间
        }
    }

    /**
     * 断链检测
     *
     * @param records 温度记录列表（按时间排序）
     * @return 断链次数和总时长
     */
    public Map<String, Object> detectBrokenChain(String zone, List<Map<String, Object>> records) {
        double[] range = ZONE_RANGE.getOrDefault(zone, ZONE_RANGE.get("NORMAL"));
        int brokenCount = 0;
        long totalBrokenMinutes = 0;
        LocalDateTime brokenStart = null;

        for (Map<String, Object> rec : records) {
            double temp = ((Number) rec.get("temperature")).doubleValue();
            LocalDateTime time = (LocalDateTime) rec.get("recordTime");
            boolean outOfRange = temp < range[0] || temp > range[1];

            if (outOfRange && brokenStart == null) {
                brokenStart = time;
            } else if (!outOfRange && brokenStart != null) {
                long duration = java.time.Duration.between(brokenStart, time).toMinutes();
                if (duration >= BROKEN_THRESHOLD_MINUTES) {
                    brokenCount++;
                    totalBrokenMinutes += duration;
                }
                brokenStart = null;
            }
        }
        return Map.of("brokenCount", brokenCount, "totalBrokenMinutes", totalBrokenMinutes);
    }

    /** 校验商品温区与库位温区匹配 */
    public boolean validateZoneMatch(String productZone, String locationZone) {
        // 冷冻商品不能放冷藏，冷藏商品不能放常温
        int productLevel = zoneLevel(productZone);
        int locationLevel = zoneLevel(locationZone);
        return locationLevel <= productLevel; // 库位温区等级必须≤商品要求等级
    }

    private int zoneLevel(String zone) {
        return switch (zone) {
            case "FROZEN" -> 4;
            case "CHILLED" -> 3;
            case "CONSTANT" -> 2;
            case "NORMAL" -> 1;
            default -> 0;
        };
    }

    /** 保质期检查（食品FEFO） */
    public String checkShelfLife(LocalDate produceDate, int shelfLifeDays) {
        LocalDate expireDate = produceDate.plusDays(shelfLifeDays);
        long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expireDate);
        if (daysLeft < 0) return "EXPIRED";
        if (daysLeft <= shelfLifeDays * 0.1) return "NEAR_EXPIRE"; // 剩余10%
        if (daysLeft <= shelfLifeDays * 0.3) return "WARNING"; // 剩余30%
        return "NORMAL";
    }
}
