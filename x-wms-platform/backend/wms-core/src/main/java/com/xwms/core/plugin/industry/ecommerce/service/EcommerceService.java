package com.xwms.core.plugin.industry.ecommerce.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.plugin.industry.ecommerce.entity.*;
import com.xwms.core.plugin.industry.ecommerce.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 电商行业服务 包含预售订单、秒杀活动、电商波次配置 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcommerceService {

    private final PreSaleOrderMapper preSaleOrderMapper;
    private final FlashSaleActivityMapper flashSaleActivityMapper;
    private final EcommerceWaveConfigMapper waveConfigMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 预售订单管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public PreSaleOrder createPreSaleOrder(PreSaleOrder order) {
        order.setPreSaleNo(generatePreSaleNo());
        order.setStatus("PENDING");
        order.setDepositStatus("UNPAID");
        order.setBalanceStatus("UNPAID");
        order.setShipStatus("PENDING");
        order.setShippedQty(BigDecimal.ZERO);
        order.setCreatedTime(LocalDateTime.now());
        preSaleOrderMapper.insert(order);
        log.info("创建预售订单: {}", order.getPreSaleNo());
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public PreSaleOrder payDeposit(String preSaleNo, String operator) {
        PreSaleOrder order = preSaleOrderMapper.selectByPreSaleNo(preSaleNo);
        if (order == null) {
            throw new RuntimeException("预售订单不存在: " + preSaleNo);
        }
        order.setDepositStatus("PAID");
        order.setStatus("ACTIVE");
        order.setUpdatedTime(LocalDateTime.now());
        preSaleOrderMapper.updateById(order);
        log.info("预售订单支付定金: {}", preSaleNo);
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public PreSaleOrder payBalance(String preSaleNo, String operator) {
        PreSaleOrder order = preSaleOrderMapper.selectByPreSaleNo(preSaleNo);
        if (order == null) {
            throw new RuntimeException("预售订单不存在: " + preSaleNo);
        }
        if (!"PAID".equals(order.getDepositStatus())) {
            throw new RuntimeException("定金未支付，无法支付尾款: " + preSaleNo);
        }
        order.setBalanceStatus("PAID");
        order.setStatus("ENDED");
        order.setUpdatedTime(LocalDateTime.now());
        preSaleOrderMapper.updateById(order);
        log.info("预售订单支付尾款: {}", preSaleNo);
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public PreSaleOrder shipPreSaleOrder(String preSaleNo, BigDecimal shipQty, String operator) {
        PreSaleOrder order = preSaleOrderMapper.selectByPreSaleNo(preSaleNo);
        if (order == null) {
            throw new RuntimeException("预售订单不存在: " + preSaleNo);
        }
        if (!"PAID".equals(order.getBalanceStatus())) {
            throw new RuntimeException("尾款未支付，无法发货: " + preSaleNo);
        }
        BigDecimal newShippedQty = order.getShippedQty().add(shipQty);
        if (newShippedQty.compareTo(order.getQuantity()) > 0) {
            throw new RuntimeException("发货数量超过预售数量: " + preSaleNo);
        }
        order.setShippedQty(newShippedQty);
        if (newShippedQty.compareTo(order.getQuantity()) == 0) {
            order.setShipStatus("SHIPPED");
            order.setActualShipTime(LocalDateTime.now());
        } else {
            order.setShipStatus("PARTIAL");
        }
        order.setUpdatedTime(LocalDateTime.now());
        preSaleOrderMapper.updateById(order);
        log.info("预售订单发货: {}, 发货数量: {}", preSaleNo, shipQty);
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public PreSaleOrder cancelPreSaleOrder(String preSaleNo, String reason, String operator) {
        PreSaleOrder order = preSaleOrderMapper.selectByPreSaleNo(preSaleNo);
        if (order == null) {
            throw new RuntimeException("预售订单不存在: " + preSaleNo);
        }
        order.setStatus("CANCELLED");
        order.setRemark(reason);
        order.setUpdatedTime(LocalDateTime.now());
        preSaleOrderMapper.updateById(order);
        log.info("取消预售订单: {}, 原因: {}", preSaleNo, reason);
        return order;
    }

    public PreSaleOrder getPreSaleOrder(Long id) {
        return preSaleOrderMapper.selectById(id);
    }

    public PreSaleOrder getPreSaleOrderByNo(String preSaleNo) {
        return preSaleOrderMapper.selectByPreSaleNo(preSaleNo);
    }

    public PreSaleOrder getPreSaleOrderByOrderNo(String orderNo) {
        return preSaleOrderMapper.selectByOrderNo(orderNo);
    }

    public List<PreSaleOrder> getPendingBalanceOrders() {
        return preSaleOrderMapper.selectPendingBalanceOrders();
    }

    public List<PreSaleOrder> getPendingShipOrders() {
        return preSaleOrderMapper.selectPendingShipOrders();
    }

    public Page<PreSaleOrder> pagePreSaleOrders(
            Page<PreSaleOrder> page,
            String shopCode,
            String platformCode,
            String status,
            String shipStatus) {
        LambdaQueryWrapper<PreSaleOrder> wrapper = new LambdaQueryWrapper<>();
        if (shopCode != null) wrapper.eq(PreSaleOrder::getShopCode, shopCode);
        if (platformCode != null) wrapper.eq(PreSaleOrder::getPlatformCode, platformCode);
        if (status != null) wrapper.eq(PreSaleOrder::getStatus, status);
        if (shipStatus != null) wrapper.eq(PreSaleOrder::getShipStatus, shipStatus);
        wrapper.orderByDesc(PreSaleOrder::getCreatedTime);
        return preSaleOrderMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. 秒杀活动管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public FlashSaleActivity createFlashSaleActivity(FlashSaleActivity activity) {
        activity.setActivityNo(generateActivityNo());
        activity.setStatus("DRAFT");
        activity.setStockLockStatus("NOT_LOCKED");
        activity.setSoldQty(BigDecimal.ZERO);
        activity.setRemainingQty(activity.getActivityStock());
        activity.setCreatedTime(LocalDateTime.now());
        flashSaleActivityMapper.insert(activity);
        log.info("创建秒杀活动: {}", activity.getActivityNo());
        return activity;
    }

    @Transactional(rollbackFor = Exception.class)
    public FlashSaleActivity publishActivity(Long activityId) {
        FlashSaleActivity activity = flashSaleActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("秒杀活动不存在: " + activityId);
        }
        activity.setStatus("PENDING");
        activity.setUpdatedTime(LocalDateTime.now());
        flashSaleActivityMapper.updateById(activity);
        log.info("发布秒杀活动: {}", activity.getActivityNo());
        return activity;
    }

    @Transactional(rollbackFor = Exception.class)
    public FlashSaleActivity startActivity(Long activityId) {
        FlashSaleActivity activity = flashSaleActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("秒杀活动不存在: " + activityId);
        }
        activity.setStatus("ACTIVE");
        activity.setUpdatedTime(LocalDateTime.now());
        flashSaleActivityMapper.updateById(activity);
        log.info("开始秒杀活动: {}", activity.getActivityNo());
        return activity;
    }

    @Transactional(rollbackFor = Exception.class)
    public FlashSaleActivity endActivity(Long activityId) {
        FlashSaleActivity activity = flashSaleActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("秒杀活动不存在: " + activityId);
        }
        activity.setStatus("ENDED");
        activity.setUpdatedTime(LocalDateTime.now());
        flashSaleActivityMapper.updateById(activity);
        log.info("结束秒杀活动: {}", activity.getActivityNo());
        return activity;
    }

    @Transactional(rollbackFor = Exception.class)
    public FlashSaleActivity lockStock(Long activityId, String waveNo) {
        FlashSaleActivity activity = flashSaleActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("秒杀活动不存在: " + activityId);
        }
        activity.setStockLockStatus("LOCKED");
        activity.setLockTime(LocalDateTime.now());
        activity.setWaveNo(waveNo);
        activity.setUpdatedTime(LocalDateTime.now());
        flashSaleActivityMapper.updateById(activity);
        log.info("锁定秒杀活动库存: {}, 波次: {}", activity.getActivityNo(), waveNo);
        return activity;
    }

    @Transactional(rollbackFor = Exception.class)
    public FlashSaleActivity releaseStock(Long activityId) {
        FlashSaleActivity activity = flashSaleActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("秒杀活动不存在: " + activityId);
        }
        activity.setStockLockStatus("RELEASED");
        activity.setUpdatedTime(LocalDateTime.now());
        flashSaleActivityMapper.updateById(activity);
        log.info("释放秒杀活动库存: {}", activity.getActivityNo());
        return activity;
    }

    @Transactional(rollbackFor = Exception.class)
    public FlashSaleActivity sellStock(Long activityId, BigDecimal qty) {
        FlashSaleActivity activity = flashSaleActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("秒杀活动不存在: " + activityId);
        }
        BigDecimal newSoldQty = activity.getSoldQty().add(qty);
        if (newSoldQty.compareTo(activity.getActivityStock()) > 0) {
            throw new RuntimeException("秒杀库存不足: " + activity.getActivityNo());
        }
        activity.setSoldQty(newSoldQty);
        activity.setRemainingQty(activity.getActivityStock().subtract(newSoldQty));
        activity.setUpdatedTime(LocalDateTime.now());
        flashSaleActivityMapper.updateById(activity);
        log.info("秒杀活动售出: {}, 数量: {}", activity.getActivityNo(), qty);
        return activity;
    }

    public FlashSaleActivity getActivity(Long id) {
        return flashSaleActivityMapper.selectById(id);
    }

    public FlashSaleActivity getActivityByNo(String activityNo) {
        return flashSaleActivityMapper.selectByActivityNo(activityNo);
    }

    public List<FlashSaleActivity> getActiveActivities() {
        return flashSaleActivityMapper.selectActiveActivities();
    }

    public List<FlashSaleActivity> getPendingActivities() {
        return flashSaleActivityMapper.selectPendingActivities();
    }

    public List<FlashSaleActivity> getUnlockedStockActivities() {
        return flashSaleActivityMapper.selectUnlockedStockActivities();
    }

    public Page<FlashSaleActivity> pageActivities(
            Page<FlashSaleActivity> page,
            String shopCode,
            String platformCode,
            String activityType,
            String status) {
        LambdaQueryWrapper<FlashSaleActivity> wrapper = new LambdaQueryWrapper<>();
        if (shopCode != null) wrapper.eq(FlashSaleActivity::getShopCode, shopCode);
        if (platformCode != null) wrapper.eq(FlashSaleActivity::getPlatformCode, platformCode);
        if (activityType != null) wrapper.eq(FlashSaleActivity::getActivityType, activityType);
        if (status != null) wrapper.eq(FlashSaleActivity::getStatus, status);
        wrapper.orderByDesc(FlashSaleActivity::getStartTime);
        return flashSaleActivityMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 3. 电商波次配置管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public EcommerceWaveConfig createWaveConfig(EcommerceWaveConfig config) {
        config.setConfigCode(generateWaveConfigNo());
        config.setEnabled("Y");
        config.setCreatedTime(LocalDateTime.now());
        waveConfigMapper.insert(config);
        log.info("创建电商波次配置: {}", config.getConfigCode());
        return config;
    }

    @Transactional(rollbackFor = Exception.class)
    public EcommerceWaveConfig updateWaveConfig(EcommerceWaveConfig config) {
        config.setUpdatedTime(LocalDateTime.now());
        waveConfigMapper.updateById(config);
        return waveConfigMapper.selectById(config.getId());
    }

    public EcommerceWaveConfig getWaveConfig(Long id) {
        return waveConfigMapper.selectById(id);
    }

    public EcommerceWaveConfig getWaveConfigByCode(String configCode) {
        return waveConfigMapper.selectByConfigCode(configCode);
    }

    public List<EcommerceWaveConfig> getEnabledWaveConfigs() {
        return waveConfigMapper.selectEnabledConfigs();
    }

    public List<EcommerceWaveConfig> getWaveConfigsByShopAndActivityType(
            String shopCode, String activityType) {
        return waveConfigMapper.selectByShopAndActivityType(shopCode, activityType);
    }

    public List<EcommerceWaveConfig> getEnabledWaveConfigsByActivityType(String activityType) {
        return waveConfigMapper.selectEnabledByActivityType(activityType);
    }

    public Page<EcommerceWaveConfig> pageWaveConfigs(
            Page<EcommerceWaveConfig> page,
            String shopCode,
            String platformCode,
            String activityType,
            String enabled) {
        LambdaQueryWrapper<EcommerceWaveConfig> wrapper = new LambdaQueryWrapper<>();
        if (shopCode != null) wrapper.eq(EcommerceWaveConfig::getShopCode, shopCode);
        if (platformCode != null) wrapper.eq(EcommerceWaveConfig::getPlatformCode, platformCode);
        if (activityType != null) wrapper.eq(EcommerceWaveConfig::getActivityType, activityType);
        if (enabled != null) wrapper.eq(EcommerceWaveConfig::getEnabled, enabled);
        wrapper.orderByAsc(EcommerceWaveConfig::getPriority);
        return waveConfigMapper.selectPage(page, wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public EcommerceWaveConfig enableWaveConfig(Long id) {
        EcommerceWaveConfig config = waveConfigMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("波次配置不存在: " + id);
        }
        config.setEnabled("Y");
        config.setUpdatedTime(LocalDateTime.now());
        waveConfigMapper.updateById(config);
        return config;
    }

    @Transactional(rollbackFor = Exception.class)
    public EcommerceWaveConfig disableWaveConfig(Long id) {
        EcommerceWaveConfig config = waveConfigMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("波次配置不存在: " + id);
        }
        config.setEnabled("N");
        config.setUpdatedTime(LocalDateTime.now());
        waveConfigMapper.updateById(config);
        return config;
    }

    // ============================================================

    // 4. 编号生成
    // ============================================================

    private String generatePreSaleNo() {
        return "EPS"
                + NO_FMT.format(LocalDateTime.now())
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateActivityNo() {
        return "EFA"
                + NO_FMT.format(LocalDateTime.now())
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateWaveConfigNo() {
        return "EWC"
                + NO_FMT.format(LocalDateTime.now())
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
