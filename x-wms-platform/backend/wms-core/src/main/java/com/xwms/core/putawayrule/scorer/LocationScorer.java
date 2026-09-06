package com.xwms.core.putawayrule.scorer;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.xwms.core.putawayrule.dto.PutawayRecommendContext;
import com.xwms.core.putawayrule.service.LocationQueryService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库位排序打分器 综合权重算法：距离(40%) + 容量利用率(30%) + 周转率匹配(20%) + 混放优先(10%) 分数越高越优先推荐 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationScorer {

    @Value("${putaway.scorer.distance-weight:0.4}")
    private double distanceWeight;

    @Value("${putaway.scorer.capacity-weight:0.3}")
    private double capacityWeight;

    @Value("${putaway.scorer.cycle-weight:0.2}")
    private double cycleWeight;

    @Value("${putaway.scorer.mix-weight:0.1}")
    private double mixWeight;

    private final LocationQueryService locationQueryService;

    /** 对候选库位进行打分排序 */
    public List<ScoredLocation> scoreAndSort(
            List<LocationQueryService.PutawayLocation> locations,
            PutawayRecommendContext context,
            LocationQueryService.PutawayLocation basePoint) {
        if (locations == null || locations.isEmpty()) {
            return List.of();
        }

        double maxDistance =
                locations.stream()
                        .mapToDouble(loc -> calculateDistance(loc, basePoint))
                        .max()
                        .orElse(1.0);
        if (maxDistance == 0) maxDistance = 1.0;

        final double finalMaxDistance = maxDistance;

        return locations.stream()
                .map(loc -> scoreLocation(loc, context, basePoint, finalMaxDistance))
                .sorted(Comparator.comparingDouble(ScoredLocation::getScore).reversed())
                .collect(Collectors.toList());
    }

    private ScoredLocation scoreLocation(
            LocationQueryService.PutawayLocation location,
            PutawayRecommendContext context,
            LocationQueryService.PutawayLocation basePoint,
            double maxDistance) {
        ScoredLocation result = new ScoredLocation();
        result.setLocation(location);

        double distance = calculateDistance(location, basePoint);
        double distanceScore = (1 - distance / maxDistance) * 100;
        result.setDistance(distance);
        result.setDistanceScore(distanceScore);

        double capacityScore = calculateCapacityScore(location);
        result.setCapacityScore(capacityScore);

        double cycleScore = calculateCycleScore(location, context);
        result.setCycleScore(cycleScore);

        double mixScore = calculateMixScore(location, context);
        result.setMixScore(mixScore);

        double totalScore =
                distanceScore * distanceWeight
                        + capacityScore * capacityWeight
                        + cycleScore * cycleWeight
                        + mixScore * mixWeight;
        result.setScore(totalScore);

        return result;
    }

    /** 计算欧氏距离（XYZ坐标） */
    public double calculateDistance(
            LocationQueryService.PutawayLocation location,
            LocationQueryService.PutawayLocation basePoint) {
        if (location.getCoordX() == null
                || location.getCoordY() == null
                || location.getCoordZ() == null
                || basePoint.getCoordX() == null
                || basePoint.getCoordY() == null
                || basePoint.getCoordZ() == null) {
            return location.getSortNo() != null ? location.getSortNo() : 100.0;
        }

        double dx = location.getCoordX() - basePoint.getCoordX();
        double dy = location.getCoordY() - basePoint.getCoordY();
        double dz = location.getCoordZ() - basePoint.getCoordZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private double calculateCapacityScore(LocationQueryService.PutawayLocation location) {
        if (location.getCapacity() == null
                || location.getUsedCapacity() == null
                || location.getCapacity().compareTo(java.math.BigDecimal.ZERO) == 0) {
            return 50.0;
        }
        double utilization =
                location.getUsedCapacity().doubleValue() / location.getCapacity().doubleValue();
        // 低利用率库位得分高，引导库存分散均衡负载（对齐文档 utilization 权重意图）
        return (1 - utilization) * 100;
    }

    private double calculateCycleScore(
            LocationQueryService.PutawayLocation location, PutawayRecommendContext context) {
        String cycleLevel = context.getCycleLevel();
        String areaCode = location.getAreaCode();
        if (cycleLevel == null || areaCode == null) {
            return 50.0;
        }
        if (areaCode.startsWith(cycleLevel)) {
            return 100.0;
        } else if (areaCode.startsWith("A") && "B".equals(cycleLevel)) {
            return 70.0;
        } else if (areaCode.startsWith("B") && "A".equals(cycleLevel)) {
            return 60.0;
        }
        return 30.0;
    }

    private double calculateMixScore(
            LocationQueryService.PutawayLocation location, PutawayRecommendContext context) {
        String currentSku = location.getCurrentSku();
        if (currentSku == null || currentSku.isEmpty()) {
            return 50.0;
        }
        if (currentSku.equals(context.getSkuCode())) {
            return 100.0;
        }
        return 0.0;
    }

    @Data
    public static class ScoredLocation {
        private LocationQueryService.PutawayLocation location;
        private double distance;
        private double distanceScore;
        private double capacityScore;
        private double cycleScore;
        private double mixScore;
        private double score;
    }
}
