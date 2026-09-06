package com.xwms.core.query.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.query.entity.*;
import com.xwms.core.query.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 搴撳瓨鏌ヨ/鎶ヨ〃绠＄悊鏍稿績鏈嶅姟 鏍稿績鑳藉姏: 瀹炴椂搴撳瓨鏌ヨ/搴撳瓨蹇収/搴撳瓨鏃ユ姤/搴撳瓨鏈堟姤/搴撳瓨鍒嗘瀽鎸囨爣 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryQueryService {

    private final InventoryDailyMapper dailyMapper;
    private final InventoryMonthlyMapper monthlyMapper;
    private final InventoryMetricMapper metricMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    // ============================================================

    // 1. 搴撳瓨蹇収绠＄悊
    // ============================================================

    // ============================================================

    // 2. 搴撳瓨鏃ユ姤绠＄悊
    // ============================================================

    /** 鐢熸垚搴撳瓨鏃ユ姤 */
    @Transactional(rollbackFor = Exception.class)
    public int generateDailyReport(
            LocalDate reportDate, String warehouseCode, List<Map<String, Object>> dailyData) {
        int count = 0;
        for (Map<String, Object> data : dailyData) {
            InventoryDaily daily = new InventoryDaily();
            daily.setReportDate(reportDate);
            daily.setWarehouseCode(warehouseCode);
            daily.setOwnerCode((String) data.get("ownerCode"));
            daily.setSkuCode((String) data.get("skuCode"));
            daily.setSkuName((String) data.get("skuName"));
            daily.setBeginQty(toBigDecimal(data.get("beginQty")));
            daily.setInboundQty(toBigDecimal(data.get("inboundQty")));
            daily.setOutboundQty(toBigDecimal(data.get("outboundQty")));
            daily.setAdjustInQty(toBigDecimal(data.get("adjustInQty")));
            daily.setAdjustOutQty(toBigDecimal(data.get("adjustOutQty")));
            daily.setEndQty(toBigDecimal(data.get("endQty")));
            daily.setBeginCost(toBigDecimal(data.get("beginCost")));
            daily.setInboundCost(toBigDecimal(data.get("inboundCost")));
            daily.setOutboundCost(toBigDecimal(data.get("outboundCost")));
            daily.setEndCost(toBigDecimal(data.get("endCost")));
            daily.setAvgCost(toBigDecimal(data.get("avgCost")));
            daily.setTurnoverDays(toBigDecimal(data.get("turnoverDays")));
            daily.setStatus("ACTIVE");
            dailyMapper.insert(daily);
            count++;
        }
        log.info("鐢熸垚搴撳瓨鏃ユ姤: date={}, warehouse={}, count={}", reportDate, warehouseCode, count);
        return count;
    }

    public List<InventoryDaily> getDailyByDateAndWarehouse(
            LocalDate reportDate, String warehouseCode) {
        return dailyMapper.selectByDateAndWarehouse(reportDate, warehouseCode);
    }

    public List<InventoryDaily> getDailyBySkuAndDateRange(
            String skuCode, LocalDate startDate, LocalDate endDate) {
        return dailyMapper.selectBySkuAndDateRange(skuCode, startDate, endDate);
    }

    public InventoryDaily getDailyByDateAndSku(
            LocalDate reportDate, String skuCode, String warehouseCode) {
        return dailyMapper.selectByDateAndSku(reportDate, skuCode, warehouseCode);
    }

    public Page<InventoryDaily> pageDaily(
            Page<InventoryDaily> page, LocalDate reportDate, String warehouseCode, String skuCode) {
        LambdaQueryWrapper<InventoryDaily> wrapper = new LambdaQueryWrapper<>();
        if (reportDate != null) wrapper.eq(InventoryDaily::getReportDate, reportDate);
        if (warehouseCode != null) wrapper.eq(InventoryDaily::getWarehouseCode, warehouseCode);
        if (skuCode != null) wrapper.eq(InventoryDaily::getSkuCode, skuCode);
        wrapper.orderByDesc(InventoryDaily::getReportDate);
        return dailyMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 3. 搴撳瓨鏈堟姤绠＄悊
    // ============================================================

    /** 鐢熸垚搴撳瓨鏈堟姤 */
    @Transactional(rollbackFor = Exception.class)
    public int generateMonthlyReport(
            String reportMonth, String warehouseCode, List<Map<String, Object>> monthlyData) {
        int count = 0;
        for (Map<String, Object> data : monthlyData) {
            InventoryMonthly monthly = new InventoryMonthly();
            monthly.setReportMonth(reportMonth);
            monthly.setWarehouseCode(warehouseCode);
            monthly.setOwnerCode((String) data.get("ownerCode"));
            monthly.setSkuCode((String) data.get("skuCode"));
            monthly.setSkuName((String) data.get("skuName"));
            monthly.setBeginQty(toBigDecimal(data.get("beginQty")));
            monthly.setInboundQty(toBigDecimal(data.get("inboundQty")));
            monthly.setOutboundQty(toBigDecimal(data.get("outboundQty")));
            monthly.setEndQty(toBigDecimal(data.get("endQty")));
            monthly.setBeginCost(toBigDecimal(data.get("beginCost")));
            monthly.setInboundCost(toBigDecimal(data.get("inboundCost")));
            monthly.setOutboundCost(toBigDecimal(data.get("outboundCost")));
            monthly.setEndCost(toBigDecimal(data.get("endCost")));
            monthly.setAvgCost(toBigDecimal(data.get("avgCost")));
            monthly.setTurnoverRate(toBigDecimal(data.get("turnoverRate")));
            monthly.setTurnoverDays(toBigDecimal(data.get("turnoverDays")));
            if (data.get("stockoutDays") != null) {
                monthly.setStockoutDays(Integer.parseInt(data.get("stockoutDays").toString()));
            }
            monthly.setStatus("ACTIVE");
            monthlyMapper.insert(monthly);
            count++;
        }
        log.info("鐢熸垚搴撳瓨鏈堟姤: month={}, warehouse={}, count={}", reportMonth, warehouseCode, count);
        return count;
    }

    public List<InventoryMonthly> getMonthlyByMonthAndWarehouse(
            String reportMonth, String warehouseCode) {
        return monthlyMapper.selectByMonthAndWarehouse(reportMonth, warehouseCode);
    }

    public List<InventoryMonthly> getMonthlyBySkuAndMonthRange(
            String skuCode, String startMonth, String endMonth) {
        return monthlyMapper.selectBySkuAndMonthRange(skuCode, startMonth, endMonth);
    }

    public InventoryMonthly getMonthlyByMonthAndSku(
            String reportMonth, String skuCode, String warehouseCode) {
        return monthlyMapper.selectByMonthAndSku(reportMonth, skuCode, warehouseCode);
    }

    public Page<InventoryMonthly> pageMonthly(
            Page<InventoryMonthly> page, String reportMonth, String warehouseCode, String skuCode) {
        LambdaQueryWrapper<InventoryMonthly> wrapper = new LambdaQueryWrapper<>();
        if (reportMonth != null) wrapper.eq(InventoryMonthly::getReportMonth, reportMonth);
        if (warehouseCode != null) wrapper.eq(InventoryMonthly::getWarehouseCode, warehouseCode);
        if (skuCode != null) wrapper.eq(InventoryMonthly::getSkuCode, skuCode);
        wrapper.orderByDesc(InventoryMonthly::getReportMonth);
        return monthlyMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 4. 搴撳瓨鍒嗘瀽鎸囨爣绠＄悊
    // ============================================================

    /** 璁＄畻搴撳瓨鍛ㄨ浆鐜? 鍏紡: 鍛ㄨ浆鐜?= 鍑哄簱鎴愭湰 / 骞冲潎搴撳瓨鎴愭湰 */
    public BigDecimal calculateTurnoverRate(BigDecimal outboundCost, BigDecimal avgInventoryCost) {
        if (avgInventoryCost == null || avgInventoryCost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return outboundCost.divide(avgInventoryCost, 4, RoundingMode.HALF_UP);
    }

    /** 璁＄畻搴撳瓨鍛ㄨ浆澶╂暟 鍏紡: 鍛ㄨ浆澶╂暟 = 365 / 鍛ㄨ浆鐜? */
    public BigDecimal calculateTurnoverDays(BigDecimal turnoverRate) {
        if (turnoverRate == null || turnoverRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal("365").divide(turnoverRate, 2, RoundingMode.HALF_UP);
    }

    /** 璁＄畻搴撳瓨鍑嗙‘鐜? 鍏紡: 鍑嗙‘鐜?= (鎬籗KU鏁?- 宸紓SKU鏁? / 鎬籗KU鏁?* 100% */
    public BigDecimal calculateAccuracyRate(int totalSku, int diffSku) {
        if (totalSku == 0) return BigDecimal.ZERO;
        return new BigDecimal(totalSku - diffSku)
                .divide(new BigDecimal(totalSku), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /** 淇濆瓨搴撳瓨鍒嗘瀽鎸囨爣 */
    @Transactional(rollbackFor = Exception.class)
    public int saveMetrics(
            LocalDate metricDate,
            String metricType,
            String warehouseCode,
            List<Map<String, Object>> metricsData) {
        int count = 0;
        for (Map<String, Object> data : metricsData) {
            InventoryMetric metric = new InventoryMetric();
            metric.setMetricId(generateMetricId());
            metric.setMetricDate(metricDate);
            metric.setMetricType(metricType);
            metric.setWarehouseCode(warehouseCode);
            metric.setOwnerCode((String) data.get("ownerCode"));
            metric.setSkuCode((String) data.get("skuCode"));
            metric.setCategoryCode((String) data.get("categoryCode"));
            metric.setMetricValue(toBigDecimal(data.get("metricValue")));
            metric.setMetricValue2(toBigDecimal(data.get("metricValue2")));
            metric.setMetricValue3(toBigDecimal(data.get("metricValue3")));
            metric.setMetricText((String) data.get("metricText"));
            if (data.get("rankNo") != null) {
                metric.setRankNo(Integer.parseInt(data.get("rankNo").toString()));
            }
            metric.setLevelCode((String) data.get("levelCode"));
            metric.setStatus("ACTIVE");
            metricMapper.insert(metric);
            count++;
        }
        log.info(
                "淇濆瓨搴撳瓨鍒嗘瀽鎸囨爣: date={}, type={}, warehouse={}, count={}",
                metricDate,
                metricType,
                warehouseCode,
                count);
        return count;
    }

    public List<InventoryMetric> getMetricsByDateAndType(
            LocalDate metricDate, String metricType, String warehouseCode) {
        return metricMapper.selectByDateAndType(metricDate, metricType, warehouseCode);
    }

    public List<InventoryMetric> getMetricsBySkuAndDateRange(
            String skuCode, String metricType, LocalDate startDate, LocalDate endDate) {
        return metricMapper.selectBySkuAndDateRange(skuCode, metricType, startDate, endDate);
    }

    public List<InventoryMetric> getMetricsByLevel(
            LocalDate metricDate, String metricType, String levelCode) {
        return metricMapper.selectByLevel(metricDate, metricType, levelCode);
    }

    public Page<InventoryMetric> pageMetrics(
            Page<InventoryMetric> page,
            LocalDate metricDate,
            String metricType,
            String warehouseCode,
            String skuCode) {
        LambdaQueryWrapper<InventoryMetric> wrapper = new LambdaQueryWrapper<>();
        if (metricDate != null) wrapper.eq(InventoryMetric::getMetricDate, metricDate);
        if (metricType != null) wrapper.eq(InventoryMetric::getMetricType, metricType);
        if (warehouseCode != null) wrapper.eq(InventoryMetric::getWarehouseCode, warehouseCode);
        if (skuCode != null) wrapper.eq(InventoryMetric::getSkuCode, skuCode);
        wrapper.orderByDesc(InventoryMetric::getMetricDate);
        return metricMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 5. 搴撳瓨姹囨€荤粺璁?
    // ============================================================

    /** 鎸夌淮搴︽眹鎬诲簱瀛? */
    public Map<String, Object> summarizeInventory(
            List<Map<String, Object>> inventoryList, String dimension) {
        Map<String, Object> result = new HashMap<>();
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAvailable = BigDecimal.ZERO;
        BigDecimal totalAllocated = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        int skuCount = 0;

        for (Map<String, Object> inv : inventoryList) {
            totalQty = totalQty.add(toBigDecimal(inv.get("totalQty")));
            totalAvailable = totalAvailable.add(toBigDecimal(inv.get("availableQty")));
            totalAllocated = totalAllocated.add(toBigDecimal(inv.get("allocatedQty")));
            totalCost = totalCost.add(toBigDecimal(inv.get("totalCost")));
            skuCount++;
        }

        result.put("dimension", dimension);
        result.put("skuCount", skuCount);
        result.put("totalQty", totalQty);
        result.put("totalAvailable", totalAvailable);
        result.put("totalAllocated", totalAllocated);
        result.put("totalCost", totalCost);
        result.put(
                "availableRate",
                totalQty.compareTo(BigDecimal.ZERO) > 0
                        ? totalAvailable
                                .divide(totalQty, 4, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"))
                        : BigDecimal.ZERO);
        return result;
    }

    /** 搴撳瓨ABC鍒嗙被 */
    public List<Map<String, Object>> classifyABC(
            List<Map<String, Object>> skuList, BigDecimal thresholdA, BigDecimal thresholdB) {
        // 鎸夊嚭搴撻噾棰濋檷搴忔帓搴?
        List<Map<String, Object>> sorted =
                skuList.stream()
                        .sorted(
                                Comparator.comparing(
                                        m ->
                                                toBigDecimal(
                                                                ((Map<String, Object>) m)
                                                                        .get("outboundAmount"))
                                                        .negate()))
                        .collect(Collectors.toList());

        BigDecimal totalAmount =
                sorted.stream()
                        .map(m -> toBigDecimal(m.get("outboundAmount")))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cumulative = BigDecimal.ZERO;
        for (Map<String, Object> sku : sorted) {
            BigDecimal amount = toBigDecimal(sku.get("outboundAmount"));
            cumulative = cumulative.add(amount);
            BigDecimal ratio =
                    totalAmount.compareTo(BigDecimal.ZERO) > 0
                            ? cumulative
                                    .divide(totalAmount, 4, RoundingMode.HALF_UP)
                                    .multiply(new BigDecimal("100"))
                            : BigDecimal.ZERO;

            String level;
            if (ratio.compareTo(thresholdA) <= 0) {
                level = "A";
            } else if (ratio.compareTo(thresholdB) <= 0) {
                level = "B";
            } else {
                level = "C";
            }
            sku.put("abcLevel", level);
            sku.put("cumulativeRatio", ratio);
        }
        return sorted;
    }

    // ============================================================

    // 宸ュ叿鏂规硶
    // ============================================================

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        return new BigDecimal(obj.toString());
    }

    private String generateSnapshotId() {
        return "SNP"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", SEQ.incrementAndGet() % 10000);
    }

    private String generateMetricId() {
        return "MET"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", SEQ.incrementAndGet() % 10000);
    }
}
