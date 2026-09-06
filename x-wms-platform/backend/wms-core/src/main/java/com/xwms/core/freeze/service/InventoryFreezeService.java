package com.xwms.core.freeze.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.freeze.entity.*;
import com.xwms.core.freeze.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库存冻结管理核心服务 核心能力: 冻结规则/冻结执行/解冻/冻结查询 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryFreezeService {

    private final InventoryFreezeMapper freezeMapper;
    private final InventoryFreezeDetailMapper detailMapper;
    private final FreezeReasonMapper reasonMapper;
    private final InventoryUnfreezeLogMapper unfreezeLogMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 冻结原因配置
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public FreezeReason createReason(FreezeReason reason) {
        reason.setStatus("ACTIVE");
        if (reason.getNeedApprove() == null) reason.setNeedApprove("N");
        if (reason.getAutoUnfreeze() == null) reason.setAutoUnfreeze("N");
        reasonMapper.insert(reason);
        log.info("创建冻结原因: {}={}", reason.getReasonCode(), reason.getReasonName());
        return reason;
    }

    public FreezeReason getReasonByCode(String reasonCode) {
        return reasonMapper.selectByReasonCode(reasonCode);
    }

    public Page<FreezeReason> pageReasons(Page<FreezeReason> page, String freezeType) {
        LambdaQueryWrapper<FreezeReason> wrapper = new LambdaQueryWrapper<>();
        if (freezeType != null) wrapper.eq(FreezeReason::getFreezeType, freezeType);
        wrapper.eq(FreezeReason::getStatus, "ACTIVE");
        return reasonMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. 冻结执行（核心）
    // ============================================================

    /**
     * 执行库存冻结
     *
     * @param freezeType 冻结类型
     * @param freezeReason 冻结原因
     * @param warehouseCode 仓库
     * @param ownerCode 货主
     * @param operator 操作人
     * @param refNo 关联单号
     * @param items 冻结明细列表
     * @return 冻结单
     */
    @Transactional(rollbackFor = Exception.class)
    public InventoryFreeze freezeInventory(
            String freezeType,
            String freezeReason,
            String warehouseCode,
            String ownerCode,
            String operator,
            String refNo,
            List<Map<String, Object>> items) {
        String freezeNo = generateFreezeNo();

        // 创建冻结单
        InventoryFreeze freeze = new InventoryFreeze();
        freeze.setFreezeNo(freezeNo);
        freeze.setFreezeType(freezeType);
        freeze.setFreezeReason(freezeReason);
        freeze.setWarehouseCode(warehouseCode);
        freeze.setOwnerCode(ownerCode);
        freeze.setStatus("FROZEN");
        freeze.setFreezeTime(LocalDateTime.now());
        freeze.setOperator(operator);
        freeze.setRefNo(refNo);
        freezeMapper.insert(freeze);

        BigDecimal totalQty = BigDecimal.ZERO;
        Set<String> skuSet = new HashSet<>();

        // 保存冻结明细
        for (Map<String, Object> item : items) {
            InventoryFreezeDetail detail = new InventoryFreezeDetail();
            detail.setFreezeNo(freezeNo);
            detail.setWarehouseCode(warehouseCode);
            detail.setOwnerCode(ownerCode);
            detail.setSkuCode((String) item.get("skuCode"));
            detail.setSkuName((String) item.get("skuName"));
            detail.setLocationCode((String) item.get("locationCode"));
            detail.setBatchNo((String) item.get("batchNo"));
            detail.setSerialNo((String) item.get("serialNo"));
            detail.setContainerNo((String) item.get("containerNo"));
            BigDecimal freezeQty = toBigDecimal(item.get("freezeQty"));
            detail.setFreezeQty(freezeQty);
            detail.setUnfreezeQty(BigDecimal.ZERO);
            detail.setRemainQty(freezeQty);
            detail.setBeforeStatus((String) item.get("beforeStatus"));
            detail.setAfterStatus("FROZEN");
            detail.setStatus("FROZEN");
            detail.setFreezeTime(LocalDateTime.now());
            detail.setOperator(operator);
            detail.setRemark((String) item.get("remark"));
            detailMapper.insert(detail);

            totalQty = totalQty.add(freezeQty);
            skuSet.add(detail.getSkuCode());
        }

        // 更新冻结单汇总
        freeze.setTotalSkuCount(skuSet.size());
        freeze.setTotalQty(totalQty);
        freezeMapper.updateById(freeze);

        log.info(
                "执行库存冻结: freezeNo={}, type={}, warehouse={}, skuCount={}, totalQty={}",
                freezeNo,
                freezeType,
                warehouseCode,
                skuSet.size(),
                totalQty);
        return freeze;
    }

    // ============================================================

    // 3. 解冻
    // ============================================================

    /** 全部解冻 */
    @Transactional(rollbackFor = Exception.class)
    public InventoryFreeze unfreezeAll(String freezeNo, String unfreezeReason, String operator) {
        InventoryFreeze freeze = freezeMapper.selectByFreezeNo(freezeNo);
        if (freeze == null) throw new RuntimeException("冻结单不存在: " + freezeNo);
        if ("UNFROZEN".equals(freeze.getStatus()) || "CANCELLED".equals(freeze.getStatus())) {
            throw new RuntimeException("冻结单状态不正确: " + freeze.getStatus());
        }

        List<InventoryFreezeDetail> details = detailMapper.selectByFreezeNo(freezeNo);
        for (InventoryFreezeDetail detail : details) {
            if ("FROZEN".equals(detail.getStatus()) || "PARTIAL".equals(detail.getStatus())) {
                // 记录解冻日志
                recordUnfreezeLog(
                        freezeNo, detail, detail.getRemainQty(), "FULL", unfreezeReason, operator);

                // 更新明细
                detailMapper.updateUnfreeze(
                        detail.getId(), detail.getFreezeQty(), BigDecimal.ZERO, "UNFROZEN");
            }
        }

        // 更新冻结单状态
        freezeMapper.updateStatus(freezeNo, "UNFROZEN");

        log.info("全部解冻: freezeNo={}, operator={}", freezeNo, operator);
        return freezeMapper.selectByFreezeNo(freezeNo);
    }

    /** 部分解冻 */
    @Transactional(rollbackFor = Exception.class)
    public InventoryFreezeDetail unfreezePart(
            Long detailId, BigDecimal unfreezeQty, String unfreezeReason, String operator) {
        InventoryFreezeDetail detail = detailMapper.selectById(detailId);
        if (detail == null) throw new RuntimeException("冻结明细不存在: " + detailId);
        if (!"FROZEN".equals(detail.getStatus()) && !"PARTIAL".equals(detail.getStatus())) {
            throw new RuntimeException("冻结明细状态不正确: " + detail.getStatus());
        }
        if (unfreezeQty.compareTo(detail.getRemainQty()) > 0) {
            throw new RuntimeException(
                    "解冻数量超过剩余冻结数量: " + unfreezeQty + " > " + detail.getRemainQty());
        }

        BigDecimal newUnfreezeQty = detail.getUnfreezeQty().add(unfreezeQty);
        BigDecimal newRemainQty = detail.getRemainQty().subtract(unfreezeQty);
        String newStatus = newRemainQty.compareTo(BigDecimal.ZERO) == 0 ? "UNFROZEN" : "PARTIAL";

        // 记录解冻日志
        recordUnfreezeLog(
                detail.getFreezeNo(), detail, unfreezeQty, "PART", unfreezeReason, operator);

        // 更新明细
        detailMapper.updateUnfreeze(detailId, newUnfreezeQty, newRemainQty, newStatus);

        // 检查冻结单是否全部解冻
        checkFreezeAllUnfrozen(detail.getFreezeNo());

        log.info("部分解冻: detailId={}, qty={}, remain={}", detailId, unfreezeQty, newRemainQty);
        return detailMapper.selectById(detailId);
    }

    /** 自动解冻（定时任务调用） */
    @Transactional(rollbackFor = Exception.class)
    public int autoUnfreeze() {
        // 查询需要自动解冻的冻结单
        List<FreezeReason> autoReasons =
                reasonMapper.selectList(
                        new LambdaQueryWrapper<FreezeReason>()
                                .eq(FreezeReason::getAutoUnfreeze, "Y")
                                .eq(FreezeReason::getStatus, "ACTIVE"));

        int count = 0;
        for (FreezeReason reason : autoReasons) {
            if (reason.getAutoUnfreezeHours() == null) continue;

            LocalDateTime threshold = LocalDateTime.now().minusHours(reason.getAutoUnfreezeHours());
            List<InventoryFreeze> freezes =
                    freezeMapper.selectList(
                            new LambdaQueryWrapper<InventoryFreeze>()
                                    .eq(InventoryFreeze::getFreezeType, reason.getFreezeType())
                                    .eq(InventoryFreeze::getStatus, "FROZEN")
                                    .le(InventoryFreeze::getFreezeTime, threshold));

            for (InventoryFreeze freeze : freezes) {
                unfreezeAll(freeze.getFreezeNo(), "自动解冻", "SYSTEM");
                count++;
            }
        }

        log.info("自动解冻完成: count={}", count);
        return count;
    }

    private void recordUnfreezeLog(
            String freezeNo,
            InventoryFreezeDetail detail,
            BigDecimal unfreezeQty,
            String unfreezeType,
            String unfreezeReason,
            String operator) {
        InventoryUnfreezeLog log = new InventoryUnfreezeLog();
        log.setUnfreezeNo(generateUnfreezeNo());
        log.setFreezeNo(freezeNo);
        log.setWarehouseCode(detail.getWarehouseCode());
        log.setSkuCode(detail.getSkuCode());
        log.setLocationCode(detail.getLocationCode());
        log.setBatchNo(detail.getBatchNo());
        log.setUnfreezeQty(unfreezeQty);
        log.setUnfreezeType(unfreezeType);
        log.setUnfreezeReason(unfreezeReason);
        log.setOperator(operator);
        log.setUnfreezeTime(LocalDateTime.now());
        unfreezeLogMapper.insert(log);
    }

    private void checkFreezeAllUnfrozen(String freezeNo) {
        List<InventoryFreezeDetail> details = detailMapper.selectByFreezeNo(freezeNo);
        boolean allUnfrozen = details.stream().allMatch(d -> "UNFROZEN".equals(d.getStatus()));
        if (allUnfrozen) {
            freezeMapper.updateStatus(freezeNo, "UNFROZEN");
        } else {
            freezeMapper.updateStatus(freezeNo, "PARTIAL");
        }
    }

    // ============================================================

    // 4. 取消冻结
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public InventoryFreeze cancelFreeze(String freezeNo, String reason, String operator) {
        InventoryFreeze freeze = freezeMapper.selectByFreezeNo(freezeNo);
        if (freeze == null) throw new RuntimeException("冻结单不存在: " + freezeNo);
        if (!"FROZEN".equals(freeze.getStatus())) {
            throw new RuntimeException("只有已冻结状态才能取消: " + freeze.getStatus());
        }

        // 先全部解冻
        unfreezeAll(freezeNo, "取消冻结: " + reason, operator);

        // 更新状态为已取消
        freezeMapper.updateStatus(freezeNo, "CANCELLED");

        log.info("取消冻结: freezeNo={}, reason={}", freezeNo, reason);
        return freezeMapper.selectByFreezeNo(freezeNo);
    }

    // ============================================================

    // 5. 查询
    // ============================================================

    public InventoryFreeze getByFreezeNo(String freezeNo) {
        return freezeMapper.selectByFreezeNo(freezeNo);
    }

    public List<InventoryFreezeDetail> getDetailsByFreezeNo(String freezeNo) {
        return detailMapper.selectByFreezeNo(freezeNo);
    }

    public List<InventoryFreezeDetail> getFrozenBySku(String skuCode) {
        return detailMapper.selectFrozenBySku(skuCode);
    }

    public List<InventoryFreezeDetail> getFrozenByLocation(String locationCode) {
        return detailMapper.selectFrozenByLocation(locationCode);
    }

    public List<InventoryFreezeDetail> getFrozenByBatch(String batchNo) {
        return detailMapper.selectFrozenByBatch(batchNo);
    }

    public Page<InventoryFreeze> pageFreezes(
            Page<InventoryFreeze> page, String warehouseCode, String freezeType, String status) {
        LambdaQueryWrapper<InventoryFreeze> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(InventoryFreeze::getWarehouseCode, warehouseCode);
        if (freezeType != null) wrapper.eq(InventoryFreeze::getFreezeType, freezeType);
        if (status != null) wrapper.eq(InventoryFreeze::getStatus, status);
        wrapper.orderByDesc(InventoryFreeze::getFreezeTime);
        return freezeMapper.selectPage(page, wrapper);
    }

    public List<InventoryUnfreezeLog> getUnfreezeLogsByFreezeNo(String freezeNo) {
        return unfreezeLogMapper.selectByFreezeNo(freezeNo);
    }

    public InventoryUnfreezeLog getUnfreezeLogByNo(String unfreezeNo) {
        return unfreezeLogMapper.selectByUnfreezeNo(unfreezeNo);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        return new BigDecimal(obj.toString());
    }

    private String generateFreezeNo() {
        return "FRZ"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateUnfreezeNo() {
        return "UFR"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
