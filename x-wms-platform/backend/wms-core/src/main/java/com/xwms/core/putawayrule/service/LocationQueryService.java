package com.xwms.core.putawayrule.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/** 库位查询服务 用于上架规则的库位推荐，实际项目中应通过Feign调用wms-base的库位服务 当前为本地实现，模拟库位数据查询 */
@Slf4j
@Service
public class LocationQueryService {

    // ============================================================

    // 库位数据模型（上架推荐专用）
    // ============================================================

    @Data
    public static class PutawayLocation {
        private String locationCode;
        private String warehouseCode;
        private String areaCode;
        private String locationType; // STORAGE/PICK/RECEIVE/SHIP/QC
        private String temperatureZone;
        private String status; // EMPTY/NORMAL/FULL/FROZEN/DISABLED
        private BigDecimal capacity;
        private BigDecimal usedCapacity;
        private BigDecimal maxWeight;
        private Integer sortNo;
        private Integer coordX;
        private Integer coordY;
        private Integer coordZ;
        private String rowNo;
        private String columnNo;
        private String levelNo;
        private String locationAttrs; // JSON属性
        // 扩展字段
        private String currentSku; // 当前库位存放的SKU（混放判断用）
        private String currentBatch; // 当前库位存放的批次
        private BigDecimal currentQty; // 当前库存数量
        private java.time.LocalDate expiryDate; // 效期（FEFO用）
    }

    // ============================================================

    // 距离计算
    // ============================================================

    /** 计算两个库位之间的曼哈顿距离 曼哈顿距离 = |x1-x2| + |y1-y2| + |z1-z2| 适用于仓库巷道中的实际行走距离 */
    public int calculateManhattanDistance(PutawayLocation loc1, PutawayLocation loc2) {
        if (loc1 == null || loc2 == null) return Integer.MAX_VALUE;
        int dx = Math.abs(loc1.getCoordX() - loc2.getCoordX());
        int dy = Math.abs(loc1.getCoordY() - loc2.getCoordY());
        int dz = Math.abs(loc1.getCoordZ() - loc2.getCoordZ());
        return dx + dy + dz;
    }

    /** 计算两个库位之间的欧几里得距离 欧几里得距离 = sqrt((x1-x2)^2 + (y1-y2)^2 + (z1-z2)^2) */
    public double calculateEuclideanDistance(PutawayLocation loc1, PutawayLocation loc2) {
        if (loc1 == null || loc2 == null) return Double.MAX_VALUE;
        int dx = loc1.getCoordX() - loc2.getCoordX();
        int dy = loc1.getCoordY() - loc2.getCoordY();
        int dz = loc1.getCoordZ() - loc2.getCoordZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** 计算库位到收货区的距离 收货区通常坐标为(0,0,0)或指定的参考点 */
    public int calculateDistanceToReceiving(
            PutawayLocation location, PutawayLocation receivingPoint) {
        return calculateManhattanDistance(location, receivingPoint);
    }

    // ============================================================

    // 可用库位查询
    // ============================================================

    /**
     * 查询可用的上架库位
     *
     * @param warehouseCode 仓库编码
     * @param areaCode 库区编码（可选）
     * @param locationType 库位类型（可选，默认STORAGE）
     * @param temperatureZone 温区（可选）
     * @param requiredCapacity 需要的容量
     * @param requiredWeight 需要的承重
     * @param allowMix 是否允许混放
     * @param skuCode 商品编码（混放判断用）
     * @param batchNo 批次号（混批判断用）
     * @return 可用库位列表
     */
    public List<PutawayLocation> queryAvailableLocations(
            String warehouseCode,
            String areaCode,
            String locationType,
            String temperatureZone,
            BigDecimal requiredCapacity,
            BigDecimal requiredWeight,
            boolean allowMix,
            String skuCode,
            String batchNo) {
        log.debug(
                "查询可用库位: warehouse={}, area={}, type={}, tempZone={}",
                warehouseCode,
                areaCode,
                locationType,
                temperatureZone);

        // TODO: 实际项目中应通过Feign调用wms-base的库位服务查询
        // 当前返回模拟数据
        List<PutawayLocation> allLocations = generateMockLocations(warehouseCode);

        // 过滤条件
        return allLocations.stream()
                .filter(
                        loc ->
                                warehouseCode == null
                                        || warehouseCode.equals(loc.getWarehouseCode()))
                .filter(loc -> areaCode == null || areaCode.equals(loc.getAreaCode()))
                .filter(loc -> locationType == null || locationType.equals(loc.getLocationType()))
                .filter(
                        loc ->
                                temperatureZone == null
                                        || temperatureZone.equals(loc.getTemperatureZone()))
                .filter(loc -> "EMPTY".equals(loc.getStatus()) || "NORMAL".equals(loc.getStatus()))
                .filter(
                        loc ->
                                !"FULL".equals(loc.getStatus())
                                        && !"DISABLED".equals(loc.getStatus()))
                .filter(loc -> hasEnoughCapacity(loc, requiredCapacity))
                .filter(loc -> hasEnoughWeight(loc, requiredWeight))
                .filter(loc -> allowMix || canPutawayWithoutMix(loc, skuCode, batchNo))
                .sorted(Comparator.comparingInt(PutawayLocation::getSortNo))
                .collect(Collectors.toList());
    }

    /** 查询已有指定SKU和批次的库位（FIFO策略用） */
    public List<PutawayLocation> queryLocationsBySkuAndBatch(
            String warehouseCode, String skuCode, String batchNo) {
        List<PutawayLocation> allLocations = generateMockLocations(warehouseCode);
        return allLocations.stream()
                .filter(loc -> warehouseCode.equals(loc.getWarehouseCode()))
                .filter(loc -> skuCode.equals(loc.getCurrentSku()))
                .filter(loc -> batchNo == null || batchNo.equals(loc.getCurrentBatch()))
                .filter(loc -> !"FULL".equals(loc.getStatus()))
                .filter(loc -> !"DISABLED".equals(loc.getStatus()))
                .collect(Collectors.toList());
    }

    /** 查询指定库区的库位（ZONE策略用） */
    public List<PutawayLocation> queryLocationsByArea(String warehouseCode, String areaCode) {
        List<PutawayLocation> allLocations = generateMockLocations(warehouseCode);
        return allLocations.stream()
                .filter(loc -> warehouseCode.equals(loc.getWarehouseCode()))
                .filter(loc -> areaCode.equals(loc.getAreaCode()))
                .filter(loc -> "EMPTY".equals(loc.getStatus()) || "NORMAL".equals(loc.getStatus()))
                .filter(
                        loc ->
                                !"FULL".equals(loc.getStatus())
                                        && !"DISABLED".equals(loc.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * 查询指定高度范围的库位（HEIGHT策略用）
     *
     * @param minLevel 最小层
     * @param maxLevel 最大层
     */
    public List<PutawayLocation> queryLocationsByLevelRange(
            String warehouseCode, int minLevel, int maxLevel) {
        List<PutawayLocation> allLocations = generateMockLocations(warehouseCode);
        return allLocations.stream()
                .filter(loc -> warehouseCode.equals(loc.getWarehouseCode()))
                .filter(
                        loc -> {
                            int level = parseLevel(loc.getLevelNo());
                            return level >= minLevel && level <= maxLevel;
                        })
                .filter(loc -> "EMPTY".equals(loc.getStatus()) || "NORMAL".equals(loc.getStatus()))
                .filter(
                        loc ->
                                !"FULL".equals(loc.getStatus())
                                        && !"DISABLED".equals(loc.getStatus()))
                .collect(Collectors.toList());
    }

    // ============================================================

    // 辅助方法
    // ============================================================

    /** 检查库位是否有足够容量 */
    private boolean hasEnoughCapacity(PutawayLocation location, BigDecimal requiredCapacity) {
        if (requiredCapacity == null || requiredCapacity.compareTo(BigDecimal.ZERO) <= 0)
            return true;
        if (location.getCapacity() == null || location.getUsedCapacity() == null) return true;
        BigDecimal available = location.getCapacity().subtract(location.getUsedCapacity());
        return available.compareTo(requiredCapacity) >= 0;
    }

    /** 检查库位是否有足够承重 */
    private boolean hasEnoughWeight(PutawayLocation location, BigDecimal requiredWeight) {
        if (requiredWeight == null || requiredWeight.compareTo(BigDecimal.ZERO) <= 0) return true;
        if (location.getMaxWeight() == null) return true;
        return location.getMaxWeight().compareTo(requiredWeight) >= 0;
    }

    /** 检查是否可以不混放上架 如果库位为空，或者库位已有相同SKU和批次，则可以不混放 */
    private boolean canPutawayWithoutMix(PutawayLocation location, String skuCode, String batchNo) {
        if ("EMPTY".equals(location.getStatus())) return true;
        if (skuCode == null) return true;
        if (!skuCode.equals(location.getCurrentSku())) return false;
        if (batchNo == null) return true;
        return batchNo.equals(location.getCurrentBatch());
    }

    /** 解析层号为整数 */
    private int parseLevel(String levelNo) {
        if (levelNo == null) return 0;
        try {
            return Integer.parseInt(levelNo.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    /** 获取收货区参考点（距离计算基准） */
    public PutawayLocation getReceivingPoint(String warehouseCode) {
        PutawayLocation receiving = new PutawayLocation();
        receiving.setLocationCode("RECEIVING-01");
        receiving.setWarehouseCode(warehouseCode);
        receiving.setLocationType("RECEIVE");
        receiving.setCoordX(0);
        receiving.setCoordY(0);
        receiving.setCoordZ(0);
        return receiving;
    }

    // ============================================================

    // 模拟数据生成（实际项目中应替换为Feign调用）
    // ============================================================

    /** 生成模拟库位数据 实际项目中应通过Feign调用wms-base的LocationService查询 */
    private List<PutawayLocation> generateMockLocations(String warehouseCode) {
        List<PutawayLocation> locations = new ArrayList<>();

        // 生成A区库位（1-3排，1-5列，1-3层）
        for (int row = 1; row <= 3; row++) {
            for (int col = 1; col <= 5; col++) {
                for (int level = 1; level <= 3; level++) {
                    PutawayLocation loc = new PutawayLocation();
                    loc.setLocationCode(
                            String.format("%s-A-%02d-%02d-%02d", warehouseCode, row, col, level));
                    loc.setWarehouseCode(warehouseCode);
                    loc.setAreaCode("A");
                    loc.setLocationType("STORAGE");
                    loc.setTemperatureZone("NORMAL");
                    loc.setStatus((row == 1 && col == 1) ? "NORMAL" : "EMPTY");
                    loc.setCapacity(new BigDecimal("1000"));
                    loc.setUsedCapacity(
                            (row == 1 && col == 1) ? new BigDecimal("300") : BigDecimal.ZERO);
                    loc.setMaxWeight(new BigDecimal("500"));
                    loc.setSortNo(row * 100 + col * 10 + level);
                    loc.setCoordX(row * 10);
                    loc.setCoordY(col * 5);
                    loc.setCoordZ(level);
                    loc.setRowNo(String.valueOf(row));
                    loc.setColumnNo(String.valueOf(col));
                    loc.setLevelNo(String.valueOf(level));
                    if (row == 1 && col == 1) {
                        loc.setCurrentSku("SKU001");
                        loc.setCurrentBatch("BATCH20260101");
                        loc.setCurrentQty(new BigDecimal("100"));
                        loc.setExpiryDate(java.time.LocalDate.now().plusDays(365));
                    }
                    locations.add(loc);
                }
            }
        }

        // 生成B区库位（冷藏区）
        for (int row = 1; row <= 2; row++) {
            for (int col = 1; col <= 3; col++) {
                for (int level = 1; level <= 2; level++) {
                    PutawayLocation loc = new PutawayLocation();
                    loc.setLocationCode(
                            String.format("%s-B-%02d-%02d-%02d", warehouseCode, row, col, level));
                    loc.setWarehouseCode(warehouseCode);
                    loc.setAreaCode("B");
                    loc.setLocationType("STORAGE");
                    loc.setTemperatureZone("COLD");
                    loc.setStatus("EMPTY");
                    loc.setCapacity(new BigDecimal("800"));
                    loc.setUsedCapacity(BigDecimal.ZERO);
                    loc.setMaxWeight(new BigDecimal("400"));
                    loc.setSortNo(1000 + row * 100 + col * 10 + level);
                    loc.setCoordX(row * 10 + 50);
                    loc.setCoordY(col * 5);
                    loc.setCoordZ(level);
                    loc.setRowNo(String.valueOf(row));
                    loc.setColumnNo(String.valueOf(col));
                    loc.setLevelNo(String.valueOf(level));
                    locations.add(loc);
                }
            }
        }

        return locations;
    }
}
