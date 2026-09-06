package com.xwms.core.crossdock.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xwms.core.crossdock.entity.*;
import com.xwms.core.crossdock.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 越库流程深化服务 核心能力: 越库预配/越库预分/与出库联动/库存分配 越库特点: 货物从入库月台直接转到出库月台，不经过存储环节 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrossdockProcessService {

    private final CrossdockMapper crossdockMapper;
    private final CrossdockDetailMapper detailMapper;
    private final CrossdockMatchMapper matchMapper;
    private final CrossdockTaskMapper taskMapper;
    private final CrossdockPreAllocationMapper preAllocMapper;
    private final CrossdockPreSortMapper preSortMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 越库预配（ASN到货前，根据越库规则为等待的SO预先匹配）
    // ============================================================

    /** 执行越库预配 ASN表头→右键→CrossDock预配，系统根据越库规则配置为等待的SO进行预先匹配 */
    @Transactional(rollbackFor = Exception.class)
    public List<CrossdockPreAllocation> executePreAllocation(
            String crossdockNo, String asnNo, String operator) {
        log.info("执行越库预配: crossdockNo={}, asnNo={}", crossdockNo, asnNo);

        Crossdock crossdock = crossdockMapper.selectByCrossdockNo(crossdockNo);
        if (crossdock == null) {
            throw new RuntimeException("越库单不存在: " + crossdockNo);
        }

        // 获取入库明细
        List<CrossdockDetail> inboundDetails = detailMapper.selectByCrossdockNo(crossdockNo);

        // TODO: 实际需要从出库模块获取等待的SO明细
        // 这里模拟等待的出库SO明细
        List<Map<String, Object>> waitingOutbounds = simulateWaitingOutbounds(inboundDetails);

        // 按越库规则匹配（SKU相同 + 数量匹配 + 优先级排序）
        List<CrossdockPreAllocation> allocations = new ArrayList<>();
        Map<String, BigDecimal> inboundRemaining = new HashMap<>();
        for (CrossdockDetail detail : inboundDetails) {
            inboundRemaining.put(detail.getSkuCode(), detail.getExpectedQty());
        }

        int priority = 1;
        for (Map<String, Object> outbound : waitingOutbounds) {
            String skuCode = (String) outbound.get("skuCode");
            BigDecimal needQty = (BigDecimal) outbound.get("qty");
            String outboundNo = (String) outbound.get("outboundNo");

            BigDecimal remaining = inboundRemaining.getOrDefault(skuCode, BigDecimal.ZERO);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal allocQty = needQty.min(remaining);
            if (allocQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // 创建预配记录
            CrossdockPreAllocation alloc = new CrossdockPreAllocation();
            alloc.setAllocNo(generateAllocNo());
            alloc.setCrossdockNo(crossdockNo);
            alloc.setAsnNo(asnNo);
            alloc.setOutboundNo(outboundNo);
            alloc.setSkuCode(skuCode);
            alloc.setSkuName((String) outbound.get("skuName"));
            alloc.setAllocQty(allocQty);
            alloc.setReceivedQty(BigDecimal.ZERO);
            alloc.setAllocatedQty(BigDecimal.ZERO);
            alloc.setMatchType("EXACT");
            alloc.setStatus("ALLOCATED");
            alloc.setPriority(priority++);
            alloc.setCreatedBy(operator);
            alloc.setCreatedTime(LocalDateTime.now());
            preAllocMapper.insert(alloc);
            allocations.add(alloc);

            // 扣减剩余数量
            inboundRemaining.put(skuCode, remaining.subtract(allocQty));
        }

        // 更新越库单状态
        crossdock.setStatus("PRE_ALLOCATED");
        crossdockMapper.updateById(crossdock);

        log.info("越库预配完成: crossdockNo={}, 预配记录数={}", crossdockNo, allocations.size());
        return allocations;
    }

    /** 模拟等待的出库SO（实际应从出库模块获取） */
    private List<Map<String, Object>> simulateWaitingOutbounds(
            List<CrossdockDetail> inboundDetails) {
        List<Map<String, Object>> outbounds = new ArrayList<>();
        for (CrossdockDetail detail : inboundDetails) {
            Map<String, Object> outbound = new HashMap<>();
            outbound.put(
                    "outboundNo", "SO" + System.currentTimeMillis() + (int) (Math.random() * 1000));
            outbound.put("skuCode", detail.getSkuCode());
            outbound.put("skuName", detail.getSkuCode());
            outbound.put("qty", detail.getExpectedQty());
            outbounds.add(outbound);
        }
        return outbounds;
    }

    // ============================================================

    // 2. 越库预分（RF扫描箱码，系统按箱号执行收货确认，后台分配库存，提取播种位）
    // ============================================================

    /** RF越库预分扫描 扫描箱码/箱序列号，系统按箱号执行收货确认，后台对已收货库存进行分配，提取订单播种位显示 */
    @Transactional(rollbackFor = Exception.class)
    public CrossdockPreSort executePreSortScan(
            String crossdockNo,
            String boxNo,
            String boxSerialNo,
            String skuCode,
            BigDecimal boxQty,
            String scanner) {
        log.info(
                "越库预分扫描: crossdockNo={}, boxNo={}, sku={}, qty={}",
                crossdockNo,
                boxNo,
                skuCode,
                boxQty);

        Crossdock crossdock = crossdockMapper.selectByCrossdockNo(crossdockNo);
        if (crossdock == null) {
            throw new RuntimeException("越库单不存在: " + crossdockNo);
        }

        // 检查箱号是否已扫描
        CrossdockPreSort existing = preSortMapper.selectByBoxNo(boxNo);
        if (existing != null && "SCANNED".equals(existing.getStatus())) {
            throw new RuntimeException("箱号已扫描: " + boxNo);
        }

        // 查找对应的预配记录
        List<CrossdockPreAllocation> allocs = preAllocMapper.selectByCrossdockNo(crossdockNo);
        CrossdockPreAllocation matchedAlloc =
                allocs.stream()
                        .filter(
                                a ->
                                        skuCode.equals(a.getSkuCode())
                                                && a.getAllocQty().compareTo(a.getAllocatedQty())
                                                        > 0)
                        .findFirst()
                        .orElse(null);

        // 判断是否需要越库
        String xdockType = matchedAlloc != null ? "XDOCK" : "NONE-XDOCK";
        String outboundNo = matchedAlloc != null ? matchedAlloc.getOutboundNo() : null;
        String sowingLocation = matchedAlloc != null ? generateSowingLocation() : null;
        String outboundDock = matchedAlloc != null ? crossdock.getOutboundDock() : null;

        // 创建预分记录
        CrossdockPreSort preSort = new CrossdockPreSort();
        preSort.setSortNo(generateSortNo());
        preSort.setCrossdockNo(crossdockNo);
        preSort.setAllocNo(matchedAlloc != null ? matchedAlloc.getAllocNo() : null);
        preSort.setAsnNo(crossdock.getInboundNo());
        preSort.setBoxNo(boxNo);
        preSort.setBoxSerialNo(boxSerialNo);
        preSort.setSkuCode(skuCode);
        preSort.setBoxQty(boxQty);
        preSort.setReceivedQty(BigDecimal.ZERO);
        preSort.setSortedQty(BigDecimal.ZERO);
        preSort.setOutboundNo(outboundNo);
        preSort.setSowingLocation(sowingLocation);
        preSort.setOutboundDock(outboundDock);
        preSort.setXdockType(xdockType);
        preSort.setStatus("SCANNED");
        preSort.setScanner(scanner);
        preSort.setScanTime(LocalDateTime.now());
        preSort.setCreatedBy(scanner);
        preSort.setCreatedTime(LocalDateTime.now());
        preSortMapper.insert(preSort);

        log.info(
                "越库预分扫描完成: sortNo={}, xdockType={}, 播种位={}",
                preSort.getSortNo(),
                xdockType,
                sowingLocation);
        return preSort;
    }

    /** 越库预分收货确认 扫描确认后，系统按箱号执行收货确认，后台对已收货库存进行分配 */
    @Transactional(rollbackFor = Exception.class)
    public CrossdockPreSort confirmPreSortReceive(String sortNo, String operator) {
        log.info("越库预分收货确认: sortNo={}", sortNo);

        CrossdockPreSort preSort = preSortMapper.selectBySortNo(sortNo);
        if (preSort == null) {
            throw new RuntimeException("预分记录不存在: " + sortNo);
        }
        if (!"SCANNED".equals(preSort.getStatus())) {
            throw new RuntimeException("预分记录状态不允许收货: " + preSort.getStatus());
        }

        // 更新预分记录为已收货
        preSort.setReceivedQty(preSort.getBoxQty());
        preSort.setStatus("RECEIVED");
        preSort.setReceiveTime(LocalDateTime.now());
        preSort.setUpdatedTime(LocalDateTime.now());
        preSortMapper.updateById(preSort);

        // 如果需要越库，更新预配记录的已收货数量
        if ("XDOCK".equals(preSort.getXdockType()) && preSort.getAllocNo() != null) {
            CrossdockPreAllocation alloc = preAllocMapper.selectByAllocNo(preSort.getAllocNo());
            if (alloc != null) {
                alloc.setReceivedQty(alloc.getReceivedQty().add(preSort.getBoxQty()));
                alloc.setStatus("RECEIVED");
                preAllocMapper.updateById(alloc);
            }
        }

        // 更新越库单收货数量
        Crossdock crossdock = crossdockMapper.selectByCrossdockNo(preSort.getCrossdockNo());
        if (crossdock != null) {
            crossdock.setReceivedQty(crossdock.getReceivedQty().add(preSort.getBoxQty()));
            crossdockMapper.updateById(crossdock);
        }

        log.info("越库预分收货确认完成: sortNo={}, 收货数量={}", sortNo, preSort.getBoxQty());
        return preSort;
    }

    /** 越库预分分配出库 收货完成后，后台对已收货库存进行分配，关联出库单 */
    @Transactional(rollbackFor = Exception.class)
    public CrossdockPreSort allocatePreSortToOutbound(String sortNo, String operator) {
        log.info("越库预分分配出库: sortNo={}", sortNo);

        CrossdockPreSort preSort = preSortMapper.selectBySortNo(sortNo);
        if (preSort == null) {
            throw new RuntimeException("预分记录不存在: " + sortNo);
        }
        if (!"RECEIVED".equals(preSort.getStatus())) {
            throw new RuntimeException("预分记录状态不允许分配: " + preSort.getStatus());
        }
        if (!"XDOCK".equals(preSort.getXdockType())) {
            throw new RuntimeException("非越库类型，无需分配出库");
        }

        // 更新预分记录为已分配
        preSort.setStatus("ALLOCATED");
        preSort.setUpdatedTime(LocalDateTime.now());
        preSortMapper.updateById(preSort);

        // 更新预配记录的已分配数量
        if (preSort.getAllocNo() != null) {
            CrossdockPreAllocation alloc = preAllocMapper.selectByAllocNo(preSort.getAllocNo());
            if (alloc != null) {
                alloc.setAllocatedQty(alloc.getAllocatedQty().add(preSort.getBoxQty()));
                alloc.setStatus("ALLOCATED_OUT");
                preAllocMapper.updateById(alloc);
            }
        }

        // TODO: 调用出库模块接口，创建/更新出库单分配
        // 这里模拟分配成功
        log.info(
                "越库预分分配出库完成: sortNo={}, 出库单={}, 分配数量={}",
                sortNo,
                preSort.getOutboundNo(),
                preSort.getBoxQty());
        return preSort;
    }

    // ============================================================

    // 3. 与出库联动（越库货物直接进入分拣流程，跳过上架、拣货）
    // ============================================================

    /** 越库货物直接分拣到出库月台 跳过上架、拣货环节，直接从入库暂存区分拣到出库月台 */
    @Transactional(rollbackFor = Exception.class)
    public CrossdockTask directSortToOutbound(
            String crossdockNo,
            String sortNo,
            String fromLocation,
            String toLocation,
            BigDecimal sortQty,
            String operator) {
        log.info(
                "越库直接分拣到出库月台: crossdockNo={}, sortNo={}, {}->{}, qty={}",
                crossdockNo,
                sortNo,
                fromLocation,
                toLocation,
                sortQty);

        Crossdock crossdock = crossdockMapper.selectByCrossdockNo(crossdockNo);
        if (crossdock == null) {
            throw new RuntimeException("越库单不存在: " + crossdockNo);
        }

        // 更新预分记录为已分拣
        CrossdockPreSort preSort = preSortMapper.selectBySortNo(sortNo);
        if (preSort != null) {
            preSort.setSortedQty(preSort.getSortedQty().add(sortQty));
            if (preSort.getSortedQty().compareTo(preSort.getReceivedQty()) >= 0) {
                preSort.setStatus("SORTED");
                preSort.setSortTime(LocalDateTime.now());
            }
            preSortMapper.updateById(preSort);
        }

        // 更新越库单分拣数量
        crossdock.setSortedQty(crossdock.getSortedQty().add(sortQty));
        if ("RECEIVED".equals(crossdock.getStatus())
                || "PRE_ALLOCATED".equals(crossdock.getStatus())) {
            crossdock.setStatus("SORTING");
        }
        crossdockMapper.updateById(crossdock);

        // 创建分拣作业记录
        CrossdockTask task = new CrossdockTask();
        task.setTaskNo(generateTaskNo());
        task.setCrossdockNo(crossdockNo);
        task.setTaskType("DIRECT_SORT");
        task.setSkuCode(preSort != null ? preSort.getSkuCode() : null);
        task.setFromLocation(fromLocation);
        task.setToLocation(toLocation);
        task.setTaskQty(sortQty);
        task.setDoneQty(sortQty);
        task.setOperator(operator);
        task.setStatus("DONE");
        task.setStartTime(LocalDateTime.now());
        task.setEndTime(LocalDateTime.now());
        taskMapper.insert(task);

        // TODO: 调用出库模块接口，更新出库单拣货状态
        // 越库货物跳过拣货，直接标记为已拣货

        log.info("越库直接分拣完成: taskNo={}, qty={}", task.getTaskNo(), sortQty);
        return task;
    }

    /** 越库货物直接发运 分拣完成后，直接从出库月台装车发运 */
    @Transactional(rollbackFor = Exception.class)
    public CrossdockTask directShipFromDock(
            String crossdockNo, String fromLocation, BigDecimal shipQty, String operator) {
        log.info("越库直接发运: crossdockNo={}, qty={}", crossdockNo, shipQty);

        Crossdock crossdock = crossdockMapper.selectByCrossdockNo(crossdockNo);
        if (crossdock == null) {
            throw new RuntimeException("越库单不存在: " + crossdockNo);
        }

        // 更新越库单发运数量
        crossdock.setShippedQty(crossdock.getShippedQty().add(shipQty));
        if ("SORTED".equals(crossdock.getStatus()) || "SORTING".equals(crossdock.getStatus())) {
            crossdock.setStatus("SHIPPING");
        }
        crossdockMapper.updateById(crossdock);

        // 创建发运作业记录
        CrossdockTask task = new CrossdockTask();
        task.setTaskNo(generateTaskNo());
        task.setCrossdockNo(crossdockNo);
        task.setTaskType("DIRECT_SHIP");
        task.setFromLocation(fromLocation);
        task.setToLocation(crossdock.getOutboundDock());
        task.setTaskQty(shipQty);
        task.setDoneQty(shipQty);
        task.setOperator(operator);
        task.setStatus("DONE");
        task.setStartTime(LocalDateTime.now());
        task.setEndTime(LocalDateTime.now());
        taskMapper.insert(task);

        // 检查是否全部发运完成
        checkShipComplete(crossdockNo);

        // TODO: 调用出库模块接口，更新出库单发运状态

        log.info("越库直接发运完成: taskNo={}, qty={}", task.getTaskNo(), shipQty);
        return task;
    }

    /** 检查发运是否完成 */
    private void checkShipComplete(String crossdockNo) {
        Crossdock crossdock = crossdockMapper.selectByCrossdockNo(crossdockNo);
        List<CrossdockDetail> details = detailMapper.selectByCrossdockNo(crossdockNo);
        boolean allShipped =
                details.stream()
                        .allMatch(
                                d ->
                                        d.getShippedQty() != null
                                                && d.getShippedQty().compareTo(d.getExpectedQty())
                                                        >= 0);
        if (allShipped) {
            crossdock.setStatus("COMPLETED");
            crossdock.setEndTime(LocalDateTime.now());
            crossdockMapper.updateById(crossdock);
            log.info("越库单全部完成: {}", crossdockNo);
        }
    }

    // ============================================================

    // 4. 查询方法
    // ============================================================

    /** 查询越库预配记录 */
    public List<CrossdockPreAllocation> getPreAllocations(String crossdockNo) {
        return preAllocMapper.selectByCrossdockNo(crossdockNo);
    }

    /** 查询越库预分记录 */
    public List<CrossdockPreSort> getPreSorts(String crossdockNo) {
        return preSortMapper.selectByCrossdockNo(crossdockNo);
    }

    /** 根据箱号查询预分记录 */
    public CrossdockPreSort getPreSortByBoxNo(String boxNo) {
        return preSortMapper.selectByBoxNo(boxNo);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateAllocNo() {
        return "CDA"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateSortNo() {
        return "CDS"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateTaskNo() {
        return "CDT"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateSowingLocation() {
        return "SW-" + (int) (Math.random() * 100);
    }
}
