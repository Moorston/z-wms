package com.xwms.core.rotation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.rotation.entity.*;
import com.xwms.core.rotation.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 搴撳瓨鍛ㄨ浆绠＄悊鏍稿績鏈嶅姟 鏍稿績鑳藉姏: 鍛ㄨ浆瑙勫垯/鍛ㄨ浆闃熷垪/鏁堟湡棰勮/鍛ㄨ浆鍒嗘瀽 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RotationService {

    private final RotationRuleMapper ruleMapper;
    private final RotationQueueMapper queueMapper;
    private final RotationAnalysisMapper analysisMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 鍛ㄨ浆瑙勫垯绠＄悊
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public RotationRule createRule(RotationRule rule) {
        rule.setStatus("ACTIVE");
        if (rule.getPriority() == null) rule.setPriority(5);
        if (rule.getSortOrder() == null) rule.setSortOrder("ASC");
        ruleMapper.insert(rule);
        log.info(
                "鍒涘缓鍛ㄨ浆瑙勫垯: {}={}, type={}",
                rule.getRuleCode(),
                rule.getRuleName(),
                rule.getRotationType());
        return rule;
    }

    public Page<RotationRule> pageRules(
            Page<RotationRule> page, String skuCode, String rotationType) {
        LambdaQueryWrapper<RotationRule> wrapper = new LambdaQueryWrapper<>();
        if (skuCode != null) wrapper.eq(RotationRule::getSkuCode, skuCode);
        if (rotationType != null) wrapper.eq(RotationRule::getRotationType, rotationType);
        wrapper.eq(RotationRule::getStatus, "ACTIVE");
        wrapper.orderByDesc(RotationRule::getPriority);
        return ruleMapper.selectPage(page, wrapper);
    }

    public RotationRule getRuleByCode(String ruleCode) {
        return ruleMapper.selectByRuleCode(ruleCode);
    }

    /** 鍖归厤SKU鐨勫懆杞鍒? */
    public RotationRule matchRule(String skuCode, String categoryCode, String ownerCode) {
        RotationRule rule = ruleMapper.matchRule(skuCode, categoryCode, ownerCode);
        if (rule == null) {
            // 榛樿FIFO
            log.debug("鏈尮閰嶅埌鍛ㄨ浆瑙勫垯锛屼娇鐢ㄩ粯璁IFO: sku={}", skuCode);
        }
        return rule;
    }

    // ============================================================

    // 2. 鍛ㄨ浆闃熷垪绠＄悊
    // ============================================================

    /** 鍏ュ簱鏃舵坊鍔犲埌鍛ㄨ浆闃熷垪 */
    @Transactional(rollbackFor = Exception.class)
    public RotationQueue addToQueue(
            String skuCode,
            String batchNo,
            String locationCode,
            String ownerCode,
            BigDecimal quantity,
            LocalDateTime productionDate,
            LocalDateTime expireDate,
            LocalDateTime receiveDate) {
        String queueId = buildQueueId(skuCode, locationCode, ownerCode);

        // 鍖归厤鍛ㄨ浆瑙勫垯锛岃绠楁帓搴忓€?
        RotationRule rule = matchRule(skuCode, null, ownerCode);
        String sortValue =
                calculateSortValue(rule, productionDate, expireDate, receiveDate, batchNo);

        RotationQueue queue = new RotationQueue();
        queue.setQueueId(queueId);
        queue.setSkuCode(skuCode);
        queue.setBatchNo(batchNo);
        queue.setLocationCode(locationCode);
        queue.setOwnerCode(ownerCode);
        queue.setQuantity(quantity);
        queue.setProductionDate(productionDate);
        queue.setExpireDate(expireDate);
        queue.setReceiveDate(receiveDate);
        queue.setSortValue(sortValue);
        queue.setStatus("ACTIVE");
        queueMapper.insert(queue);

        // 閲嶆柊璁＄畻鎺掑簭搴忓彿
        recalculateSortOrder(queueId);

        log.info(
                "娣诲姞鍒板懆杞槦鍒? sku={}, batch={}, location={}, qty={}",
                skuCode,
                batchNo,
                locationCode,
                quantity);
        return queue;
    }

    /** 鍑哄簱鏃朵粠鍛ㄨ浆闃熷垪鎵ｅ噺锛堟寜鍛ㄨ浆椤哄簭锛? */
    @Transactional(rollbackFor = Exception.class)
    public List<RotationQueue> allocateFromQueue(
            String skuCode, String locationCode, String ownerCode, BigDecimal needQty) {
        String queueId = buildQueueId(skuCode, locationCode, ownerCode);
        List<RotationQueue> queues = queueMapper.selectByQueueId(queueId);

        List<RotationQueue> allocated = new ArrayList<>();
        BigDecimal remaining = needQty;

        for (RotationQueue queue : queues) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            if (queue.getQuantity().compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal deductQty = queue.getQuantity().min(remaining);
            int rows = queueMapper.deductQuantity(queue.getId(), deductQty);
            if (rows > 0) {
                queue.setQuantity(queue.getQuantity().subtract(deductQty));
                allocated.add(queue);
                remaining = remaining.subtract(deductQty);
                log.info(
                        "浠庡懆杞槦鍒楀垎閰? queueId={}, batch={}, deduct={}",
                        queueId,
                        queue.getBatchNo(),
                        deductQty);
            }
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            log.warn(
                    "鍛ㄨ浆闃熷垪搴撳瓨涓嶈冻: sku={}, need={}, allocated={}",
                    skuCode,
                    needQty,
                    needQty.subtract(remaining));
        }

        return allocated;
    }

    /** 閲嶆柊璁＄畻鎺掑簭搴忓彿 */
    private void recalculateSortOrder(String queueId) {
        List<RotationQueue> queues = queueMapper.selectByQueueId(queueId);
        int order = 1;
        for (RotationQueue queue : queues) {
            queue.setSortOrder(order++);
            queueMapper.updateById(queue);
        }
    }

    /** 璁＄畻鎺掑簭鍊? */
    private String calculateSortValue(
            RotationRule rule,
            LocalDateTime productionDate,
            LocalDateTime expireDate,
            LocalDateTime receiveDate,
            String batchNo) {
        if (rule == null) {
            // 榛樿FIFO锛氭寜鏀惰揣鏃ユ湡
            return receiveDate != null ? receiveDate.toString() : batchNo;
        }

        String sortField = rule.getSortField();
        if (sortField == null) {
            sortField =
                    switch (rule.getRotationType()) {
                        case "FEFO", "FIFO_FEFO" -> "EXPIRE_DATE";
                        case "LIFO" -> "RECEIVE_DATE";
                        default -> "RECEIVE_DATE";
                    };
        }

        return switch (sortField) {
            case "PRODUCTION_DATE" -> productionDate != null ? productionDate.toString() : "";
            case "EXPIRE_DATE" -> expireDate != null ? expireDate.toString() : "";
            case "BATCH_NO" -> batchNo != null ? batchNo : "";
            default -> receiveDate != null ? receiveDate.toString() : "";
        };
    }

    private String buildQueueId(String skuCode, String locationCode, String ownerCode) {
        return skuCode + "_" + locationCode + "_" + (ownerCode != null ? ownerCode : "DEFAULT");
    }

    public List<RotationQueue> getQueueBySku(String skuCode) {
        return queueMapper.selectBySku(skuCode);
    }

    public List<RotationQueue> getQueueById(String queueId) {
        return queueMapper.selectByQueueId(queueId);
    }

    // ============================================================

    // ============================================================
    // 4. 鍛ㄨ浆鍒嗘瀽
    // ============================================================

    /** 璁＄畻鍛ㄨ浆鐜囧拰鍛ㄨ浆澶╂暟 鍛ㄨ浆鐜?= 鍑哄簱鏁伴噺 / 骞冲潎搴撳瓨 鍛ㄨ浆澶╂暟 = 缁熻澶╂暟 / 鍛ㄨ浆鐜? */
    @Transactional(rollbackFor = Exception.class)
    public RotationAnalysis calculateAnalysis(
            String skuCode,
            String warehouseCode,
            String ownerCode,
            LocalDateTime analysisDate,
            String periodType,
            BigDecimal openingQty,
            BigDecimal inboundQty,
            BigDecimal outboundQty,
            BigDecimal closingQty) {
        BigDecimal avgInventory =
                openingQty.add(closingQty).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        BigDecimal turnoverRate =
                avgInventory.compareTo(BigDecimal.ZERO) > 0
                        ? outboundQty.divide(avgInventory, 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

        int days =
                switch (periodType) {
                    case "WEEK" -> 7;
                    case "MONTH" -> 30;
                    default -> 1;
                };
        BigDecimal turnoverDays =
                turnoverRate.compareTo(BigDecimal.ZERO) > 0
                        ? BigDecimal.valueOf(days).divide(turnoverRate, 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

        RotationAnalysis analysis = new RotationAnalysis();
        analysis.setAnalysisDate(analysisDate);
        analysis.setPeriodType(periodType);
        analysis.setSkuCode(skuCode);
        analysis.setWarehouseCode(warehouseCode);
        analysis.setOwnerCode(ownerCode);
        analysis.setOpeningQty(openingQty);
        analysis.setInboundQty(inboundQty);
        analysis.setOutboundQty(outboundQty);
        analysis.setClosingQty(closingQty);
        analysis.setTurnoverRate(turnoverRate);
        analysis.setTurnoverDays(turnoverDays);
        analysis.setAvgInventory(avgInventory);
        analysisMapper.insert(analysis);

        log.info(
                "璁＄畻鍛ㄨ浆鍒嗘瀽: sku={}, period={}, rate={}, days={}",
                skuCode,
                periodType,
                turnoverRate,
                turnoverDays);
        return analysis;
    }

    public List<RotationAnalysis> getAnalysisByDateRange(
            LocalDateTime startDate, LocalDateTime endDate, String periodType) {
        return analysisMapper.selectByDateRange(startDate, endDate, periodType);
    }

    // ============================================================

    // 宸ュ叿鏂规硶
    // ============================================================

    private String generateAlertNo() {
        return "EA"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
