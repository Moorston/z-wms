package com.xwms.core.allocation.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.allocation.entity.*;
import com.xwms.core.allocation.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库存分配策略管理核心服务 核心能力: 分配规则/分配策略/分配执行/分配释放/分配查询 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryAllocationService {

    private final AllocationRuleMapper ruleMapper;
    private final AllocationStrategyMapper strategyMapper;
    private final AllocationDetailMapper detailMapper;
    private final AllocationLogMapper logMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 分配规则管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public AllocationRule createRule(AllocationRule rule, AllocationStrategy strategy) {
        rule.setStatus("ACTIVE");
        if (rule.getPriority() == null) rule.setPriority(5);
        ruleMapper.insert(rule);

        if (strategy != null) {
            strategy.setRuleCode(rule.getRuleCode());
            strategy.setStatus("ACTIVE");
            if (strategy.getAllowSplit() == null) strategy.setAllowSplit("Y");
            if (strategy.getAutoRelease() == null) strategy.setAutoRelease("Y");
            strategyMapper.insert(strategy);
        }

        log.info(
                "创建分配规则: {}={}, strategy={}",
                rule.getRuleCode(),
                rule.getRuleName(),
                strategy != null ? strategy.getStrategyType() : "NONE");
        return rule;
    }

    public AllocationRule getRuleByCode(String ruleCode) {
        return ruleMapper.selectByRuleCode(ruleCode);
    }

    public AllocationStrategy getStrategyByRuleCode(String ruleCode) {
        return strategyMapper.selectByRuleCode(ruleCode);
    }

    public AllocationRule matchRule(
            String warehouseCode,
            String ownerCode,
            String categoryCode,
            String skuCode,
            String orderType) {
        return ruleMapper.matchRule(warehouseCode, ownerCode, categoryCode, skuCode, orderType);
    }

    public Page<AllocationRule> pageRules(Page<AllocationRule> page, String warehouseCode) {
        LambdaQueryWrapper<AllocationRule> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(AllocationRule::getWarehouseCode, warehouseCode);
        wrapper.eq(AllocationRule::getStatus, "ACTIVE");
        return ruleMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. 分配执行（核心）
    // ============================================================

    /**
     * 执行库存分配
     *
     * @param orderNo 订单号
     * @param warehouseCode 仓库
     * @param ownerCode 货主
     * @param items 分配明细列表
     * @param operator 操作人
     * @return 分配单号
     */
    @Transactional(rollbackFor = Exception.class)
    public String allocate(
            String orderNo,
            String warehouseCode,
            String ownerCode,
            List<Map<String, Object>> items,
            String operator) {
        String allocationNo = generateAllocationNo();

        for (Map<String, Object> item : items) {
            String skuCode = (String) item.get("skuCode");
            BigDecimal needQty = toBigDecimal(item.get("quantity"));
            Integer orderLine =
                    item.get("orderLine") != null
                            ? Integer.parseInt(item.get("orderLine").toString())
                            : null;

            // 匹配分配规则和策略
            AllocationRule rule =
                    ruleMapper.matchRule(warehouseCode, ownerCode, null, skuCode, null);
            AllocationStrategy strategy =
                    rule != null ? strategyMapper.selectByRuleCode(rule.getRuleCode()) : null;
            String strategyType = strategy != null ? strategy.getStrategyType() : "FIFO";

            // 获取可用库存并按策略排序
            List<Map<String, Object>> availableInventory =
                    (List<Map<String, Object>>) item.get("availableInventory");
            if (availableInventory == null) {
                availableInventory = new ArrayList<>();
            }
            sortInventory(availableInventory, strategyType);

            // 按策略分配
            BigDecimal remaining = needQty;
            for (Map<String, Object> inv : availableInventory) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal available = toBigDecimal(inv.get("availableQty"));
                if (available.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal allocQty = available.min(remaining);

                // 检查是否允许拆单
                if ("N".equals(strategy != null ? strategy.getAllowSplit() : "Y")
                        && allocQty.compareTo(remaining) < 0
                        && allocQty.compareTo(available) < 0) {
                    continue; // 不允许拆单，跳过
                }

                AllocationDetail detail = new AllocationDetail();
                detail.setAllocationNo(allocationNo);
                detail.setOrderNo(orderNo);
                detail.setOrderLine(orderLine);
                detail.setWarehouseCode(warehouseCode);
                detail.setOwnerCode(ownerCode);
                detail.setSkuCode(skuCode);
                detail.setSkuName((String) item.get("skuName"));
                detail.setLocationCode((String) inv.get("locationCode"));
                detail.setBatchNo((String) inv.get("batchNo"));
                detail.setSerialNo((String) inv.get("serialNo"));
                detail.setContainerNo((String) inv.get("containerNo"));
                detail.setAllocatedQty(allocQty);
                detail.setPickedQty(BigDecimal.ZERO);
                detail.setRemainQty(allocQty);
                detail.setUnitCost(toBigDecimal(inv.get("unitCost")));
                detail.setTotalCost(
                        toBigDecimal(inv.get("unitCost")) != null
                                ? toBigDecimal(inv.get("unitCost")).multiply(allocQty)
                                : null);
                detail.setStatus("ALLOCATED");
                detail.setAllocateTime(LocalDateTime.now());
                detail.setOperator(operator);
                detailMapper.insert(detail);

                // 记录分配日志
                recordLog(
                        allocationNo,
                        orderNo,
                        skuCode,
                        "ALLOCATE",
                        BigDecimal.ZERO,
                        allocQty,
                        allocQty,
                        (String) inv.get("locationCode"),
                        (String) inv.get("batchNo"),
                        "自动分配",
                        operator);

                remaining = remaining.subtract(allocQty);
            }

            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                log.warn(
                        "库存分配不足: order={}, sku={}, need={}, allocated={}",
                        orderNo,
                        skuCode,
                        needQty,
                        needQty.subtract(remaining));
            }
        }

        log.info(
                "执行库存分配: allocationNo={}, order={}, items={}", allocationNo, orderNo, items.size());
        return allocationNo;
    }

    /** 按策略排序库存 */
    private void sortInventory(List<Map<String, Object>> inventory, String strategyType) {
        Comparator<Map<String, Object>> comparator;
        switch (strategyType) {
            case "FEFO":
                comparator = Comparator.comparing(m -> (String) m.getOrDefault("expiryDate", ""));
                break;
            case "LIFO":
                comparator = Comparator.comparing(m -> (String) m.getOrDefault("inboundDate", ""));
                comparator = comparator.reversed();
                break;
            case "LEFO":
                comparator = Comparator.comparing(m -> (String) m.getOrDefault("expiryDate", ""));
                comparator = comparator.reversed();
                break;
            case "FIFO":
            default:
                comparator = Comparator.comparing(m -> (String) m.getOrDefault("inboundDate", ""));
                break;
        }
        inventory.sort(comparator);
    }

    // ============================================================

    // 3. 重新分配
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public String reallocate(
            String orderNo,
            String warehouseCode,
            String ownerCode,
            List<Map<String, Object>> items,
            String operator,
            String reason) {
        // 先释放原分配
        List<AllocationDetail> oldDetails = detailMapper.selectByOrderNo(orderNo);
        for (AllocationDetail detail : oldDetails) {
            if ("ALLOCATED".equals(detail.getStatus()) || "PICKING".equals(detail.getStatus())) {
                detailMapper.updateStatus(
                        detail.getId(), "RELEASED", detail.getPickedQty(), BigDecimal.ZERO);
                recordLog(
                        detail.getAllocationNo(),
                        orderNo,
                        detail.getSkuCode(),
                        "RELEASE",
                        detail.getAllocatedQty(),
                        BigDecimal.ZERO,
                        detail.getAllocatedQty().negate(),
                        detail.getLocationCode(),
                        detail.getBatchNo(),
                        reason,
                        operator);
            }
        }

        // 重新分配
        return allocate(orderNo, warehouseCode, ownerCode, items, operator);
    }

    // ============================================================

    // 4. 释放分配
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public int releaseAllocation(String allocationNo, String reason, String operator) {
        List<AllocationDetail> details = detailMapper.selectByAllocationNo(allocationNo);
        int count = 0;
        for (AllocationDetail detail : details) {
            if ("ALLOCATED".equals(detail.getStatus()) || "PICKING".equals(detail.getStatus())) {
                detailMapper.updateStatus(
                        detail.getId(), "RELEASED", detail.getPickedQty(), BigDecimal.ZERO);
                recordLog(
                        allocationNo,
                        detail.getOrderNo(),
                        detail.getSkuCode(),
                        "RELEASE",
                        detail.getAllocatedQty(),
                        BigDecimal.ZERO,
                        detail.getAllocatedQty().negate(),
                        detail.getLocationCode(),
                        detail.getBatchNo(),
                        reason,
                        operator);
                count++;
            }
        }
        log.info("释放分配: allocationNo={}, count={}, reason={}", allocationNo, count, reason);
        return count;
    }

    @Transactional(rollbackFor = Exception.class)
    public int releaseByOrderNo(String orderNo, String reason, String operator) {
        List<AllocationDetail> details = detailMapper.selectByOrderNo(orderNo);
        int count = 0;
        Set<String> allocationNos = new HashSet<>();
        for (AllocationDetail detail : details) {
            allocationNos.add(detail.getAllocationNo());
        }
        for (String allocationNo : allocationNos) {
            count += releaseAllocation(allocationNo, reason, operator);
        }
        return count;
    }

    // ============================================================

    // 5. 拣货确认
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public AllocationDetail confirmPick(Long detailId, BigDecimal pickedQty, String operator) {
        AllocationDetail detail = detailMapper.selectById(detailId);
        if (detail == null) throw new RuntimeException("分配明细不存在: " + detailId);
        if (!"ALLOCATED".equals(detail.getStatus()) && !"PICKING".equals(detail.getStatus())) {
            throw new RuntimeException("分配明细状态不正确: " + detail.getStatus());
        }

        BigDecimal newPickedQty = detail.getPickedQty().add(pickedQty);
        BigDecimal newRemainQty = detail.getAllocatedQty().subtract(newPickedQty);
        String newStatus = newRemainQty.compareTo(BigDecimal.ZERO) <= 0 ? "PICKED" : "PICKING";

        detailMapper.updateStatus(detailId, newStatus, newPickedQty, newRemainQty);

        recordLog(
                detail.getAllocationNo(),
                detail.getOrderNo(),
                detail.getSkuCode(),
                "PICK",
                detail.getRemainQty(),
                newRemainQty,
                pickedQty.negate(),
                detail.getLocationCode(),
                detail.getBatchNo(),
                "拣货确认",
                operator);

        log.info("拣货确认: detailId={}, picked={}, remain={}", detailId, pickedQty, newRemainQty);
        return detailMapper.selectById(detailId);
    }

    // ============================================================

    // 6. 超时自动释放
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public int autoReleaseTimeout() {
        // 查询超时的分配（超过预占保留时间）
        List<AllocationDetail> timeoutDetails =
                detailMapper.selectList(
                        new LambdaQueryWrapper<AllocationDetail>()
                                .in(AllocationDetail::getStatus, "ALLOCATED", "PICKING")
                                .lt(
                                        AllocationDetail::getAllocateTime,
                                        LocalDateTime.now().minusHours(24))); // 默认24小时

        int count = 0;
        Map<String, String> allocationReasons = new HashMap<>();
        for (AllocationDetail detail : timeoutDetails) {
            if (!allocationReasons.containsKey(detail.getAllocationNo())) {
                releaseAllocation(detail.getAllocationNo(), "超时自动释放", "SYSTEM");
                allocationReasons.put(detail.getAllocationNo(), "released");
                count++;
            }
        }
        log.info("超时自动释放: count={}", count);
        return count;
    }

    // ============================================================

    // 7. 查询
    // ============================================================

    public List<AllocationDetail> getDetailsByAllocationNo(String allocationNo) {
        return detailMapper.selectByAllocationNo(allocationNo);
    }

    public List<AllocationDetail> getDetailsByOrderNo(String orderNo) {
        return detailMapper.selectByOrderNo(orderNo);
    }

    public List<AllocationDetail> getActiveBySku(String skuCode) {
        return detailMapper.selectActiveBySku(skuCode);
    }

    public List<AllocationDetail> getActiveByLocation(String locationCode) {
        return detailMapper.selectActiveByLocation(locationCode);
    }

    public Page<AllocationDetail> pageDetails(
            Page<AllocationDetail> page, String warehouseCode, String skuCode, String status) {
        LambdaQueryWrapper<AllocationDetail> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(AllocationDetail::getWarehouseCode, warehouseCode);
        if (skuCode != null) wrapper.eq(AllocationDetail::getSkuCode, skuCode);
        if (status != null) wrapper.eq(AllocationDetail::getStatus, status);
        wrapper.orderByDesc(AllocationDetail::getAllocateTime);
        return detailMapper.selectPage(page, wrapper);
    }

    public List<AllocationLog> getLogsByAllocationNo(String allocationNo) {
        return logMapper.selectByAllocationNo(allocationNo);
    }

    public List<AllocationLog> getLogsByOrderNo(String orderNo) {
        return logMapper.selectByOrderNo(orderNo);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private void recordLog(
            String allocationNo,
            String orderNo,
            String skuCode,
            String actionType,
            BigDecimal beforeQty,
            BigDecimal afterQty,
            BigDecimal changeQty,
            String locationCode,
            String batchNo,
            String reason,
            String operator) {
        AllocationLog log = new AllocationLog();
        log.setLogId(generateLogId());
        log.setAllocationNo(allocationNo);
        log.setOrderNo(orderNo);
        log.setSkuCode(skuCode);
        log.setActionType(actionType);
        log.setBeforeQty(beforeQty);
        log.setAfterQty(afterQty);
        log.setChangeQty(changeQty);
        log.setLocationCode(locationCode);
        log.setBatchNo(batchNo);
        log.setReason(reason);
        log.setOperator(operator);
        log.setOperateTime(LocalDateTime.now());
        logMapper.insert(log);
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        return new BigDecimal(obj.toString());
    }

    private String generateAllocationNo() {
        return "ALC"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateLogId() {
        return "ALG"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
