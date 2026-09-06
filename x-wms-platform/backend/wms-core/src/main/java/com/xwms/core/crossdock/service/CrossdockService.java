package com.xwms.core.crossdock.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.crossdock.entity.*;
import com.xwms.core.crossdock.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 越库管理核心服务 核心能力: 越库创建/匹配/收货/分拣/发运 越库特点: 货物从入库月台直接转到出库月台，不经过存储环节 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrossdockService {

    private final CrossdockMapper crossdockMapper;
    private final CrossdockDetailMapper detailMapper;
    private final CrossdockMatchMapper matchMapper;
    private final CrossdockTaskMapper taskMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 越库单创建
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Crossdock createCrossdock(
            Crossdock crossdock, List<CrossdockDetail> details, String operator) {
        if (crossdock.getCrossdockNo() == null) {
            crossdock.setCrossdockNo(generateCrossdockNo());
        }
        if (crossdock.getStatus() == null) crossdock.setStatus("CREATED");
        if (crossdock.getTotalQty() == null) crossdock.setTotalQty(BigDecimal.ZERO);
        if (crossdock.getReceivedQty() == null) crossdock.setReceivedQty(BigDecimal.ZERO);
        if (crossdock.getSortedQty() == null) crossdock.setSortedQty(BigDecimal.ZERO);
        if (crossdock.getShippedQty() == null) crossdock.setShippedQty(BigDecimal.ZERO);
        crossdock.setCreatedBy(operator);
        crossdockMapper.insert(crossdock);

        // 保存明细
        if (details != null) {
            for (int i = 0; i < details.size(); i++) {
                CrossdockDetail detail = details.get(i);
                detail.setCrossdockNo(crossdock.getCrossdockNo());
                detail.setLineNo(i + 1);
                if (detail.getExpectedQty() == null) detail.setExpectedQty(BigDecimal.ZERO);
                if (detail.getReceivedQty() == null) detail.setReceivedQty(BigDecimal.ZERO);
                if (detail.getSortedQty() == null) detail.setSortedQty(BigDecimal.ZERO);
                if (detail.getShippedQty() == null) detail.setShippedQty(BigDecimal.ZERO);
                if (detail.getStatus() == null) detail.setStatus("PENDING");
                detailMapper.insert(detail);
            }
            crossdock.setTotalQty(
                    details.stream()
                            .map(CrossdockDetail::getExpectedQty)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
            crossdockMapper.updateById(crossdock);
        }

        log.info(
                "创建越库单: {}={}, 明细{}行",
                crossdock.getCrossdockNo(),
                crossdock.getCrossdockType(),
                details != null ? details.size() : 0);
        return crossdock;
    }

    // ============================================================

    // 2. 越库匹配（入库明细与出库明细匹配）
    // ============================================================

    /** 自动匹配入库与出库 匹配规则: SKU相同 + 数量匹配 + 批次匹配(可选) */
    @Transactional(rollbackFor = Exception.class)
    public List<CrossdockMatch> autoMatch(String crossdockNo) {
        Crossdock crossdock = crossdockMapper.selectByCrossdockNo(crossdockNo);
        if (crossdock == null) throw new RuntimeException("越库单不存在: " + crossdockNo);

        List<CrossdockDetail> details = detailMapper.selectByCrossdockNo(crossdockNo);
        List<CrossdockMatch> matches = new ArrayList<>();

        // TODO: 实际需要从入库单和出库单获取明细进行匹配
        // 这里简化处理，按SKU分组匹配
        Map<String, List<CrossdockDetail>> groupedBySku =
                details.stream().collect(Collectors.groupingBy(CrossdockDetail::getSkuCode));

        int lineNo = 1;
        for (Map.Entry<String, List<CrossdockDetail>> entry : groupedBySku.entrySet()) {
            String skuCode = entry.getKey();
            List<CrossdockDetail> skuDetails = entry.getValue();
            BigDecimal totalQty =
                    skuDetails.stream()
                            .map(CrossdockDetail::getExpectedQty)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            CrossdockMatch match = new CrossdockMatch();
            match.setMatchNo(generateMatchNo());
            match.setCrossdockNo(crossdockNo);
            match.setInboundLineNo(lineNo);
            match.setOutboundLineNo(lineNo);
            match.setSkuCode(skuCode);
            match.setMatchQty(totalQty);
            match.setMatchType("EXACT");
            match.setStatus("MATCHED");
            matchMapper.insert(match);
            matches.add(match);
            lineNo++;
        }

        crossdock.setStatus("MATCHED");
        crossdockMapper.updateById(crossdock);

        log.info("越库自动匹配: {}, 匹配{}条", crossdockNo, matches.size());
        return matches;
    }

    /** 手动匹配 */
    @Transactional(rollbackFor = Exception.class)
    public CrossdockMatch manualMatch(
            String crossdockNo,
            Integer inboundLineNo,
            Integer outboundLineNo,
            String skuCode,
            String batchNo,
            BigDecimal matchQty,
            String matchType) {
        CrossdockMatch match = new CrossdockMatch();
        match.setMatchNo(generateMatchNo());
        match.setCrossdockNo(crossdockNo);
        match.setInboundLineNo(inboundLineNo);
        match.setOutboundLineNo(outboundLineNo);
        match.setSkuCode(skuCode);
        match.setBatchNo(batchNo);
        match.setMatchQty(matchQty);
        match.setMatchType(matchType);
        match.setStatus("MATCHED");
        matchMapper.insert(match);

        log.info(
                "越库手动匹配: {}, inbound={}, outbound={}, qty={}",
                crossdockNo,
                inboundLineNo,
                outboundLineNo,
                matchQty);
        return match;
    }

    // ============================================================

    // 3. 越库收货（入库月台收货，直接转到暂存区）
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public CrossdockTask receive(
            String crossdockNo,
            String skuCode,
            String batchNo,
            String fromLocation,
            BigDecimal receivedQty,
            String operator) {
        Crossdock crossdock = crossdockMapper.selectByCrossdockNo(crossdockNo);
        if (crossdock == null) throw new RuntimeException("越库单不存在: " + crossdockNo);

        if ("CREATED".equals(crossdock.getStatus()) || "MATCHED".equals(crossdock.getStatus())) {
            crossdock.setStatus("RECEIVING");
            crossdock.setStartTime(LocalDateTime.now());
            crossdockMapper.updateById(crossdock);
        }

        // 更新越库单收货数量
        crossdock.setReceivedQty(crossdock.getReceivedQty().add(receivedQty));
        crossdockMapper.updateById(crossdock);

        // 更新明细收货数量
        List<CrossdockDetail> details = detailMapper.selectByCrossdockNo(crossdockNo);
        for (CrossdockDetail detail : details) {
            if (skuCode.equals(detail.getSkuCode())) {
                detail.setReceivedQty(detail.getReceivedQty().add(receivedQty));
                if (detail.getReceivedQty().compareTo(detail.getExpectedQty()) >= 0) {
                    detail.setStatus("RECEIVED");
                }
                detailMapper.updateById(detail);
                break;
            }
        }

        // 创建收货作业记录
        CrossdockTask task = new CrossdockTask();
        task.setTaskNo(generateTaskNo());
        task.setCrossdockNo(crossdockNo);
        task.setTaskType("RECEIVE");
        task.setSkuCode(skuCode);
        task.setBatchNo(batchNo);
        task.setFromLocation(fromLocation);
        task.setToLocation(crossdock.getInboundDock());
        task.setTaskQty(receivedQty);
        task.setDoneQty(receivedQty);
        task.setOperator(operator);
        task.setStatus("DONE");
        task.setStartTime(LocalDateTime.now());
        task.setEndTime(LocalDateTime.now());
        taskMapper.insert(task);

        // 检查收货是否完成
        checkReceiveComplete(crossdockNo);

        log.info("越库收货: {}, sku={}, qty={}", crossdockNo, skuCode, receivedQty);
        return task;
    }

    /** 检查收货是否完成 */
    private void checkReceiveComplete(String crossdockNo) {
        Crossdock crossdock = crossdockMapper.selectByCrossdockNo(crossdockNo);
        List<CrossdockDetail> details = detailMapper.selectByCrossdockNo(crossdockNo);
        boolean allReceived = details.stream().allMatch(d -> "RECEIVED".equals(d.getStatus()));
        if (allReceived) {
            crossdock.setStatus("RECEIVED");
            crossdockMapper.updateById(crossdock);
            log.info("越库收货完成: {}", crossdockNo);
        }
    }

    // ============================================================

    // 4. 越库分拣（从暂存区分拣到出库月台）
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public CrossdockTask sort(
            String crossdockNo,
            String skuCode,
            String batchNo,
            String fromLocation,
            String toLocation,
            BigDecimal sortQty,
            String operator) {
        Crossdock crossdock = crossdockMapper.selectByCrossdockNo(crossdockNo);
        if (crossdock == null) throw new RuntimeException("越库单不存在: " + crossdockNo);

        if ("RECEIVED".equals(crossdock.getStatus())) {
            crossdock.setStatus("SORTING");
            crossdockMapper.updateById(crossdock);
        }

        // 更新越库单分拣数量
        crossdock.setSortedQty(crossdock.getSortedQty().add(sortQty));
        crossdockMapper.updateById(crossdock);

        // 更新明细分拣数量
        List<CrossdockDetail> details = detailMapper.selectByCrossdockNo(crossdockNo);
        for (CrossdockDetail detail : details) {
            if (skuCode.equals(detail.getSkuCode())) {
                detail.setSortedQty(detail.getSortedQty().add(sortQty));
                if (detail.getSortedQty().compareTo(detail.getReceivedQty()) >= 0) {
                    detail.setStatus("SORTED");
                }
                detailMapper.updateById(detail);
                break;
            }
        }

        // 创建分拣作业记录
        CrossdockTask task = new CrossdockTask();
        task.setTaskNo(generateTaskNo());
        task.setCrossdockNo(crossdockNo);
        task.setTaskType("SORT");
        task.setSkuCode(skuCode);
        task.setBatchNo(batchNo);
        task.setFromLocation(fromLocation);
        task.setToLocation(toLocation);
        task.setTaskQty(sortQty);
        task.setDoneQty(sortQty);
        task.setOperator(operator);
        task.setStatus("DONE");
        task.setStartTime(LocalDateTime.now());
        task.setEndTime(LocalDateTime.now());
        taskMapper.insert(task);

        // 检查分拣是否完成
        checkSortComplete(crossdockNo);

        log.info(
                "越库分拣: {}, sku={}, qty={}, {}->{}",
                crossdockNo,
                skuCode,
                sortQty,
                fromLocation,
                toLocation);
        return task;
    }

    /** 检查分拣是否完成 */
    private void checkSortComplete(String crossdockNo) {
        Crossdock crossdock = crossdockMapper.selectByCrossdockNo(crossdockNo);
        List<CrossdockDetail> details = detailMapper.selectByCrossdockNo(crossdockNo);
        boolean allSorted = details.stream().allMatch(d -> "SORTED".equals(d.getStatus()));
        if (allSorted) {
            crossdock.setStatus("SORTED");
            crossdockMapper.updateById(crossdock);
            log.info("越库分拣完成: {}", crossdockNo);
        }
    }

    // ============================================================

    // 5. 越库发运（从出库月台装车发运）
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public CrossdockTask ship(
            String crossdockNo,
            String skuCode,
            String batchNo,
            String fromLocation,
            BigDecimal shipQty,
            String operator) {
        Crossdock crossdock = crossdockMapper.selectByCrossdockNo(crossdockNo);
        if (crossdock == null) throw new RuntimeException("越库单不存在: " + crossdockNo);

        if ("SORTED".equals(crossdock.getStatus())) {
            crossdock.setStatus("SHIPPING");
            crossdockMapper.updateById(crossdock);
        }

        // 更新越库单发运数量
        crossdock.setShippedQty(crossdock.getShippedQty().add(shipQty));
        crossdockMapper.updateById(crossdock);

        // 更新明细发运数量
        List<CrossdockDetail> details = detailMapper.selectByCrossdockNo(crossdockNo);
        for (CrossdockDetail detail : details) {
            if (skuCode.equals(detail.getSkuCode())) {
                detail.setShippedQty(detail.getShippedQty().add(shipQty));
                if (detail.getShippedQty().compareTo(detail.getSortedQty()) >= 0) {
                    detail.setStatus("SHIPPED");
                }
                detailMapper.updateById(detail);
                break;
            }
        }

        // 创建发运作业记录
        CrossdockTask task = new CrossdockTask();
        task.setTaskNo(generateTaskNo());
        task.setCrossdockNo(crossdockNo);
        task.setTaskType("SHIP");
        task.setSkuCode(skuCode);
        task.setBatchNo(batchNo);
        task.setFromLocation(fromLocation);
        task.setToLocation(crossdock.getOutboundDock());
        task.setTaskQty(shipQty);
        task.setDoneQty(shipQty);
        task.setOperator(operator);
        task.setStatus("DONE");
        task.setStartTime(LocalDateTime.now());
        task.setEndTime(LocalDateTime.now());
        taskMapper.insert(task);

        // 检查发运是否完成
        checkShipComplete(crossdockNo);

        log.info("越库发运: {}, sku={}, qty={}", crossdockNo, skuCode, shipQty);
        return task;
    }

    /** 检查发运是否完成 */
    private void checkShipComplete(String crossdockNo) {
        Crossdock crossdock = crossdockMapper.selectByCrossdockNo(crossdockNo);
        List<CrossdockDetail> details = detailMapper.selectByCrossdockNo(crossdockNo);
        boolean allShipped = details.stream().allMatch(d -> "SHIPPED".equals(d.getStatus()));
        if (allShipped) {
            crossdock.setStatus("SHIPPED");
            crossdock.setEndTime(LocalDateTime.now());
            crossdockMapper.updateById(crossdock);
            log.info("越库发运完成: {}", crossdockNo);
        }
    }

    // ============================================================

    // 6. 查询
    // ============================================================

    public Page<Crossdock> pageCrossdocks(
            Page<Crossdock> page,
            String crossdockType,
            String status,
            String warehouseCode,
            String ownerCode) {
        LambdaQueryWrapper<Crossdock> wrapper = new LambdaQueryWrapper<>();
        if (crossdockType != null) wrapper.eq(Crossdock::getCrossdockType, crossdockType);
        if (status != null) wrapper.eq(Crossdock::getStatus, status);
        if (warehouseCode != null) wrapper.eq(Crossdock::getWarehouseCode, warehouseCode);
        if (ownerCode != null) wrapper.eq(Crossdock::getOwnerCodeCol, ownerCode);
        wrapper.orderByDesc(Crossdock::getCreatedTime);
        return crossdockMapper.selectPage(page, wrapper);
    }

    public Crossdock getCrossdockByNo(String crossdockNo) {
        return crossdockMapper.selectByCrossdockNo(crossdockNo);
    }

    public List<CrossdockDetail> getDetails(String crossdockNo) {
        return detailMapper.selectByCrossdockNo(crossdockNo);
    }

    public List<CrossdockMatch> getMatches(String crossdockNo) {
        return matchMapper.selectByCrossdockNo(crossdockNo);
    }

    public List<CrossdockTask> getTasks(String crossdockNo) {
        return taskMapper.selectByCrossdockNo(crossdockNo);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateCrossdockNo() {
        return "CD"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateMatchNo() {
        return "CDM"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateTaskNo() {
        return "CDT"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
