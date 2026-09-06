package com.xwms.core.putawayrule.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.putawayrule.entity.*;
import com.xwms.core.putawayrule.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 上架规则服务 负责上架规则管理、策略管理、库位推荐算法、上架执行 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PutawayRuleService {

    private final PutawayRuleMapper ruleMapper;
    private final PutawayStrategyMapper strategyMapper;
    private final PutawayLogMapper logMapper;
    private final PutawayDetailMapper detailMapper;
    private final PutawayRuleLineMapper ruleLineMapper;
    private final LocationQueryService locationQueryService;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 上架规则管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public PutawayRule createRule(PutawayRule rule) {
        rule.setRuleCode(generateRuleCode());
        rule.setStatus("ACTIVE");
        rule.setCreatedTime(LocalDateTime.now());
        ruleMapper.insert(rule);
        log.info("创建上架规则: {}", rule.getRuleCode());
        return rule;
    }

    @Transactional(rollbackFor = Exception.class)
    public PutawayRule updateRule(PutawayRule rule) {
        rule.setUpdatedTime(LocalDateTime.now());
        ruleMapper.updateById(rule);
        return ruleMapper.selectById(rule.getId());
    }

    public PutawayRule getRule(Long id) {
        return ruleMapper.selectById(id);
    }

    public PutawayRule getRuleByCode(String ruleCode) {
        return ruleMapper.selectByRuleCode(ruleCode);
    }

    public Page<PutawayRule> pageRules(
            Page<PutawayRule> page, String ruleType, String status, String warehouseCode) {
        LambdaQueryWrapper<PutawayRule> wrapper = new LambdaQueryWrapper<>();
        if (ruleType != null) wrapper.eq(PutawayRule::getRuleType, ruleType);
        if (status != null) wrapper.eq(PutawayRule::getStatus, status);
        if (warehouseCode != null) wrapper.eq(PutawayRule::getWarehouseCode, warehouseCode);
        wrapper.orderByAsc(PutawayRule::getPriority);
        return ruleMapper.selectPage(page, wrapper);
    }

    public List<PutawayRule> getAllEnabledRules() {
        return ruleMapper.selectAllEnabled();
    }

    /** 按仓库+货主查询匹配的启用规则（按优先级排序） 替代全量加载，提升推荐引擎性能 */
    public List<PutawayRule> getMatchedRules(
            String warehouseCode, String ownerCode, String skuCode) {
        return ruleMapper.selectMatchedRules(skuCode, null, ownerCode, warehouseCode);
    }

    // ============================================================

    // 2. 上架策略管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public PutawayStrategy createStrategy(PutawayStrategy strategy) {
        strategy.setStrategyCode(generateStrategyCode());
        strategy.setStatus("ACTIVE");
        strategy.setCreatedTime(LocalDateTime.now());
        strategyMapper.insert(strategy);
        log.info("创建上架策略: {}", strategy.getStrategyCode());
        return strategy;
    }

    public PutawayStrategy getStrategy(Long id) {
        return strategyMapper.selectById(id);
    }

    public PutawayStrategy getStrategyByCode(String strategyCode) {
        return strategyMapper.selectByStrategyCode(strategyCode);
    }

    public List<PutawayStrategy> getStrategiesByRuleCode(String ruleCode) {
        return strategyMapper.selectByRuleCode(ruleCode);
    }

    public Page<PutawayStrategy> pageStrategies(
            Page<PutawayStrategy> page, String strategyType, String status) {
        LambdaQueryWrapper<PutawayStrategy> wrapper = new LambdaQueryWrapper<>();
        if (strategyType != null) wrapper.eq(PutawayStrategy::getStrategyType, strategyType);
        if (status != null) wrapper.eq(PutawayStrategy::getStatus, status);
        wrapper.orderByAsc(PutawayStrategy::getStrategyCode);
        return strategyMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 3. 库位推荐算法（核心）
    // ============================================================

    /**
     * 推荐上架库位
     *
     * @param skuCode 商品编码
     * @param categoryCode 品类编码
     * @param ownerCode 货主编码
     * @param warehouseCode 仓库编码
     * @param batchNo 批次号
     * @param quantity 上架数量
     * @return 推荐的库位列表（按推荐度排序）
     */
    public List<PutawayDetail> recommendLocations(
            String skuCode,
            String categoryCode,
            String ownerCode,
            String warehouseCode,
            String batchNo,
            BigDecimal quantity) {
        long startTime = System.currentTimeMillis();
        log.info(
                "开始推荐上架库位: sku={}, owner={}, warehouse={}, qty={}",
                skuCode,
                ownerCode,
                warehouseCode,
                quantity);

        // 1. 匹配上架规则
        List<PutawayRule> matchedRules =
                ruleMapper.selectMatchedRules(skuCode, categoryCode, ownerCode, warehouseCode);
        if (matchedRules.isEmpty()) {
            log.warn("未找到匹配的上架规则，使用默认策略");
            matchedRules = getDefaultRules(warehouseCode);
        }

        // 2. 获取最优规则
        PutawayRule bestRule = matchedRules.get(0);
        log.info("匹配到上架规则: {} (策略: {})", bestRule.getRuleCode(), bestRule.getStrategy());

        // 3. 根据策略推荐库位
        List<PutawayDetail> recommendations = new ArrayList<>();
        switch (bestRule.getStrategy()) {
            case "NEAREST":
                recommendations =
                        recommendByNearest(
                                skuCode, ownerCode, warehouseCode, batchNo, quantity, bestRule);
                break;
            case "FIFO":
                recommendations =
                        recommendByFIFO(
                                skuCode, ownerCode, warehouseCode, batchNo, quantity, bestRule);
                break;
            case "FEFO":
                recommendations =
                        recommendByFEFO(
                                skuCode, ownerCode, warehouseCode, batchNo, quantity, bestRule);
                break;
            case "ZONE":
                recommendations =
                        recommendByZone(
                                skuCode, ownerCode, warehouseCode, batchNo, quantity, bestRule);
                break;
            case "HEIGHT":
                recommendations =
                        recommendByHeight(
                                skuCode, ownerCode, warehouseCode, batchNo, quantity, bestRule);
                break;
            case "WEIGHT":
                recommendations =
                        recommendByWeight(
                                skuCode, ownerCode, warehouseCode, batchNo, quantity, bestRule);
                break;
            default:
                recommendations =
                        recommendByNearest(
                                skuCode, ownerCode, warehouseCode, batchNo, quantity, bestRule);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("推荐上架库位完成: 推荐{}个库位, 耗时{}ms", recommendations.size(), duration);
        return recommendations;
    }

    /** 按最近距离推荐（NEAREST） 算法：查询所有可用库位，计算到收货区的曼哈顿距离，按距离升序排序 适用场景：通用商品，优先减少搬运距离 */
    private List<PutawayDetail> recommendByNearest(
            String skuCode,
            String ownerCode,
            String warehouseCode,
            String batchNo,
            BigDecimal quantity,
            PutawayRule rule) {
        log.info("执行NEAREST最近距离推荐: sku={}, warehouse={}", skuCode, warehouseCode);

        // 1. 查询可用库位
        boolean allowMix = "Y".equals(rule.getAllowMix());
        List<LocationQueryService.PutawayLocation> availableLocations =
                locationQueryService.queryAvailableLocations(
                        warehouseCode,
                        null,
                        "STORAGE",
                        null,
                        null,
                        null,
                        allowMix,
                        skuCode,
                        batchNo);

        if (availableLocations.isEmpty()) {
            log.warn("NEAREST推荐: 未找到可用库位");
            return new ArrayList<>();
        }

        // 2. 获取收货区参考点
        LocationQueryService.PutawayLocation receivingPoint =
                locationQueryService.getReceivingPoint(warehouseCode);

        // 3. 计算每个库位到收货区的距离，按距离升序排序
        availableLocations.sort(
                (loc1, loc2) -> {
                    int dist1 =
                            locationQueryService.calculateDistanceToReceiving(loc1, receivingPoint);
                    int dist2 =
                            locationQueryService.calculateDistanceToReceiving(loc2, receivingPoint);
                    return Integer.compare(dist1, dist2);
                });

        // 4. 转换为上架详情，取前5个推荐
        return convertToPutawayDetails(
                availableLocations,
                skuCode,
                ownerCode,
                warehouseCode,
                batchNo,
                quantity,
                rule,
                "NEAREST",
                5);
    }

    /** 按先进先出推荐（FIFO） 算法：优先放入已有同商品同批次的库位（合并库存），其次选择空库位 适用场景：批次管理商品，减少库位碎片化，同批次集中存放 */
    private List<PutawayDetail> recommendByFIFO(
            String skuCode,
            String ownerCode,
            String warehouseCode,
            String batchNo,
            BigDecimal quantity,
            PutawayRule rule) {
        log.info("执行FIFO先进先出推荐: sku={}, batch={}, warehouse={}", skuCode, batchNo, warehouseCode);

        List<PutawayDetail> recommendations = new ArrayList<>();

        // 1. 优先查询已有同SKU同批次的库位（合并库存）
        List<LocationQueryService.PutawayLocation> sameBatchLocations =
                locationQueryService.queryLocationsBySkuAndBatch(warehouseCode, skuCode, batchNo);

        if (!sameBatchLocations.isEmpty()) {
            log.info("FIFO推荐: 找到{}个同批次库位，优先合并", sameBatchLocations.size());
            recommendations.addAll(
                    convertToPutawayDetails(
                            sameBatchLocations,
                            skuCode,
                            ownerCode,
                            warehouseCode,
                            batchNo,
                            quantity,
                            rule,
                            "FIFO",
                            3));
        }

        // 2. 其次查询空库位（新批次上架）
        List<LocationQueryService.PutawayLocation> emptyLocations =
                locationQueryService
                        .queryAvailableLocations(
                                warehouseCode,
                                null,
                                "STORAGE",
                                null,
                                null,
                                null,
                                false,
                                skuCode,
                                batchNo)
                        .stream()
                        .filter(loc -> "EMPTY".equals(loc.getStatus()))
                        .collect(java.util.stream.Collectors.toList());

        if (!emptyLocations.isEmpty()) {
            // 空库位按距离排序
            LocationQueryService.PutawayLocation receivingPoint =
                    locationQueryService.getReceivingPoint(warehouseCode);
            emptyLocations.sort(
                    (loc1, loc2) -> {
                        int dist1 =
                                locationQueryService.calculateDistanceToReceiving(
                                        loc1, receivingPoint);
                        int dist2 =
                                locationQueryService.calculateDistanceToReceiving(
                                        loc2, receivingPoint);
                        return Integer.compare(dist1, dist2);
                    });
            recommendations.addAll(
                    convertToPutawayDetails(
                            emptyLocations,
                            skuCode,
                            ownerCode,
                            warehouseCode,
                            batchNo,
                            quantity,
                            rule,
                            "FIFO",
                            2));
        }

        if (recommendations.isEmpty()) {
            log.warn("FIFO推荐: 未找到合适库位");
        }

        return recommendations;
    }

    /** 按先到期先出推荐（FEFO） 算法：优先放入效期最早的库位（确保先出库），其次选择空库位 适用场景：食品、医药等效期敏感商品，确保先进先出 */
    private List<PutawayDetail> recommendByFEFO(
            String skuCode,
            String ownerCode,
            String warehouseCode,
            String batchNo,
            BigDecimal quantity,
            PutawayRule rule) {
        log.info("执行FEFO先到期先出推荐: sku={}, batch={}, warehouse={}", skuCode, batchNo, warehouseCode);

        List<PutawayDetail> recommendations = new ArrayList<>();

        // 1. 查询已有同SKU的库位，按效期升序排序（效期早的优先）
        List<LocationQueryService.PutawayLocation> sameSkuLocations =
                locationQueryService.queryLocationsBySkuAndBatch(warehouseCode, skuCode, null);

        if (!sameSkuLocations.isEmpty()) {
            // 按效期升序排序（效期早的排在前面，确保先出库）
            sameSkuLocations.sort(
                    (loc1, loc2) -> {
                        if (loc1.getExpiryDate() == null) return 1;
                        if (loc2.getExpiryDate() == null) return -1;
                        return loc1.getExpiryDate().compareTo(loc2.getExpiryDate());
                    });
            log.info("FEFO推荐: 找到{}个同SKU库位，按效期排序", sameSkuLocations.size());
            recommendations.addAll(
                    convertToPutawayDetails(
                            sameSkuLocations,
                            skuCode,
                            ownerCode,
                            warehouseCode,
                            batchNo,
                            quantity,
                            rule,
                            "FEFO",
                            3));
        }

        // 2. 其次查询空库位
        List<LocationQueryService.PutawayLocation> emptyLocations =
                locationQueryService
                        .queryAvailableLocations(
                                warehouseCode,
                                null,
                                "STORAGE",
                                null,
                                null,
                                null,
                                false,
                                skuCode,
                                batchNo)
                        .stream()
                        .filter(loc -> "EMPTY".equals(loc.getStatus()))
                        .collect(java.util.stream.Collectors.toList());

        if (!emptyLocations.isEmpty()) {
            recommendations.addAll(
                    convertToPutawayDetails(
                            emptyLocations,
                            skuCode,
                            ownerCode,
                            warehouseCode,
                            batchNo,
                            quantity,
                            rule,
                            "FEFO",
                            2));
        }

        if (recommendations.isEmpty()) {
            log.warn("FEFO推荐: 未找到合适库位");
        }

        return recommendations;
    }

    /** 按区域优先推荐（ZONE） 算法：优先推荐规则指定库区的库位，其次推荐其他库区的库位 适用场景：按品类/货主/温区划分存储区域的仓库 */
    private List<PutawayDetail> recommendByZone(
            String skuCode,
            String ownerCode,
            String warehouseCode,
            String batchNo,
            BigDecimal quantity,
            PutawayRule rule) {
        log.info(
                "执行ZONE区域优先推荐: sku={}, warehouse={}, rule={}",
                skuCode,
                warehouseCode,
                rule.getRuleCode());

        List<PutawayDetail> recommendations = new ArrayList<>();

        // 1. 获取规则指定的优先库区（从规则配置中读取，这里模拟为A区）
        String preferredArea = rule.getPreferredArea() != null ? rule.getPreferredArea() : "A";
        log.info("ZONE推荐: 优先库区={}", preferredArea);

        // 2. 优先查询指定库区的可用库位
        List<LocationQueryService.PutawayLocation> zoneLocations =
                locationQueryService.queryLocationsByArea(warehouseCode, preferredArea);

        if (!zoneLocations.isEmpty()) {
            log.info("ZONE推荐: 在{}区找到{}个可用库位", preferredArea, zoneLocations.size());
            // 按距离排序
            LocationQueryService.PutawayLocation receivingPoint =
                    locationQueryService.getReceivingPoint(warehouseCode);
            zoneLocations.sort(
                    (loc1, loc2) -> {
                        int dist1 =
                                locationQueryService.calculateDistanceToReceiving(
                                        loc1, receivingPoint);
                        int dist2 =
                                locationQueryService.calculateDistanceToReceiving(
                                        loc2, receivingPoint);
                        return Integer.compare(dist1, dist2);
                    });
            recommendations.addAll(
                    convertToPutawayDetails(
                            zoneLocations,
                            skuCode,
                            ownerCode,
                            warehouseCode,
                            batchNo,
                            quantity,
                            rule,
                            "ZONE",
                            3));
        }

        // 3. 其次查询其他库区的可用库位
        if (recommendations.size() < 3) {
            List<LocationQueryService.PutawayLocation> otherLocations =
                    locationQueryService
                            .queryAvailableLocations(
                                    warehouseCode,
                                    null,
                                    "STORAGE",
                                    null,
                                    null,
                                    null,
                                    "Y".equals(rule.getAllowMix()),
                                    skuCode,
                                    batchNo)
                            .stream()
                            .filter(loc -> !preferredArea.equals(loc.getAreaCode()))
                            .collect(java.util.stream.Collectors.toList());

            if (!otherLocations.isEmpty()) {
                log.info("ZONE推荐: 在其他库区找到{}个可用库位", otherLocations.size());
                recommendations.addAll(
                        convertToPutawayDetails(
                                otherLocations,
                                skuCode,
                                ownerCode,
                                warehouseCode,
                                batchNo,
                                quantity,
                                rule,
                                "ZONE",
                                2));
            }
        }

        if (recommendations.isEmpty()) {
            log.warn("ZONE推荐: 未找到合适库位");
        }

        return recommendations;
    }

    /**
     * 按高度优先推荐（HEIGHT） 算法：根据商品重量推荐合适高度的库位 - 重货（>20kg）：推荐低层（1-2层），减少搬运难度 - 中货（5-20kg）：推荐中层（2-3层） -
     * 轻货（<5kg）：推荐高层（3层以上），充分利用空间 适用场景：有重量差异的商品，优化搬运效率和空间利用率
     */
    private List<PutawayDetail> recommendByHeight(
            String skuCode,
            String ownerCode,
            String warehouseCode,
            String batchNo,
            BigDecimal quantity,
            PutawayRule rule) {
        log.info("执行HEIGHT高度优先推荐: sku={}, warehouse={}", skuCode, warehouseCode);

        // 1. 根据商品重量确定推荐的层范围（这里模拟商品重量为10kg）
        BigDecimal productWeight =
                rule.getProductWeight() != null ? rule.getProductWeight() : new BigDecimal("10");
        int minLevel, maxLevel;
        String weightCategory;

        if (productWeight.compareTo(new BigDecimal("20")) > 0) {
            // 重货：低层
            minLevel = 1;
            maxLevel = 2;
            weightCategory = "重货";
        } else if (productWeight.compareTo(new BigDecimal("5")) >= 0) {
            // 中货：中层
            minLevel = 2;
            maxLevel = 3;
            weightCategory = "中货";
        } else {
            // 轻货：高层
            minLevel = 3;
            maxLevel = 5;
            weightCategory = "轻货";
        }

        log.info(
                "HEIGHT推荐: 商品重量={}kg, 分类={}, 推荐层={}-{}",
                productWeight,
                weightCategory,
                minLevel,
                maxLevel);

        // 2. 查询指定高度范围的可用库位
        List<LocationQueryService.PutawayLocation> heightLocations =
                locationQueryService.queryLocationsByLevelRange(warehouseCode, minLevel, maxLevel);

        if (heightLocations.isEmpty()) {
            log.warn("HEIGHT推荐: 在{}-{}层未找到可用库位，扩大范围查询", minLevel, maxLevel);
            // 扩大范围查询所有可用库位
            heightLocations =
                    locationQueryService.queryAvailableLocations(
                            warehouseCode,
                            null,
                            "STORAGE",
                            null,
                            null,
                            productWeight,
                            "Y".equals(rule.getAllowMix()),
                            skuCode,
                            batchNo);
        }

        // 3. 按距离排序
        LocationQueryService.PutawayLocation receivingPoint =
                locationQueryService.getReceivingPoint(warehouseCode);
        heightLocations.sort(
                (loc1, loc2) -> {
                    int dist1 =
                            locationQueryService.calculateDistanceToReceiving(loc1, receivingPoint);
                    int dist2 =
                            locationQueryService.calculateDistanceToReceiving(loc2, receivingPoint);
                    return Integer.compare(dist1, dist2);
                });

        return convertToPutawayDetails(
                heightLocations,
                skuCode,
                ownerCode,
                warehouseCode,
                batchNo,
                quantity,
                rule,
                "HEIGHT",
                5);
    }

    /**
     * 按重量优先推荐（WEIGHT） 算法：重货就近放（减少搬运距离），轻货可远放（充分利用远端空间） - 重货（>20kg）：只推荐距离收货区最近的20%库位 -
     * 中货（5-20kg）：推荐距离收货区最近的50%库位 - 轻货（<5kg）：推荐所有可用库位 适用场景：重量差异大的商品，平衡搬运效率和空间利用率
     */
    private List<PutawayDetail> recommendByWeight(
            String skuCode,
            String ownerCode,
            String warehouseCode,
            String batchNo,
            BigDecimal quantity,
            PutawayRule rule) {
        log.info("执行WEIGHT重量优先推荐: sku={}, warehouse={}", skuCode, warehouseCode);

        // 1. 根据商品重量确定距离阈值
        BigDecimal productWeight =
                rule.getProductWeight() != null ? rule.getProductWeight() : new BigDecimal("10");
        double distancePercentile;
        String weightCategory;

        if (productWeight.compareTo(new BigDecimal("20")) > 0) {
            // 重货：只推荐最近的20%
            distancePercentile = 0.2;
            weightCategory = "重货";
        } else if (productWeight.compareTo(new BigDecimal("5")) >= 0) {
            // 中货：推荐最近的50%
            distancePercentile = 0.5;
            weightCategory = "中货";
        } else {
            // 轻货：推荐所有
            distancePercentile = 1.0;
            weightCategory = "轻货";
        }

        log.info(
                "WEIGHT推荐: 商品重量={}kg, 分类={}, 距离百分位={}",
                productWeight,
                weightCategory,
                distancePercentile);

        // 2. 查询所有可用库位
        List<LocationQueryService.PutawayLocation> availableLocations =
                locationQueryService.queryAvailableLocations(
                        warehouseCode,
                        null,
                        "STORAGE",
                        null,
                        null,
                        productWeight,
                        "Y".equals(rule.getAllowMix()),
                        skuCode,
                        batchNo);

        if (availableLocations.isEmpty()) {
            log.warn("WEIGHT推荐: 未找到可用库位");
            return new ArrayList<>();
        }

        // 3. 计算每个库位到收货区的距离，按距离升序排序
        LocationQueryService.PutawayLocation receivingPoint =
                locationQueryService.getReceivingPoint(warehouseCode);
        availableLocations.sort(
                (loc1, loc2) -> {
                    int dist1 =
                            locationQueryService.calculateDistanceToReceiving(loc1, receivingPoint);
                    int dist2 =
                            locationQueryService.calculateDistanceToReceiving(loc2, receivingPoint);
                    return Integer.compare(dist1, dist2);
                });

        // 4. 根据重量分类筛选库位
        int limit = (int) Math.ceil(availableLocations.size() * distancePercentile);
        List<LocationQueryService.PutawayLocation> filteredLocations =
                availableLocations.subList(0, Math.min(limit, availableLocations.size()));

        log.info("WEIGHT推荐: 从{}个库位中筛选出{}个库位", availableLocations.size(), filteredLocations.size());

        return convertToPutawayDetails(
                filteredLocations,
                skuCode,
                ownerCode,
                warehouseCode,
                batchNo,
                quantity,
                rule,
                "WEIGHT",
                5);
    }

    /**
     * 将库位列表转换为上架详情列表
     *
     * @param locations 库位列表
     * @param maxCount 最大推荐数量
     */
    private List<PutawayDetail> convertToPutawayDetails(
            List<LocationQueryService.PutawayLocation> locations,
            String skuCode,
            String ownerCode,
            String warehouseCode,
            String batchNo,
            BigDecimal quantity,
            PutawayRule rule,
            String strategy,
            int maxCount) {
        List<PutawayDetail> details = new ArrayList<>();
        int count = Math.min(locations.size(), maxCount);

        for (int i = 0; i < count; i++) {
            LocationQueryService.PutawayLocation loc = locations.get(i);
            PutawayDetail detail = new PutawayDetail();
            detail.setDetailNo(generateDetailNo());
            detail.setSkuCode(skuCode);
            detail.setBatchNo(batchNo);
            detail.setOwnerCode(ownerCode);
            detail.setWarehouseCode(warehouseCode);
            detail.setTargetLocation(loc.getLocationCode());
            detail.setTargetArea(loc.getAreaCode());
            detail.setQuantity(quantity);
            detail.setOriginalQty(
                    loc.getCurrentQty() != null ? loc.getCurrentQty() : BigDecimal.ZERO);
            detail.setCurrentQty(quantity);
            // 计算容量利用率
            if (loc.getCapacity() != null
                    && loc.getUsedCapacity() != null
                    && loc.getCapacity().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal utilization =
                        loc.getUsedCapacity()
                                .multiply(new BigDecimal("100"))
                                .divide(loc.getCapacity(), 2, java.math.RoundingMode.HALF_UP);
                detail.setCapacityUtilization(utilization.intValue());
            } else {
                detail.setCapacityUtilization(0);
            }
            detail.setIsRecommend(i == 0 ? "Y" : "N");
            detail.setSortOrder(i + 1);
            detail.setStatus("PENDING");
            detail.setRemark(
                    String.format(
                            "策略:%s, 距离:%d, 层:%s",
                            strategy,
                            locationQueryService.calculateDistanceToReceiving(
                                    loc, locationQueryService.getReceivingPoint(warehouseCode)),
                            loc.getLevelNo()));
            detail.setCreatedTime(LocalDateTime.now());
            details.add(detail);
        }

        return details;
    }

    /** 获取默认规则 */
    private List<PutawayRule> getDefaultRules(String warehouseCode) {
        LambdaQueryWrapper<PutawayRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PutawayRule::getRuleType, "GLOBAL");
        wrapper.eq(PutawayRule::getStatus, "ACTIVE");
        if (warehouseCode != null) {
            wrapper.and(
                    w ->
                            w.eq(PutawayRule::getWarehouseCode, warehouseCode)
                                    .or()
                                    .isNull(PutawayRule::getWarehouseCode));
        }
        wrapper.orderByAsc(PutawayRule::getPriority);
        List<PutawayRule> rules = ruleMapper.selectList(wrapper);
        if (rules.isEmpty()) {
            // 创建默认规则
            PutawayRule defaultRule = new PutawayRule();
            defaultRule.setRuleCode("DEFAULT_PUTAWAY");
            defaultRule.setRuleName("默认上架规则");
            defaultRule.setRuleType("GLOBAL");
            defaultRule.setStrategy("NEAREST");
            defaultRule.setAllowMix("Y");
            defaultRule.setAllowBatchMix("N");
            defaultRule.setPriority(999);
            defaultRule.setStatus("ACTIVE");
            defaultRule.setCreatedTime(LocalDateTime.now());
            ruleMapper.insert(defaultRule);
            rules.add(defaultRule);
        }
        return rules;
    }

    // ============================================================

    // 4. 执行上架
    // ============================================================

    /** 执行上架 */
    @Transactional(rollbackFor = Exception.class)
    public PutawayLog executePutaway(
            String inboundNo,
            String asnNo,
            String skuCode,
            String batchNo,
            String ownerCode,
            String warehouseCode,
            String sourceLocation,
            String targetLocation,
            BigDecimal quantity,
            String operator) {
        long startTime = System.currentTimeMillis();
        log.info(
                "执行上架: inbound={}, sku={}, from={}, to={}, qty={}",
                inboundNo,
                skuCode,
                sourceLocation,
                targetLocation,
                quantity);

        // 1. 匹配规则
        List<PutawayRule> rules =
                ruleMapper.selectMatchedRules(skuCode, null, ownerCode, warehouseCode);
        PutawayRule rule = rules.isEmpty() ? getDefaultRules(warehouseCode).get(0) : rules.get(0);

        // 2. 创建日志
        PutawayLog putawayLog = new PutawayLog();
        putawayLog.setLogNo(generateLogNo());
        putawayLog.setInboundNo(inboundNo);
        putawayLog.setAsnNo(asnNo);
        putawayLog.setSkuCode(skuCode);
        putawayLog.setBatchNo(batchNo);
        putawayLog.setOwnerCode(ownerCode);
        putawayLog.setWarehouseCode(warehouseCode);
        putawayLog.setQuantity(quantity);
        putawayLog.setSourceLocation(sourceLocation);
        putawayLog.setTargetLocation(targetLocation);
        putawayLog.setRuleCode(rule.getRuleCode());
        putawayLog.setStrategyCode(rule.getStrategy());
        putawayLog.setStatus("SUCCESS");
        putawayLog.setTryCount(1);
        putawayLog.setOperator(operator);
        putawayLog.setOperateTime(LocalDateTime.now());
        putawayLog.setCreatedTime(LocalDateTime.now());

        // 3. 创建详情
        PutawayDetail detail = new PutawayDetail();
        detail.setDetailNo(generateDetailNo());
        detail.setLogNo(putawayLog.getLogNo());
        detail.setInboundNo(inboundNo);
        detail.setSkuCode(skuCode);
        detail.setBatchNo(batchNo);
        detail.setOwnerCode(ownerCode);
        detail.setWarehouseCode(warehouseCode);
        detail.setSourceLocation(sourceLocation);
        detail.setTargetLocation(targetLocation);
        detail.setQuantity(quantity);
        detail.setStatus("COMPLETED");
        detail.setOperator(operator);
        detail.setOperateTime(LocalDateTime.now());
        detail.setCreatedTime(LocalDateTime.now());

        // 4. 保存
        logMapper.insert(putawayLog);
        detailMapper.insert(detail);

        long duration = System.currentTimeMillis() - startTime;
        putawayLog.setDurationMs(duration);
        logMapper.updateById(putawayLog);

        log.info("上架完成: logNo={}, 耗时{}ms", putawayLog.getLogNo(), duration);
        return putawayLog;
    }

    // ============================================================

    // 5. 查询日志和详情
    // ============================================================

    public PutawayLog getLog(Long id) {
        return logMapper.selectById(id);
    }

    public PutawayLog getLogByNo(String logNo) {
        return logMapper.selectByLogNo(logNo);
    }

    public List<PutawayLog> getLogsByInboundNo(String inboundNo) {
        return logMapper.selectByInboundNo(inboundNo);
    }

    public Page<PutawayLog> pageLogs(
            Page<PutawayLog> page, String inboundNo, String skuCode, String status) {
        LambdaQueryWrapper<PutawayLog> wrapper = new LambdaQueryWrapper<>();
        if (inboundNo != null) wrapper.eq(PutawayLog::getInboundNo, inboundNo);
        if (skuCode != null) wrapper.eq(PutawayLog::getSkuCode, skuCode);
        if (status != null) wrapper.eq(PutawayLog::getStatus, status);
        wrapper.orderByDesc(PutawayLog::getOperateTime);
        return logMapper.selectPage(page, wrapper);
    }

    public List<PutawayDetail> getDetailsByLogNo(String logNo) {
        return detailMapper.selectByLogNo(logNo);
    }

    public List<PutawayDetail> getDetailsByInboundNo(String inboundNo) {
        return detailMapper.selectByInboundNo(inboundNo);
    }

    // ============================================================

    // 6. 编号生成
    // ============================================================

    private String generateRuleCode() {
        return "PAR"
                + NO_FMT.format(LocalDateTime.now())
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateStrategyCode() {
        return "PAS"
                + NO_FMT.format(LocalDateTime.now())
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateLogNo() {
        return "PAL"
                + NO_FMT.format(LocalDateTime.now())
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateDetailNo() {
        return "PAD"
                + NO_FMT.format(LocalDateTime.now())
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    // ============================================================

    // 7. 规则行管理（规则链）
    // ============================================================

    /** 获取规则的所有规则行（按行号排序） */
    public List<PutawayRuleLine> getRuleLines(Long ruleId) {
        return ruleLineMapper.selectByRuleId(ruleId);
    }

    /** 获取规则详情（含规则行） */
    public PutawayRule getRuleWithLines(Long ruleId) {
        PutawayRule rule = ruleMapper.selectById(ruleId);
        if (rule != null) {
            rule.setRuleLines(ruleLineMapper.selectByRuleId(ruleId));
        }
        return rule;
    }

    /** 创建规则行 */
    @Transactional(rollbackFor = Exception.class)
    public PutawayRuleLine createRuleLine(Long ruleId, PutawayRuleLine line) {
        PutawayRule rule = ruleMapper.selectById(ruleId);
        if (rule == null) {
            throw new RuntimeException("上架规则不存在: " + ruleId);
        }
        // 校验行号唯一性
        List<PutawayRuleLine> existing = ruleLineMapper.selectByRuleId(ruleId);
        boolean lineNoExists =
                existing.stream().anyMatch(l -> l.getLineNo().equals(line.getLineNo()));
        if (lineNoExists) {
            throw new RuntimeException("行号已存在: " + line.getLineNo());
        }
        line.setRuleId(ruleId);
        line.setCreatedTime(LocalDateTime.now());
        ruleLineMapper.insert(line);
        // 规则变更后状态变为草稿
        rule.setStatus("DRAFT");
        rule.setUpdatedTime(LocalDateTime.now());
        ruleMapper.updateById(rule);
        log.info("创建规则行: ruleId={}, lineNo={}", ruleId, line.getLineNo());
        return line;
    }

    /** 更新规则行 */
    @Transactional(rollbackFor = Exception.class)
    public PutawayRuleLine updateRuleLine(Long ruleId, PutawayRuleLine line) {
        line.setRuleId(ruleId);
        line.setUpdatedTime(LocalDateTime.now());
        ruleLineMapper.updateById(line);
        // 规则变更后状态变为草稿
        PutawayRule rule = new PutawayRule();
        rule.setId(ruleId);
        rule.setStatus("DRAFT");
        rule.setUpdatedTime(LocalDateTime.now());
        ruleMapper.updateById(rule);
        return ruleLineMapper.selectById(line.getId());
    }

    /** 删除规则行 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRuleLine(Long ruleId, Long lineId) {
        ruleLineMapper.deleteById(lineId);
        // 规则变更后状态变为草稿
        PutawayRule rule = new PutawayRule();
        rule.setId(ruleId);
        rule.setStatus("DRAFT");
        rule.setUpdatedTime(LocalDateTime.now());
        ruleMapper.updateById(rule);
        log.info("删除规则行: ruleId={}, lineId={}", ruleId, lineId);
    }

    /** 批量保存规则行（全量替换） */
    @Transactional(rollbackFor = Exception.class)
    public List<PutawayRuleLine> batchSaveRuleLines(Long ruleId, List<PutawayRuleLine> lines) {
        // 删除现有规则行
        ruleLineMapper.deleteByRuleId(ruleId);
        // 批量插入新规则行
        for (PutawayRuleLine line : lines) {
            line.setRuleId(ruleId);
            line.setId(null);
            line.setCreatedTime(LocalDateTime.now());
        }
        if (!lines.isEmpty()) {
            ruleLineMapper.batchInsert(lines);
        }
        // 规则变更后状态变为草稿
        PutawayRule rule = new PutawayRule();
        rule.setId(ruleId);
        rule.setStatus("DRAFT");
        rule.setUpdatedTime(LocalDateTime.now());
        ruleMapper.updateById(rule);
        log.info("批量保存规则行: ruleId={}, 行数={}", ruleId, lines.size());
        return ruleLineMapper.selectByRuleId(ruleId);
    }

    // ============================================================

    // 8. 规则复制/启用/停用
    // ============================================================

    /** 复制规则（含规则行） */
    @Transactional(rollbackFor = Exception.class)
    public PutawayRule copyRule(Long sourceRuleId, String newRuleName) {
        PutawayRule source = ruleMapper.selectById(sourceRuleId);
        if (source == null) {
            throw new RuntimeException("源规则不存在: " + sourceRuleId);
        }
        // 复制规则头
        PutawayRule copy = new PutawayRule();
        copy.setRuleCode(generateRuleCode());
        copy.setRuleName(newRuleName != null ? newRuleName : source.getRuleName() + "_副本");
        copy.setRuleType(source.getRuleType());
        copy.setSkuCode(source.getSkuCode());
        copy.setCategoryCode(source.getCategoryCode());
        copy.setOwnerCode(source.getOwnerCode());
        copy.setWarehouseCode(source.getWarehouseCode());
        copy.setWarehouseCodes(source.getWarehouseCodes());
        copy.setStrategy(source.getStrategy());
        copy.setTargetAreaCode(source.getTargetAreaCode());
        copy.setTargetLocationGroup(source.getTargetLocationGroup());
        copy.setPreferredArea(source.getPreferredArea());
        copy.setProductWeight(source.getProductWeight());
        copy.setAllowMix(source.getAllowMix());
        copy.setAllowBatchMix(source.getAllowBatchMix());
        copy.setMinCapacityUtilization(source.getMinCapacityUtilization());
        copy.setMaxCapacityUtilization(source.getMaxCapacityUtilization());
        copy.setPriority(source.getPriority());
        copy.setStatus("DRAFT");
        copy.setVersion(1);
        copy.setRemark(source.getRemark());
        copy.setCreatedTime(LocalDateTime.now());
        ruleMapper.insert(copy);

        // 复制规则行
        List<PutawayRuleLine> sourceLines = ruleLineMapper.selectByRuleId(sourceRuleId);
        for (PutawayRuleLine srcLine : sourceLines) {
            PutawayRuleLine newLine = new PutawayRuleLine();
            newLine.setRuleId(copy.getId());
            newLine.setLineNo(srcLine.getLineNo());
            newLine.setDescription(srcLine.getDescription());
            newLine.setConditionJson(srcLine.getConditionJson());
            newLine.setRuleCode(srcLine.getRuleCode());
            newLine.setTargetZone(srcLine.getTargetZone());
            newLine.setTargetLocation(srcLine.getTargetLocation());
            newLine.setLocationLimitsJson(srcLine.getLocationLimitsJson());
            newLine.setSpaceLimitsJson(srcLine.getSpaceLimitsJson());
            newLine.setExtendedConstraintsJson(srcLine.getExtendedConstraintsJson());
            newLine.setSuccessJumpLine(srcLine.getSuccessJumpLine());
            newLine.setFailJumpLine(srcLine.getFailJumpLine());
            newLine.setCreatedTime(LocalDateTime.now());
            ruleLineMapper.insert(newLine);
        }

        log.info(
                "复制规则: sourceId={}, newId={}, 规则行数={}",
                sourceRuleId,
                copy.getId(),
                sourceLines.size());
        return getRuleWithLines(copy.getId());
    }

    /** 启用规则 */
    @Transactional(rollbackFor = Exception.class)
    public PutawayRule enableRule(Long ruleId) {
        PutawayRule rule = ruleMapper.selectById(ruleId);
        if (rule == null) {
            throw new RuntimeException("规则不存在: " + ruleId);
        }
        // 校验至少有一行规则
        List<PutawayRuleLine> lines = ruleLineMapper.selectByRuleId(ruleId);
        if (lines.isEmpty()) {
            throw new RuntimeException("请至少配置一行规则");
        }
        rule.setStatus("ACTIVE");
        rule.setUpdatedTime(LocalDateTime.now());
        ruleMapper.updateById(rule);
        log.info("启用规则: ruleId={}", ruleId);
        return rule;
    }

    /** 停用规则 */
    @Transactional(rollbackFor = Exception.class)
    public PutawayRule disableRule(Long ruleId) {
        PutawayRule rule = ruleMapper.selectById(ruleId);
        if (rule == null) {
            throw new RuntimeException("规则不存在: " + ruleId);
        }
        rule.setStatus("DISABLED");
        rule.setUpdatedTime(LocalDateTime.now());
        ruleMapper.updateById(rule);
        log.info("停用规则: ruleId={}", ruleId);
        return rule;
    }

    /** 删除规则（含规则行） */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long ruleId) {
        PutawayRule rule = ruleMapper.selectById(ruleId);
        if (rule == null) {
            throw new RuntimeException("规则不存在: " + ruleId);
        }
        if ("ACTIVE".equals(rule.getStatus())) {
            throw new RuntimeException("已启用规则不可删除，请先停用");
        }
        // 删除规则行
        ruleLineMapper.deleteByRuleId(ruleId);
        // 删除规则
        ruleMapper.deleteById(ruleId);
        log.info("删除规则: ruleId={}", ruleId);
    }
}
