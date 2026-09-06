package com.xwms.core.stockdiff.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.stockdiff.entity.*;
import com.xwms.core.stockdiff.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库存盘点差异管理核心服务 核心能力: 差异记录/差异处理/差异审批/差异分析 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockDiffService {

    private final StockDiffMapper diffMapper;
    private final StockDiffHandleMapper handleMapper;
    private final StockDiffApproveMapper approveMapper;
    private final StockDiffAnalysisMapper analysisMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 差异记录管理
    // ============================================================

    /** 创建差异记录 */
    @Transactional(rollbackFor = Exception.class)
    public StockDiff createDiff(StockDiff diff) {
        diff.setDiffId(generateDiffId());
        diff.setStatus("PENDING");
        if (diff.getAdjustFlag() == null) diff.setAdjustFlag("N");

        // 计算差异数量和差异率
        if (diff.getSystemQty() != null && diff.getCountedQty() != null) {
            diff.setDiffQty(diff.getCountedQty().subtract(diff.getSystemQty()));
            if (diff.getSystemQty().compareTo(BigDecimal.ZERO) != 0) {
                diff.setDiffRate(
                        diff.getDiffQty()
                                .abs()
                                .divide(diff.getSystemQty(), 4, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100")));
            }
        }

        // 自动判断差异类型
        if (diff.getDiffType() == null) {
            if (diff.getDiffQty().compareTo(BigDecimal.ZERO) < 0) {
                diff.setDiffType("SHORTAGE");
            } else if (diff.getDiffQty().compareTo(BigDecimal.ZERO) > 0) {
                diff.setDiffType("OVERAGE");
            } else {
                diff.setDiffType("OTHER");
            }
        }

        // 自动判断差异级别
        if (diff.getDiffLevel() == null) {
            diff.setDiffLevel(judgeDiffLevel(diff.getDiffRate(), diff.getDiffQty()));
        }

        diffMapper.insert(diff);
        log.info(
                "创建差异记录: diffId={}, stocktake={}, sku={}, system={}, counted={}, diff={}",
                diff.getDiffId(),
                diff.getStocktakeNo(),
                diff.getSkuCode(),
                diff.getSystemQty(),
                diff.getCountedQty(),
                diff.getDiffQty());
        return diff;
    }

    /** 判断差异级别 */
    private String judgeDiffLevel(BigDecimal diffRate, BigDecimal diffQty) {
        if (diffRate == null) return "MINOR";
        // 差异率 > 20% 或 差异数量 > 100 为严重
        if (diffRate.compareTo(new BigDecimal("20")) > 0
                || diffQty.abs().compareTo(new BigDecimal("100")) > 0) {
            return "CRITICAL";
        }
        // 差异率 > 10% 为重大
        if (diffRate.compareTo(new BigDecimal("10")) > 0) {
            return "MAJOR";
        }
        // 差异率 > 5% 为正常
        if (diffRate.compareTo(new BigDecimal("5")) > 0) {
            return "NORMAL";
        }
        return "MINOR";
    }

    public StockDiff getDiffById(String diffId) {
        return diffMapper.selectByDiffId(diffId);
    }

    public List<StockDiff> getDiffsByStocktake(String stocktakeNo) {
        return diffMapper.selectByStocktakeNo(stocktakeNo);
    }

    public List<StockDiff> getDiffsByStatus(String status) {
        return diffMapper.selectByStatus(status);
    }

    public List<StockDiff> getRecentDiffsBySku(String skuCode, int limit) {
        return diffMapper.selectRecentBySku(skuCode, limit);
    }

    public List<StockDiff> getActiveDiffsByLocation(String locationCode) {
        return diffMapper.selectActiveByLocation(locationCode);
    }

    public int countActiveDiffs() {
        return diffMapper.countActive();
    }

    public int countActiveDiffsByType(String diffType) {
        return diffMapper.countActiveByType(diffType);
    }

    public Page<StockDiff> pageDiffs(
            Page<StockDiff> page,
            String stocktakeNo,
            String diffType,
            String status,
            String warehouseCode,
            String skuCode) {
        LambdaQueryWrapper<StockDiff> wrapper = new LambdaQueryWrapper<>();
        if (stocktakeNo != null) wrapper.eq(StockDiff::getStocktakeNo, stocktakeNo);
        if (diffType != null) wrapper.eq(StockDiff::getDiffType, diffType);
        if (status != null) wrapper.eq(StockDiff::getStatus, status);
        if (warehouseCode != null) wrapper.eq(StockDiff::getWarehouseCode, warehouseCode);
        if (skuCode != null) wrapper.eq(StockDiff::getSkuCode, skuCode);
        wrapper.orderByDesc(StockDiff::getCreatedTime);
        return diffMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. 差异处理
    // ============================================================

    /** 处理差异（调整库存） */
    @Transactional(rollbackFor = Exception.class)
    public StockDiff handleDiff(
            String diffId,
            String handleType,
            String handleAction,
            String handleNote,
            String adjustNo,
            BigDecimal adjustQty,
            String operator) {
        StockDiff diff = diffMapper.selectByDiffId(diffId);
        if (diff == null) throw new RuntimeException("差异记录不存在: " + diffId);
        if (!"PENDING".equals(diff.getStatus()) && !"PROCESSING".equals(diff.getStatus())) {
            throw new RuntimeException("差异状态不正确: " + diff.getStatus());
        }

        String beforeStatus = diff.getStatus();
        String afterStatus = "RESOLVED";

        // 更新差异状态
        if ("ADJUST".equals(handleType)) {
            diffMapper.resolveWithAdjust(diffId, adjustNo, operator);
        } else {
            diffMapper.updateStatus(diffId, afterStatus, operator);
        }

        // 记录处理日志
        StockDiffHandle handle = new StockDiffHandle();
        handle.setHandleId(generateHandleId());
        handle.setDiffId(diffId);
        handle.setHandleType(handleType);
        handle.setHandleAction(handleAction);
        handle.setHandleNote(handleNote);
        handle.setBeforeStatus(beforeStatus);
        handle.setAfterStatus(afterStatus);
        handle.setAdjustNo(adjustNo);
        handle.setAdjustQty(adjustQty);
        handle.setOperator(operator);
        handle.setHandleTime(LocalDateTime.now());
        handleMapper.insert(handle);

        log.info(
                "处理差异: diffId={}, type={}, action={}, operator={}",
                diffId,
                handleType,
                handleAction,
                operator);
        return diffMapper.selectByDiffId(diffId);
    }

    /** 复盘差异 */
    @Transactional(rollbackFor = Exception.class)
    public StockDiff recountDiff(String diffId, BigDecimal newCountedQty, String operator) {
        StockDiff diff = diffMapper.selectByDiffId(diffId);
        if (diff == null) throw new RuntimeException("差异记录不存在: " + diffId);

        String beforeStatus = diff.getStatus();

        // 更新实盘数量和差异
        diff.setCountedQty(newCountedQty);
        diff.setDiffQty(newCountedQty.subtract(diff.getSystemQty()));
        if (diff.getSystemQty().compareTo(BigDecimal.ZERO) != 0) {
            diff.setDiffRate(
                    diff.getDiffQty()
                            .abs()
                            .divide(diff.getSystemQty(), 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100")));
        }
        diff.setDiffLevel(judgeDiffLevel(diff.getDiffRate(), diff.getDiffQty()));
        diff.setStatus("PENDING");
        diffMapper.updateById(diff);

        // 记录处理日志
        StockDiffHandle handle = new StockDiffHandle();
        handle.setHandleId(generateHandleId());
        handle.setDiffId(diffId);
        handle.setHandleType("RECOUNT");
        handle.setHandleAction("RECOUNT");
        handle.setHandleNote("复盘, 新实盘数量: " + newCountedQty);
        handle.setBeforeStatus(beforeStatus);
        handle.setAfterStatus("PENDING");
        handle.setOperator(operator);
        handle.setHandleTime(LocalDateTime.now());
        handleMapper.insert(handle);

        log.info("复盘差异: diffId={}, newCounted={}", diffId, newCountedQty);
        return diffMapper.selectByDiffId(diffId);
    }

    /** 取消差异 */
    @Transactional(rollbackFor = Exception.class)
    public StockDiff cancelDiff(String diffId, String reason, String operator) {
        StockDiff diff = diffMapper.selectByDiffId(diffId);
        if (diff == null) throw new RuntimeException("差异记录不存在: " + diffId);

        String beforeStatus = diff.getStatus();
        diffMapper.updateStatus(diffId, "CANCELLED", operator);

        StockDiffHandle handle = new StockDiffHandle();
        handle.setHandleId(generateHandleId());
        handle.setDiffId(diffId);
        handle.setHandleType("OTHER");
        handle.setHandleAction("CANCEL");
        handle.setHandleNote(reason);
        handle.setBeforeStatus(beforeStatus);
        handle.setAfterStatus("CANCELLED");
        handle.setOperator(operator);
        handle.setHandleTime(LocalDateTime.now());
        handleMapper.insert(handle);

        log.info("取消差异: diffId={}, reason={}", diffId, reason);
        return diffMapper.selectByDiffId(diffId);
    }

    public List<StockDiffHandle> getHandlesByDiff(String diffId) {
        return handleMapper.selectByDiffId(diffId);
    }

    // ============================================================

    // 3. 差异审批
    // ============================================================

    /** 提交审批 */
    @Transactional(rollbackFor = Exception.class)
    public StockDiff submitApprove(String diffId, String operator) {
        StockDiff diff = diffMapper.selectByDiffId(diffId);
        if (diff == null) throw new RuntimeException("差异记录不存在: " + diffId);
        if (!"PENDING".equals(diff.getStatus()) && !"PROCESSING".equals(diff.getStatus())) {
            throw new RuntimeException("差异状态不正确: " + diff.getStatus());
        }

        diffMapper.updateStatus(diffId, "APPROVING", operator);
        log.info("提交审批: diffId={}, operator={}", diffId, operator);
        return diffMapper.selectByDiffId(diffId);
    }

    /** 审批差异 */
    @Transactional(rollbackFor = Exception.class)
    public StockDiff approveDiff(
            String diffId,
            String approveNode,
            String approveRole,
            String approver,
            String approveResult,
            String approveNote) {
        StockDiff diff = diffMapper.selectByDiffId(diffId);
        if (diff == null) throw new RuntimeException("差异记录不存在: " + diffId);
        if (!"APPROVING".equals(diff.getStatus())) {
            throw new RuntimeException("差异状态不正确: " + diff.getStatus());
        }

        // 记录审批日志
        StockDiffApprove approve = new StockDiffApprove();
        approve.setApproveId(generateApproveId());
        approve.setDiffId(diffId);
        approve.setApproveNode(approveNode);
        approve.setApproveRole(approveRole);
        approve.setApprover(approver);
        approve.setApproveResult(approveResult);
        approve.setApproveNote(approveNote);
        approve.setApproveTime(LocalDateTime.now());
        approveMapper.insert(approve);

        // 审批通过则回到处理中，驳回则回到待处理
        String newStatus = "APPROVED".equals(approveResult) ? "PROCESSING" : "PENDING";
        diffMapper.updateStatus(diffId, newStatus, approver);

        log.info("审批差异: diffId={}, result={}, approver={}", diffId, approveResult, approver);
        return diffMapper.selectByDiffId(diffId);
    }

    public List<StockDiffApprove> getApprovesByDiff(String diffId) {
        return approveMapper.selectByDiffId(diffId);
    }

    // ============================================================

    // 4. 差异分析
    // ============================================================

    /** 生成差异分析 */
    @Transactional(rollbackFor = Exception.class)
    public StockDiffAnalysis generateAnalysis(
            LocalDate analysisDate,
            String analysisType,
            String warehouseCode,
            List<StockDiff> diffs) {
        StockDiffAnalysis analysis = new StockDiffAnalysis();
        analysis.setAnalysisId(generateAnalysisId());
        analysis.setAnalysisDate(analysisDate);
        analysis.setAnalysisType(analysisType);
        analysis.setWarehouseCode(warehouseCode);

        int totalCount = diffs.size();
        int diffCount = 0;
        int shortageCount = 0;
        int overageCount = 0;
        int majorCount = 0;
        int criticalCount = 0;
        BigDecimal totalSystemQty = BigDecimal.ZERO;
        BigDecimal totalCountedQty = BigDecimal.ZERO;
        BigDecimal totalDiffQty = BigDecimal.ZERO;

        for (StockDiff diff : diffs) {
            totalSystemQty =
                    totalSystemQty.add(
                            diff.getSystemQty() != null ? diff.getSystemQty() : BigDecimal.ZERO);
            totalCountedQty =
                    totalCountedQty.add(
                            diff.getCountedQty() != null ? diff.getCountedQty() : BigDecimal.ZERO);
            totalDiffQty =
                    totalDiffQty.add(
                            diff.getDiffQty() != null ? diff.getDiffQty() : BigDecimal.ZERO);

            if (diff.getDiffQty() != null && diff.getDiffQty().compareTo(BigDecimal.ZERO) != 0) {
                diffCount++;
                if ("SHORTAGE".equals(diff.getDiffType())) shortageCount++;
                if ("OVERAGE".equals(diff.getDiffType())) overageCount++;
                if ("MAJOR".equals(diff.getDiffLevel())) majorCount++;
                if ("CRITICAL".equals(diff.getDiffLevel())) criticalCount++;
            }
        }

        analysis.setTotalCount(totalCount);
        analysis.setDiffCount(diffCount);
        analysis.setShortageCount(shortageCount);
        analysis.setOverageCount(overageCount);
        analysis.setTotalSystemQty(totalSystemQty);
        analysis.setTotalCountedQty(totalCountedQty);
        analysis.setTotalDiffQty(totalDiffQty);
        analysis.setMajorDiffCount(majorCount);
        analysis.setCriticalDiffCount(criticalCount);

        // 计算准确率和差异率
        if (totalCount > 0) {
            analysis.setAccuracyRate(
                    new BigDecimal(totalCount - diffCount)
                            .divide(new BigDecimal(totalCount), 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100")));
        }
        if (totalSystemQty.compareTo(BigDecimal.ZERO) != 0) {
            analysis.setDiffRate(
                    totalDiffQty
                            .abs()
                            .divide(totalSystemQty, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100")));
        }

        analysis.setStatus("ACTIVE");
        analysisMapper.insert(analysis);

        log.info(
                "生成差异分析: analysisId={}, date={}, type={}, total={}, diff={}, accuracy={}",
                analysis.getAnalysisId(),
                analysisDate,
                analysisType,
                totalCount,
                diffCount,
                analysis.getAccuracyRate());
        return analysis;
    }

    public StockDiffAnalysis getAnalysisById(String analysisId) {
        return analysisMapper.selectByAnalysisId(analysisId);
    }

    public List<StockDiffAnalysis> getAnalysisByDateAndType(
            LocalDate analysisDate, String analysisType) {
        return analysisMapper.selectByDateAndType(analysisDate, analysisType);
    }

    public List<StockDiffAnalysis> getAnalysisByWarehouseAndDateRange(
            String warehouseCode, LocalDate startDate, LocalDate endDate) {
        return analysisMapper.selectByWarehouseAndDateRange(warehouseCode, startDate, endDate);
    }

    public Page<StockDiffAnalysis> pageAnalysis(
            Page<StockDiffAnalysis> page, String analysisType, String warehouseCode) {
        LambdaQueryWrapper<StockDiffAnalysis> wrapper = new LambdaQueryWrapper<>();
        if (analysisType != null) wrapper.eq(StockDiffAnalysis::getAnalysisType, analysisType);
        if (warehouseCode != null) wrapper.eq(StockDiffAnalysis::getWarehouseCode, warehouseCode);
        wrapper.orderByDesc(StockDiffAnalysis::getAnalysisDate);
        return analysisMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 5. 差异统计
    // ============================================================

    public Map<String, Object> getDiffStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalActive", diffMapper.countActive());
        stats.put("shortageActive", diffMapper.countActiveByType("SHORTAGE"));
        stats.put("overageActive", diffMapper.countActiveByType("OVERAGE"));
        stats.put("damageActive", diffMapper.countActiveByType("DAMAGE"));
        return stats;
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateDiffId() {
        return "DF"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateHandleId() {
        return "DH"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateApproveId() {
        return "DA"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateAnalysisId() {
        return "DAN"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
