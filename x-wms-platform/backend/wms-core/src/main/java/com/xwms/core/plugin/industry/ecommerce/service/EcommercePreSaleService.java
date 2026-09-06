package com.xwms.core.plugin.industry.ecommerce.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 电商预售预占服务 核心能力： 1. 预售订单库存预占（支付前锁定，支付后转正式预占） 2. 预占超时自动释放（未支付订单30分钟释放） 3. 大促库存池隔离（活动库存与日常库存分离） 4.
 * 库存扣减优先级（预售>现货>预约）
 */
@Slf4j
@Service
public class EcommercePreSaleService {

    /** 预占超时时间（分钟） */
    private static final int ALLOCATE_TIMEOUT_MINUTES = 30;

    /** 活动库存池：activityId -> (sku -> 活动库存数量) */
    private final Map<String, Map<String, Integer>> activityStockPool = new ConcurrentHashMap<>();

    /** 预售预占库存 预售阶段：先预占活动库存池，不扣减实际库存 */
    public boolean preAllocate(String activityId, String sku, int qty, String orderNo) {
        Map<String, Integer> pool =
                activityStockPool.computeIfAbsent(activityId, k -> new ConcurrentHashMap<>());
        int available = pool.getOrDefault(sku, 0);
        if (available < qty) {
            log.warn(
                    "预售活动库存不足: activity={}, sku={}, need={}, avail={}",
                    activityId,
                    sku,
                    qty,
                    available);
            return false;
        }
        pool.put(sku, available - qty);
        log.info(
                "预售预占成功: activity={}, sku={}, qty={}, orderNo={}, 剩余活动库存={}",
                activityId,
                sku,
                qty,
                orderNo,
                available - qty);
        return true;
    }

    /** 预售转正式：支付成功后，活动预占转为正式库存预占 */
    public void convertToFormal(String activityId, String sku, int qty, String orderNo) {
        log.info("预售转正式预占: activity={}, sku={}, qty={}, orderNo={}", activityId, sku, qty, orderNo);
        // TODO: 调用库存服务正式预占（Redis预占+Oracle allocated_qty增加）
    }

    /** 预占超时释放（定时任务扫描） */
    public void releaseTimeoutAllocate() {
        log.info("扫描超时预占订单，超时阈值={}分钟", ALLOCATE_TIMEOUT_MINUTES);
        // TODO: 查询超过30分钟未支付的预售订单，释放活动库存池
    }

    /** 大促库存池初始化 */
    public void initActivityPool(String activityId, Map<String, Integer> skuStock) {
        activityStockPool.put(activityId, new ConcurrentHashMap<>(skuStock));
        log.info(
                "大促活动库存池初始化: activity={}, SKU数={}, 总库存={}",
                activityId,
                skuStock.size(),
                skuStock.values().stream().mapToInt(Integer::intValue).sum());
    }

    /** 获取活动剩余库存 */
    public int getActivityStock(String activityId, String sku) {
        return activityStockPool.getOrDefault(activityId, Map.of()).getOrDefault(sku, 0);
    }
}
