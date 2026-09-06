package com.xwms.core.snapshot.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.snapshot.entity.*;
import com.xwms.core.snapshot.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库存快照管理核心服务 核心能力: 快照生成/快照查询/快照对比/快照恢复 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventorySnapshotService {

    private final InventorySnapshotMapper snapshotMapper;
    private final InventorySnapshotDetailMapper detailMapper;
    private final SnapshotCompareMapper compareMapper;
    private final SnapshotCompareDetailMapper compareDetailMapper;
    private final SnapshotRestoreLogMapper restoreLogMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 快照生成（核心）
    // ============================================================

    /**
     * 生成库存快照
     *
     * @param snapshotType 快照类型
     * @param warehouseCode 仓库
     * @param ownerCode 货主
     * @param snapshotDate 快照日期
     * @param operator 操作人
     * @param inventoryData 当前库存数据
     * @return 快照记录
     */
    @Transactional(rollbackFor = Exception.class)
    public InventorySnapshot createSnapshot(
            String snapshotType,
            String warehouseCode,
            String ownerCode,
            LocalDate snapshotDate,
            String operator,
            List<Map<String, Object>> inventoryData) {
        String snapshotNo = generateSnapshotNo();

        // 创建快照主记录
        InventorySnapshot snapshot = new InventorySnapshot();
        snapshot.setSnapshotNo(snapshotNo);
        snapshot.setSnapshotType(snapshotType);
        snapshot.setWarehouseCode(warehouseCode);
        snapshot.setOwnerCode(ownerCode);
        snapshot.setSnapshotDate(snapshotDate);
        snapshot.setSnapshotTime(LocalDateTime.now());
        snapshot.setStatus("PROCESSING");
        snapshot.setOperator(operator);
        snapshotMapper.insert(snapshot);

        try {
            BigDecimal totalQty = BigDecimal.ZERO;
            BigDecimal totalCost = BigDecimal.ZERO;
            BigDecimal totalValue = BigDecimal.ZERO;
            Set<String> skuSet = new HashSet<>();

            // 保存快照明细
            for (Map<String, Object> item : inventoryData) {
                InventorySnapshotDetail detail = new InventorySnapshotDetail();
                detail.setSnapshotNo(snapshotNo);
                detail.setWarehouseCode(warehouseCode);
                detail.setOwnerCode(ownerCode);
                detail.setSkuCode((String) item.get("skuCode"));
                detail.setSkuName((String) item.get("skuName"));
                detail.setCategoryCode((String) item.get("categoryCode"));
                detail.setLocationCode((String) item.get("locationCode"));
                detail.setBatchNo((String) item.get("batchNo"));
                detail.setSerialNo((String) item.get("serialNo"));
                detail.setContainerNo((String) item.get("containerNo"));
                detail.setQuantity(toBigDecimal(item.get("quantity")));
                detail.setAvailableQty(toBigDecimal(item.get("availableQty")));
                detail.setAllocatedQty(toBigDecimal(item.get("allocatedQty")));
                detail.setPickingQty(toBigDecimal(item.get("pickingQty")));
                detail.setFrozenQty(toBigDecimal(item.get("frozenQty")));
                detail.setUnitCost(toBigDecimal(item.get("unitCost")));
                detail.setTotalCost(toBigDecimal(item.get("totalCost")));
                detail.setAbcClass((String) item.get("abcClass"));
                detail.setInventoryStatus((String) item.get("inventoryStatus"));
                detailMapper.insert(detail);

                totalQty = totalQty.add(detail.getQuantity());
                if (detail.getTotalCost() != null) totalCost = totalCost.add(detail.getTotalCost());
                if (detail.getUnitCost() != null && detail.getQuantity() != null) {
                    totalValue =
                            totalValue.add(detail.getUnitCost().multiply(detail.getQuantity()));
                }
                skuSet.add(detail.getSkuCode());
            }

            // 更新快照主记录
            snapshot.setTotalSkuCount(skuSet.size());
            snapshot.setTotalQty(totalQty);
            snapshot.setTotalCost(totalCost);
            snapshot.setTotalValue(totalValue);
            snapshot.setStatus("COMPLETED");
            snapshotMapper.updateById(snapshot);

            log.info(
                    "生成库存快照: snapshotNo={}, type={}, warehouse={}, skuCount={}, totalQty={}",
                    snapshotNo,
                    snapshotType,
                    warehouseCode,
                    skuSet.size(),
                    totalQty);
            return snapshot;

        } catch (Exception e) {
            snapshot.setStatus("FAILED");
            snapshot.setFailReason(e.getMessage());
            snapshotMapper.updateById(snapshot);
            log.error("生成库存快照失败: snapshotNo={}, error={}", snapshotNo, e.getMessage(), e);
            throw e;
        }
    }

    // ============================================================

    // 2. 快照查询
    // ============================================================

    public InventorySnapshot getBySnapshotNo(String snapshotNo) {
        return snapshotMapper.selectBySnapshotNo(snapshotNo);
    }

    public InventorySnapshot getByDateAndType(
            String warehouseCode, LocalDate snapshotDate, String snapshotType) {
        return snapshotMapper.selectByDateAndType(warehouseCode, snapshotDate, snapshotType);
    }

    public List<InventorySnapshot> getByDateRange(
            String warehouseCode, LocalDate startDate, LocalDate endDate) {
        return snapshotMapper.selectByDateRange(warehouseCode, startDate, endDate);
    }

    public List<InventorySnapshotDetail> getDetailsBySnapshotNo(String snapshotNo) {
        return detailMapper.selectBySnapshotNo(snapshotNo);
    }

    public List<InventorySnapshotDetail> getDetailsBySnapshotNoAndSku(
            String snapshotNo, String skuCode) {
        return detailMapper.selectBySnapshotNoAndSku(snapshotNo, skuCode);
    }

    public Page<InventorySnapshot> pageSnapshots(
            Page<InventorySnapshot> page,
            String warehouseCode,
            String snapshotType,
            String status) {
        LambdaQueryWrapper<InventorySnapshot> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(InventorySnapshot::getWarehouseCode, warehouseCode);
        if (snapshotType != null) wrapper.eq(InventorySnapshot::getSnapshotType, snapshotType);
        if (status != null) wrapper.eq(InventorySnapshot::getStatus, status);
        wrapper.orderByDesc(InventorySnapshot::getSnapshotDate);
        return snapshotMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 3. 快照对比
    // ============================================================

    /** 对比两个快照 */
    @Transactional(rollbackFor = Exception.class)
    public SnapshotCompare compareSnapshots(
            String snapshotNo1, String snapshotNo2, String compareType, String operator) {
        InventorySnapshot snapshot1 = snapshotMapper.selectBySnapshotNo(snapshotNo1);
        InventorySnapshot snapshot2 = snapshotMapper.selectBySnapshotNo(snapshotNo2);
        if (snapshot1 == null || snapshot2 == null) {
            throw new RuntimeException("快照不存在: " + snapshotNo1 + " 或 " + snapshotNo2);
        }

        List<InventorySnapshotDetail> details1 = detailMapper.selectBySnapshotNo(snapshotNo1);
        List<InventorySnapshotDetail> details2 = detailMapper.selectBySnapshotNo(snapshotNo2);

        // 构建快照2的Map
        Map<String, InventorySnapshotDetail> map2 = new HashMap<>();
        for (InventorySnapshotDetail d : details2) {
            String key = d.getSkuCode() + "|" + d.getLocationCode() + "|" + d.getBatchNo();
            map2.put(key, d);
        }

        String compareNo = generateCompareNo();
        int diffSkuCount = 0;
        BigDecimal totalDiffQty = BigDecimal.ZERO;
        BigDecimal totalDiffCost = BigDecimal.ZERO;

        // 对比快照1中的每个SKU
        for (InventorySnapshotDetail d1 : details1) {
            String key = d1.getSkuCode() + "|" + d1.getLocationCode() + "|" + d1.getBatchNo();
            InventorySnapshotDetail d2 = map2.get(key);

            BigDecimal qty1 = d1.getQuantity() != null ? d1.getQuantity() : BigDecimal.ZERO;
            BigDecimal qty2 =
                    d2 != null && d2.getQuantity() != null ? d2.getQuantity() : BigDecimal.ZERO;
            BigDecimal cost1 = d1.getTotalCost() != null ? d1.getTotalCost() : BigDecimal.ZERO;
            BigDecimal cost2 =
                    d2 != null && d2.getTotalCost() != null ? d2.getTotalCost() : BigDecimal.ZERO;

            BigDecimal diffQty = qty2.subtract(qty1);
            BigDecimal diffCost = cost2.subtract(cost1);

            if (diffQty.compareTo(BigDecimal.ZERO) != 0
                    || diffCost.compareTo(BigDecimal.ZERO) != 0
                    || d2 == null) {
                diffSkuCount++;
                totalDiffQty = totalDiffQty.add(diffQty.abs());
                totalDiffCost = totalDiffCost.add(diffCost.abs());

                String diffType;
                if (d2 == null) {
                    diffType = "REMOVED";
                } else if (qty1.compareTo(BigDecimal.ZERO) == 0) {
                    diffType = "NEW";
                } else if (diffQty.compareTo(BigDecimal.ZERO) > 0) {
                    diffType = "INCREASE";
                } else {
                    diffType = "DECREASE";
                }

                SnapshotCompareDetail compareDetail = new SnapshotCompareDetail();
                compareDetail.setCompareNo(compareNo);
                compareDetail.setSkuCode(d1.getSkuCode());
                compareDetail.setLocationCode(d1.getLocationCode());
                compareDetail.setBatchNo(d1.getBatchNo());
                compareDetail.setQty1(qty1);
                compareDetail.setQty2(qty2);
                compareDetail.setDiffQty(diffQty);
                compareDetail.setCost1(cost1);
                compareDetail.setCost2(cost2);
                compareDetail.setDiffCost(diffCost);
                compareDetail.setDiffType(diffType);
                compareDetailMapper.insert(compareDetail);
            }
            map2.remove(key);
        }

        // 快照2中新增的SKU
        for (InventorySnapshotDetail d2 : map2.values()) {
            diffSkuCount++;
            BigDecimal qty2 = d2.getQuantity() != null ? d2.getQuantity() : BigDecimal.ZERO;
            BigDecimal cost2 = d2.getTotalCost() != null ? d2.getTotalCost() : BigDecimal.ZERO;
            totalDiffQty = totalDiffQty.add(qty2);
            totalDiffCost = totalDiffCost.add(cost2);

            SnapshotCompareDetail compareDetail = new SnapshotCompareDetail();
            compareDetail.setCompareNo(compareNo);
            compareDetail.setSkuCode(d2.getSkuCode());
            compareDetail.setLocationCode(d2.getLocationCode());
            compareDetail.setBatchNo(d2.getBatchNo());
            compareDetail.setQty1(BigDecimal.ZERO);
            compareDetail.setQty2(qty2);
            compareDetail.setDiffQty(qty2);
            compareDetail.setCost1(BigDecimal.ZERO);
            compareDetail.setCost2(cost2);
            compareDetail.setDiffCost(cost2);
            compareDetail.setDiffType("NEW");
            compareDetailMapper.insert(compareDetail);
        }

        SnapshotCompare compare = new SnapshotCompare();
        compare.setCompareNo(compareNo);
        compare.setSnapshotNo1(snapshotNo1);
        compare.setSnapshotNo2(snapshotNo2);
        compare.setWarehouseCode(snapshot1.getWarehouseCode());
        compare.setCompareType(compareType);
        compare.setTotalDiffSku(diffSkuCount);
        compare.setTotalDiffQty(totalDiffQty);
        compare.setTotalDiffCost(totalDiffCost);
        compare.setStatus("COMPLETED");
        compare.setOperator(operator);
        compare.setCompareTime(LocalDateTime.now());
        compareMapper.insert(compare);

        log.info(
                "快照对比完成: compareNo={}, {} vs {}, diffSku={}, diffQty={}",
                compareNo,
                snapshotNo1,
                snapshotNo2,
                diffSkuCount,
                totalDiffQty);
        return compare;
    }

    public SnapshotCompare getCompareByNo(String compareNo) {
        return compareMapper.selectByCompareNo(compareNo);
    }

    public List<SnapshotCompareDetail> getCompareDetails(String compareNo) {
        return compareDetailMapper.selectByCompareNo(compareNo);
    }

    // ============================================================

    // 4. 快照恢复
    // ============================================================

    /** 从快照恢复库存 注意: 这是高风险操作，需要审批 */
    @Transactional(rollbackFor = Exception.class)
    public SnapshotRestoreLog restoreFromSnapshot(
            String snapshotNo,
            String restoreType,
            List<String> skuCodes,
            String operator,
            String remark) {
        InventorySnapshot snapshot = snapshotMapper.selectBySnapshotNo(snapshotNo);
        if (snapshot == null) throw new RuntimeException("快照不存在: " + snapshotNo);

        String restoreNo = generateRestoreNo();
        SnapshotRestoreLog restoreLog = new SnapshotRestoreLog();
        restoreLog.setRestoreNo(restoreNo);
        restoreLog.setSnapshotNo(snapshotNo);
        restoreLog.setWarehouseCode(snapshot.getWarehouseCode());
        restoreLog.setRestoreType(restoreType);
        restoreLog.setRestoreScope(skuCodes != null ? String.join(",", skuCodes) : "ALL");
        restoreLog.setStatus("PROCESSING");
        restoreLog.setOperator(operator);
        restoreLog.setRemark(remark);
        restoreLogMapper.insert(restoreLog);

        try {
            // 获取快照明细
            List<InventorySnapshotDetail> details = detailMapper.selectBySnapshotNo(snapshotNo);

            // 过滤需要恢复的SKU
            if ("PARTIAL".equals(restoreType) && skuCodes != null && !skuCodes.isEmpty()) {
                Set<String> skuSet = new HashSet<>(skuCodes);
                details.removeIf(d -> !skuSet.contains(d.getSkuCode()));
            }

            BigDecimal restoreQty = BigDecimal.ZERO;
            for (InventorySnapshotDetail detail : details) {
                // TODO: 实际恢复逻辑 - 将当前库存调整为快照时的库存
                // 需要调用库存调整服务，生成调整单
                restoreQty =
                        restoreQty.add(
                                detail.getQuantity() != null
                                        ? detail.getQuantity()
                                        : BigDecimal.ZERO);
            }

            restoreLog.setRestoreSkuCount(details.size());
            restoreLog.setRestoreQty(restoreQty);
            restoreLog.setStatus("COMPLETED");
            restoreLog.setRestoreTime(LocalDateTime.now());
            restoreLogMapper.updateById(restoreLog);

            log.info(
                    "快照恢复完成: restoreNo={}, snapshotNo={}, type={}, skuCount={}, qty={}",
                    restoreNo,
                    snapshotNo,
                    restoreType,
                    details.size(),
                    restoreQty);
            return restoreLog;

        } catch (Exception e) {
            restoreLog.setStatus("FAILED");
            restoreLog.setFailReason(e.getMessage());
            restoreLogMapper.updateById(restoreLog);
            log.error("快照恢复失败: restoreNo={}, error={}", restoreNo, e.getMessage(), e);
            throw e;
        }
    }

    public SnapshotRestoreLog getRestoreByNo(String restoreNo) {
        return restoreLogMapper.selectByRestoreNo(restoreNo);
    }

    public Page<SnapshotRestoreLog> pageRestoreLogs(
            Page<SnapshotRestoreLog> page, String warehouseCode, String status) {
        LambdaQueryWrapper<SnapshotRestoreLog> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(SnapshotRestoreLog::getWarehouseCode, warehouseCode);
        if (status != null) wrapper.eq(SnapshotRestoreLog::getStatus, status);
        wrapper.orderByDesc(SnapshotRestoreLog::getCreatedTime);
        return restoreLogMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return null;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        return new BigDecimal(obj.toString());
    }

    private String generateSnapshotNo() {
        return "SNP"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateCompareNo() {
        return "CMP"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateRestoreNo() {
        return "RST"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
