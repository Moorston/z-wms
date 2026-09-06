package com.xwms.core.recommend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.recommend.entity.*;
import com.xwms.core.recommend.enums.RecommendPriority;
import com.xwms.core.recommend.enums.RecommendStatus;
import com.xwms.core.recommend.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库存智能推荐管理核心服务 核心能力: 补货推荐/库位推荐/波次推荐/路径推荐 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryRecommendService {

    private final ReplenishRecommendMapper replenishRecommendMapper;
    private final LocationRecommendMapper locationRecommendMapper;
    private final WaveRecommendMapper waveRecommendMapper;
    private final PathRecommendMapper pathRecommendMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 补货推荐
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ReplenishRecommend createReplenishRecommend(
            String warehouseCode,
            String ownerCode,
            String skuCode,
            String skuName,
            String categoryCode,
            String fromLocation,
            String toLocation,
            BigDecimal currentQuantity,
            BigDecimal safetyStock,
            BigDecimal reorderPoint,
            BigDecimal maxStock,
            BigDecimal forecastDemand,
            String forecastPeriod,
            Integer leadTime,
            String priority,
            BigDecimal confidence,
            String reason,
            String operator) {
        ReplenishRecommend recommend = new ReplenishRecommend();
        recommend.setRecommendId(generateRecommendId("RR"));
        recommend.setWarehouseCode(warehouseCode);
        recommend.setOwnerCode(ownerCode);
        recommend.setSkuCode(skuCode);
        recommend.setSkuName(skuName);
        recommend.setCategoryCode(categoryCode);
        recommend.setFromLocation(fromLocation);
        recommend.setToLocation(toLocation);
        recommend.setCurrentQuantity(currentQuantity);
        recommend.setSafetyStock(safetyStock);
        recommend.setReorderPoint(reorderPoint);
        recommend.setMaxStock(maxStock);
        recommend.setForecastDemand(forecastDemand);
        recommend.setForecastPeriod(forecastPeriod);
        recommend.setLeadTime(leadTime);
        recommend.setPriority(priority != null ? priority : RecommendPriority.NORMAL.getCode());
        recommend.setConfidence(confidence);
        recommend.setReason(reason);
        recommend.setStatus(RecommendStatus.PENDING.getCode());
        recommend.setOperator(operator);
        recommend.setRecommendTime(LocalDateTime.now());

        // 计算推荐补货量
        BigDecimal recommendQuantity =
                calculateReplenishQuantity(
                        currentQuantity,
                        safetyStock,
                        reorderPoint,
                        maxStock,
                        forecastDemand,
                        leadTime);
        recommend.setRecommendQuantity(recommendQuantity);

        replenishRecommendMapper.insert(recommend);
        log.info(
                "创建补货推荐: id={}, sku={}, current={}, recommend={}, priority={}",
                recommend.getRecommendId(),
                skuCode,
                currentQuantity,
                recommendQuantity,
                priority);
        return recommend;
    }

    /**
     * 计算推荐补货量（核心逻辑） 公式: 推荐补货量 = max(0, maxStock - currentQuantity + forecastDemand * leadTime)
     * 若当前库存 <= 再订货点，则触发补货
     */
    private BigDecimal calculateReplenishQuantity(
            BigDecimal currentQuantity,
            BigDecimal safetyStock,
            BigDecimal reorderPoint,
            BigDecimal maxStock,
            BigDecimal forecastDemand,
            Integer leadTime) {
        if (currentQuantity == null) return BigDecimal.ZERO;
        if (reorderPoint != null && currentQuantity.compareTo(reorderPoint) > 0) {
            return BigDecimal.ZERO; // 未到再订货点，不补货
        }
        BigDecimal target = maxStock != null ? maxStock : safetyStock;
        BigDecimal demand =
                forecastDemand != null && leadTime != null
                        ? forecastDemand.multiply(BigDecimal.valueOf(leadTime))
                        : BigDecimal.ZERO;
        BigDecimal quantity = target.subtract(currentQuantity).add(demand);
        return quantity.max(BigDecimal.ZERO);
    }

    /** 接受补货推荐 */
    @Transactional(rollbackFor = Exception.class)
    public ReplenishRecommend acceptReplenishRecommend(String recommendId, String operator) {
        ReplenishRecommend recommend = replenishRecommendMapper.selectByRecommendId(recommendId);
        if (recommend == null) throw new RuntimeException("补货推荐不存在: " + recommendId);
        if (!RecommendStatus.PENDING.getCode().equals(recommend.getStatus())) {
            throw new RuntimeException("补货推荐状态不是待处理: " + recommend.getStatus());
        }
        recommend.setStatus(RecommendStatus.ACCEPTED.getCode());
        recommend.setOperator(operator);
        recommend.setOperateTime(LocalDateTime.now());
        replenishRecommendMapper.updateById(recommend);
        log.info(
                "接受补货推荐: id={}, sku={}, quantity={}",
                recommendId,
                recommend.getSkuCode(),
                recommend.getRecommendQuantity());
        return replenishRecommendMapper.selectByRecommendId(recommendId);
    }

    /** 拒绝补货推荐 */
    @Transactional(rollbackFor = Exception.class)
    public ReplenishRecommend rejectReplenishRecommend(
            String recommendId, String reason, String operator) {
        ReplenishRecommend recommend = replenishRecommendMapper.selectByRecommendId(recommendId);
        if (recommend == null) throw new RuntimeException("补货推荐不存在: " + recommendId);
        recommend.setStatus(RecommendStatus.REJECTED.getCode());
        recommend.setReason(reason != null ? reason : recommend.getReason());
        recommend.setOperator(operator);
        recommend.setOperateTime(LocalDateTime.now());
        replenishRecommendMapper.updateById(recommend);
        log.info("拒绝补货推荐: id={}, sku={}, reason={}", recommendId, recommend.getSkuCode(), reason);
        return replenishRecommendMapper.selectByRecommendId(recommendId);
    }

    public ReplenishRecommend getReplenishRecommendById(String recommendId) {
        return replenishRecommendMapper.selectByRecommendId(recommendId);
    }

    public List<ReplenishRecommend> getPendingReplenishByWarehouseAndSku(
            String warehouseCode, String skuCode) {
        return replenishRecommendMapper.selectPendingByWarehouseAndSku(warehouseCode, skuCode);
    }

    public List<ReplenishRecommend> getReplenishByWarehouseAndStatus(
            String warehouseCode, String status) {
        return replenishRecommendMapper.selectByWarehouseAndStatus(warehouseCode, status);
    }

    public List<ReplenishRecommend> getReplenishByWarehouseAndPriority(
            String warehouseCode, String priority) {
        return replenishRecommendMapper.selectByWarehouseAndPriority(warehouseCode, priority);
    }

    public Page<ReplenishRecommend> pageReplenish(
            Page<ReplenishRecommend> page,
            String warehouseCode,
            String skuCode,
            String status,
            String priority) {
        LambdaQueryWrapper<ReplenishRecommend> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(ReplenishRecommend::getWarehouseCode, warehouseCode);
        if (skuCode != null) wrapper.eq(ReplenishRecommend::getSkuCode, skuCode);
        if (status != null) wrapper.eq(ReplenishRecommend::getStatus, status);
        if (priority != null) wrapper.eq(ReplenishRecommend::getPriority, priority);
        wrapper.orderByDesc(ReplenishRecommend::getRecommendTime);
        return replenishRecommendMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. 库位推荐
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public LocationRecommend createLocationRecommend(
            String warehouseCode,
            String ownerCode,
            String skuCode,
            String skuName,
            String batchNo,
            BigDecimal quantity,
            String bizType,
            String bizNo,
            String recommendLocations,
            String bestLocation,
            BigDecimal bestScore,
            String recommendReason,
            String factors,
            String operator) {
        LocationRecommend recommend = new LocationRecommend();
        recommend.setRecommendId(generateRecommendId("LR"));
        recommend.setWarehouseCode(warehouseCode);
        recommend.setOwnerCode(ownerCode);
        recommend.setSkuCode(skuCode);
        recommend.setSkuName(skuName);
        recommend.setBatchNo(batchNo);
        recommend.setQuantity(quantity);
        recommend.setBizType(bizType);
        recommend.setBizNo(bizNo);
        recommend.setRecommendLocations(recommendLocations);
        recommend.setBestLocation(bestLocation);
        recommend.setBestScore(bestScore);
        recommend.setRecommendReason(recommendReason);
        recommend.setFactors(factors);
        recommend.setStatus(RecommendStatus.PENDING.getCode());
        recommend.setOperator(operator);
        recommend.setRecommendTime(LocalDateTime.now());
        locationRecommendMapper.insert(recommend);
        log.info(
                "创建库位推荐: id={}, sku={}, biz={}, best={}, score={}",
                recommend.getRecommendId(),
                skuCode,
                bizType,
                bestLocation,
                bestScore);
        return recommend;
    }

    /** 接受库位推荐 */
    @Transactional(rollbackFor = Exception.class)
    public LocationRecommend acceptLocationRecommend(
            String recommendId, String selectedLocation, String operator) {
        LocationRecommend recommend = locationRecommendMapper.selectByRecommendId(recommendId);
        if (recommend == null) throw new RuntimeException("库位推荐不存在: " + recommendId);
        recommend.setStatus(RecommendStatus.ACCEPTED.getCode());
        recommend.setSelectedLocation(
                selectedLocation != null ? selectedLocation : recommend.getBestLocation());
        recommend.setOperator(operator);
        recommend.setOperateTime(LocalDateTime.now());
        locationRecommendMapper.updateById(recommend);
        log.info(
                "接受库位推荐: id={}, sku={}, selected={}",
                recommendId,
                recommend.getSkuCode(),
                selectedLocation);
        return locationRecommendMapper.selectByRecommendId(recommendId);
    }

    /** 拒绝库位推荐 */
    @Transactional(rollbackFor = Exception.class)
    public LocationRecommend rejectLocationRecommend(
            String recommendId, String reason, String operator) {
        LocationRecommend recommend = locationRecommendMapper.selectByRecommendId(recommendId);
        if (recommend == null) throw new RuntimeException("库位推荐不存在: " + recommendId);
        recommend.setStatus(RecommendStatus.REJECTED.getCode());
        recommend.setRecommendReason(reason != null ? reason : recommend.getRecommendReason());
        recommend.setOperator(operator);
        recommend.setOperateTime(LocalDateTime.now());
        locationRecommendMapper.updateById(recommend);
        log.info("拒绝库位推荐: id={}, sku={}, reason={}", recommendId, recommend.getSkuCode(), reason);
        return locationRecommendMapper.selectByRecommendId(recommendId);
    }

    public LocationRecommend getLocationRecommendById(String recommendId) {
        return locationRecommendMapper.selectByRecommendId(recommendId);
    }

    public List<LocationRecommend> getPendingLocationByWarehouseAndSku(
            String warehouseCode, String skuCode) {
        return locationRecommendMapper.selectPendingByWarehouseAndSku(warehouseCode, skuCode);
    }

    public List<LocationRecommend> getLocationByBiz(String bizType, String bizNo) {
        return locationRecommendMapper.selectByBiz(bizType, bizNo);
    }

    public List<LocationRecommend> getLocationByWarehouseAndStatus(
            String warehouseCode, String status) {
        return locationRecommendMapper.selectByWarehouseAndStatus(warehouseCode, status);
    }

    public Page<LocationRecommend> pageLocation(
            Page<LocationRecommend> page,
            String warehouseCode,
            String skuCode,
            String bizType,
            String status) {
        LambdaQueryWrapper<LocationRecommend> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(LocationRecommend::getWarehouseCode, warehouseCode);
        if (skuCode != null) wrapper.eq(LocationRecommend::getSkuCode, skuCode);
        if (bizType != null) wrapper.eq(LocationRecommend::getBizType, bizType);
        if (status != null) wrapper.eq(LocationRecommend::getStatus, status);
        wrapper.orderByDesc(LocationRecommend::getRecommendTime);
        return locationRecommendMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 3. 波次推荐
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public WaveRecommend createWaveRecommend(
            String warehouseCode,
            String ownerCode,
            String waveType,
            String recommendStrategy,
            Integer orderCount,
            Integer skuCount,
            BigDecimal totalQuantity,
            String recommendOrders,
            String recommendWaveName,
            Long estimatePickTime,
            Integer estimatePickers,
            String priority,
            BigDecimal confidence,
            String reason,
            String operator) {
        WaveRecommend recommend = new WaveRecommend();
        recommend.setRecommendId(generateRecommendId("WR"));
        recommend.setWarehouseCode(warehouseCode);
        recommend.setOwnerCode(ownerCode);
        recommend.setWaveType(waveType);
        recommend.setRecommendStrategy(recommendStrategy);
        recommend.setOrderCount(orderCount);
        recommend.setSkuCount(skuCount);
        recommend.setTotalQuantity(totalQuantity);
        recommend.setRecommendOrders(recommendOrders);
        recommend.setRecommendWaveName(recommendWaveName);
        recommend.setEstimatePickTime(estimatePickTime);
        recommend.setEstimatePickers(estimatePickers);
        recommend.setPriority(priority != null ? priority : RecommendPriority.NORMAL.getCode());
        recommend.setConfidence(confidence);
        recommend.setReason(reason);
        recommend.setStatus(RecommendStatus.PENDING.getCode());
        recommend.setOperator(operator);
        recommend.setRecommendTime(LocalDateTime.now());
        waveRecommendMapper.insert(recommend);
        log.info(
                "创建波次推荐: id={}, type={}, orders={}, strategy={}, priority={}",
                recommend.getRecommendId(),
                waveType,
                orderCount,
                recommendStrategy,
                priority);
        return recommend;
    }

    /** 接受波次推荐 */
    @Transactional(rollbackFor = Exception.class)
    public WaveRecommend acceptWaveRecommend(
            String recommendId, String relatedWaveId, String operator) {
        WaveRecommend recommend = waveRecommendMapper.selectByRecommendId(recommendId);
        if (recommend == null) throw new RuntimeException("波次推荐不存在: " + recommendId);
        recommend.setStatus(RecommendStatus.ACCEPTED.getCode());
        recommend.setRelatedWaveId(relatedWaveId);
        recommend.setOperator(operator);
        recommend.setOperateTime(LocalDateTime.now());
        waveRecommendMapper.updateById(recommend);
        log.info(
                "接受波次推荐: id={}, type={}, relatedWave={}",
                recommendId,
                recommend.getWaveType(),
                relatedWaveId);
        return waveRecommendMapper.selectByRecommendId(recommendId);
    }

    /** 拒绝波次推荐 */
    @Transactional(rollbackFor = Exception.class)
    public WaveRecommend rejectWaveRecommend(String recommendId, String reason, String operator) {
        WaveRecommend recommend = waveRecommendMapper.selectByRecommendId(recommendId);
        if (recommend == null) throw new RuntimeException("波次推荐不存在: " + recommendId);
        recommend.setStatus(RecommendStatus.REJECTED.getCode());
        recommend.setReason(reason != null ? reason : recommend.getReason());
        recommend.setOperator(operator);
        recommend.setOperateTime(LocalDateTime.now());
        waveRecommendMapper.updateById(recommend);
        log.info("拒绝波次推荐: id={}, type={}, reason={}", recommendId, recommend.getWaveType(), reason);
        return waveRecommendMapper.selectByRecommendId(recommendId);
    }

    public WaveRecommend getWaveRecommendById(String recommendId) {
        return waveRecommendMapper.selectByRecommendId(recommendId);
    }

    public List<WaveRecommend> getPendingWaveByWarehouseAndType(
            String warehouseCode, String waveType) {
        return waveRecommendMapper.selectPendingByWarehouseAndType(warehouseCode, waveType);
    }

    public List<WaveRecommend> getWaveByWarehouseAndStatus(String warehouseCode, String status) {
        return waveRecommendMapper.selectByWarehouseAndStatus(warehouseCode, status);
    }

    public List<WaveRecommend> getWaveByWarehouseAndPriority(
            String warehouseCode, String priority) {
        return waveRecommendMapper.selectByWarehouseAndPriority(warehouseCode, priority);
    }

    public Page<WaveRecommend> pageWave(
            Page<WaveRecommend> page,
            String warehouseCode,
            String waveType,
            String status,
            String priority) {
        LambdaQueryWrapper<WaveRecommend> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(WaveRecommend::getWarehouseCode, warehouseCode);
        if (waveType != null) wrapper.eq(WaveRecommend::getWaveType, waveType);
        if (status != null) wrapper.eq(WaveRecommend::getStatus, status);
        if (priority != null) wrapper.eq(WaveRecommend::getPriority, priority);
        wrapper.orderByDesc(WaveRecommend::getRecommendTime);
        return waveRecommendMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 4. 路径推荐
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public PathRecommend createPathRecommend(
            String warehouseCode,
            String ownerCode,
            String waveId,
            String pickerId,
            String pickerName,
            String pickMode,
            Integer locationCount,
            Integer skuCount,
            BigDecimal totalQuantity,
            String recommendPath,
            String startLocation,
            String endLocation,
            BigDecimal estimateDistance,
            Long estimateTime,
            String pathAlgorithm,
            String congestionAvoidance,
            String revisitAvoidance,
            String priority,
            BigDecimal confidence,
            String reason,
            String operator) {
        PathRecommend recommend = new PathRecommend();
        recommend.setRecommendId(generateRecommendId("PR"));
        recommend.setWarehouseCode(warehouseCode);
        recommend.setOwnerCode(ownerCode);
        recommend.setWaveId(waveId);
        recommend.setPickerId(pickerId);
        recommend.setPickerName(pickerName);
        recommend.setPickMode(pickMode);
        recommend.setLocationCount(locationCount);
        recommend.setSkuCount(skuCount);
        recommend.setTotalQuantity(totalQuantity);
        recommend.setRecommendPath(recommendPath);
        recommend.setStartLocation(startLocation);
        recommend.setEndLocation(endLocation);
        recommend.setEstimateDistance(estimateDistance);
        recommend.setEstimateTime(estimateTime);
        recommend.setPathAlgorithm(pathAlgorithm);
        recommend.setCongestionAvoidance(congestionAvoidance != null ? congestionAvoidance : "Y");
        recommend.setRevisitAvoidance(revisitAvoidance != null ? revisitAvoidance : "Y");
        recommend.setPriority(priority != null ? priority : RecommendPriority.NORMAL.getCode());
        recommend.setConfidence(confidence);
        recommend.setReason(reason);
        recommend.setStatus(RecommendStatus.PENDING.getCode());
        recommend.setOperator(operator);
        recommend.setRecommendTime(LocalDateTime.now());
        pathRecommendMapper.insert(recommend);
        log.info(
                "创建路径推荐: id={}, wave={}, picker={}, locations={}, distance={}, time={}",
                recommend.getRecommendId(),
                waveId,
                pickerId,
                locationCount,
                estimateDistance,
                estimateTime);
        return recommend;
    }

    /** 接受路径推荐 */
    @Transactional(rollbackFor = Exception.class)
    public PathRecommend acceptPathRecommend(String recommendId, String operator) {
        PathRecommend recommend = pathRecommendMapper.selectByRecommendId(recommendId);
        if (recommend == null) throw new RuntimeException("路径推荐不存在: " + recommendId);
        recommend.setStatus(RecommendStatus.ACCEPTED.getCode());
        recommend.setOperator(operator);
        recommend.setOperateTime(LocalDateTime.now());
        pathRecommendMapper.updateById(recommend);
        log.info(
                "接受路径推荐: id={}, wave={}, picker={}",
                recommendId,
                recommend.getWaveId(),
                recommend.getPickerId());
        return pathRecommendMapper.selectByRecommendId(recommendId);
    }

    /** 拒绝路径推荐 */
    @Transactional(rollbackFor = Exception.class)
    public PathRecommend rejectPathRecommend(String recommendId, String reason, String operator) {
        PathRecommend recommend = pathRecommendMapper.selectByRecommendId(recommendId);
        if (recommend == null) throw new RuntimeException("路径推荐不存在: " + recommendId);
        recommend.setStatus(RecommendStatus.REJECTED.getCode());
        recommend.setReason(reason != null ? reason : recommend.getReason());
        recommend.setOperator(operator);
        recommend.setOperateTime(LocalDateTime.now());
        pathRecommendMapper.updateById(recommend);
        log.info("拒绝路径推荐: id={}, wave={}, reason={}", recommendId, recommend.getWaveId(), reason);
        return pathRecommendMapper.selectByRecommendId(recommendId);
    }

    /** 完成路径推荐（记录实际路径） */
    @Transactional(rollbackFor = Exception.class)
    public PathRecommend completePathRecommend(
            String recommendId,
            String actualPath,
            BigDecimal actualDistance,
            Long actualTime,
            String operator) {
        PathRecommend recommend = pathRecommendMapper.selectByRecommendId(recommendId);
        if (recommend == null) throw new RuntimeException("路径推荐不存在: " + recommendId);
        recommend.setStatus(RecommendStatus.EXECUTED.getCode());
        recommend.setActualPath(actualPath);
        recommend.setActualDistance(actualDistance);
        recommend.setActualTime(actualTime);
        recommend.setOperator(operator);
        recommend.setOperateTime(LocalDateTime.now());
        pathRecommendMapper.updateById(recommend);

        // 计算路径优化率
        if (recommend.getEstimateDistance() != null
                && actualDistance != null
                && recommend.getEstimateDistance().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal optimizationRate =
                    actualDistance
                            .subtract(recommend.getEstimateDistance())
                            .divide(recommend.getEstimateDistance(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
            log.info(
                    "路径推荐完成: id={}, 优化率={}%, 预估距离={}, 实际距离={}",
                    recommendId, optimizationRate, recommend.getEstimateDistance(), actualDistance);
        }
        return pathRecommendMapper.selectByRecommendId(recommendId);
    }

    public PathRecommend getPathRecommendById(String recommendId) {
        return pathRecommendMapper.selectByRecommendId(recommendId);
    }

    public List<PathRecommend> getPathByWaveId(String waveId) {
        return pathRecommendMapper.selectByWaveId(waveId);
    }

    public List<PathRecommend> getPendingPathByPicker(String pickerId) {
        return pathRecommendMapper.selectPendingByPicker(pickerId);
    }

    public List<PathRecommend> getPathByWarehouseAndStatus(String warehouseCode, String status) {
        return pathRecommendMapper.selectByWarehouseAndStatus(warehouseCode, status);
    }

    public Page<PathRecommend> pagePath(
            Page<PathRecommend> page,
            String warehouseCode,
            String waveId,
            String pickerId,
            String status) {
        LambdaQueryWrapper<PathRecommend> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(PathRecommend::getWarehouseCode, warehouseCode);
        if (waveId != null) wrapper.eq(PathRecommend::getWaveId, waveId);
        if (pickerId != null) wrapper.eq(PathRecommend::getPickerId, pickerId);
        if (status != null) wrapper.eq(PathRecommend::getStatus, status);
        wrapper.orderByDesc(PathRecommend::getRecommendTime);
        return pathRecommendMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateRecommendId(String prefix) {
        return prefix
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
