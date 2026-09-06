package com.xwms.core.pallet.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.pallet.entity.*;
import com.xwms.core.pallet.enums.PalletOperationType;
import com.xwms.core.pallet.enums.PalletStatus;
import com.xwms.core.pallet.enums.PalletType;
import com.xwms.core.pallet.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 托盘/LPN管理核心服务 功能： 1. LPN管理（生成、查询、状态管理） 2. 码盘操作（收货码盘、组盘） 3. 拆盘操作 4. 托盘合并/拆分 5. 托盘移动/转移 6. 托盘封存/解封
 * 7. 码盘预约（预计算码盘方案和上架库位） 8. 托盘操作记录追溯
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PalletService {

    private final PalletMapper palletMapper;
    private final PalletDetailMapper palletDetailMapper;
    private final PalletOperationMapper operationMapper;
    private final PalletReservationMapper reservationMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. LPN管理
    // ============================================================

    /** 生成空托盘/LPN */
    @Transactional(rollbackFor = Exception.class)
    public Pallet createEmptyPallet(
            String palletType, String warehouseCode, String locationCode, String operator) {
        log.info(
                "生成空托盘: type={}, warehouse={}, location={}",
                palletType,
                warehouseCode,
                locationCode);

        Pallet pallet = new Pallet();
        pallet.setLpnNo(generateLpnNo());
        pallet.setPalletType(palletType != null ? palletType : PalletType.STANDARD.getCode());
        pallet.setStatus(PalletStatus.EMPTY.getCode());
        pallet.setWarehouseCode(warehouseCode);
        pallet.setLocationCode(locationCode);
        pallet.setSkuCount(0);
        pallet.setTotalQty(BigDecimal.ZERO);
        pallet.setTotalWeight(BigDecimal.ZERO);
        pallet.setTotalVolume(BigDecimal.ZERO);
        pallet.setMixedSku("N");
        pallet.setMixedBatch("N");
        pallet.setSealed("N");
        pallet.setSource("MANUAL");
        pallet.setCreatedBy(operator);
        palletMapper.insert(pallet);

        // 记录操作
        recordOperation(
                pallet.getLpnNo(),
                PalletOperationType.PALLETIZE.getCode(),
                null,
                PalletStatus.EMPTY.getCode(),
                null,
                locationCode,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                null,
                "生成空托盘",
                operator);

        log.info("生成空托盘完成: lpnNo={}", pallet.getLpnNo());
        return pallet;
    }

    /** 批量生成空托盘 */
    @Transactional(rollbackFor = Exception.class)
    public List<Pallet> batchCreateEmptyPallets(
            int count,
            String palletType,
            String warehouseCode,
            String locationCode,
            String operator) {
        log.info("批量生成空托盘: count={}, type={}", count, palletType);
        List<Pallet> pallets = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            pallets.add(createEmptyPallet(palletType, warehouseCode, locationCode, operator));
        }
        log.info("批量生成空托盘完成: count={}", pallets.size());
        return pallets;
    }

    public Pallet getPalletByLpn(String lpnNo) {
        return palletMapper.selectByLpnNo(lpnNo);
    }

    public List<PalletDetail> getPalletDetails(String lpnNo) {
        return palletDetailMapper.selectByLpnNo(lpnNo);
    }

    public Page<Pallet> pagePallets(
            Page<Pallet> page,
            String status,
            String palletType,
            String warehouseCode,
            String locationCode) {
        LambdaQueryWrapper<Pallet> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Pallet::getStatus, status);
        if (palletType != null) wrapper.eq(Pallet::getPalletType, palletType);
        if (warehouseCode != null) wrapper.eq(Pallet::getWarehouseCode, warehouseCode);
        if (locationCode != null) wrapper.eq(Pallet::getLocationCode, locationCode);
        wrapper.orderByDesc(Pallet::getCreatedTime);
        return palletMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. 码盘操作（收货码盘）
    // ============================================================

    /** 收货码盘（将收货商品码放到托盘上） */
    @Transactional(rollbackFor = Exception.class)
    public PalletDetail palletize(
            String lpnNo,
            String asnNo,
            String inboundNo,
            String inboundDetailNo,
            String skuCode,
            String skuName,
            BigDecimal qty,
            String batchNo,
            String serialNo,
            String operator) {
        log.info("收货码盘: lpn={}, sku={}, qty={}", lpnNo, skuCode, qty);

        Pallet pallet = palletMapper.selectByLpnNo(lpnNo);
        if (pallet == null) throw new RuntimeException("托盘不存在: " + lpnNo);

        if (PalletStatus.SHIPPED.getCode().equals(pallet.getStatus())
                || PalletStatus.DAMAGED.getCode().equals(pallet.getStatus())) {
            throw new RuntimeException("托盘状态不允许码盘: " + pallet.getStatus());
        }

        if ("Y".equals(pallet.getSealed())) {
            throw new RuntimeException("托盘已封存，不允许码盘: " + lpnNo);
        }

        String beforeStatus = pallet.getStatus();
        BigDecimal beforeQty = pallet.getTotalQty();

        // 检查托盘上是否已有同SKU
        List<PalletDetail> existingDetails = palletDetailMapper.selectByLpnAndSku(lpnNo, skuCode);
        PalletDetail detail;

        if (existingDetails != null && !existingDetails.isEmpty()) {
            // 同SKU已存在，累加数量
            detail = existingDetails.get(0);
            detail.setQty(detail.getQty().add(qty));
            detail.setRemainingQty(detail.getRemainingQty().add(qty));
            detail.setUpdatedBy(operator);
            detail.setUpdatedTime(LocalDateTime.now());
            palletDetailMapper.updateById(detail);
        } else {
            // 新建明细
            detail = new PalletDetail();
            detail.setDetailNo(generateDetailNo());
            detail.setLpnNo(lpnNo);
            detail.setLineNo(getNextLineNo(lpnNo));
            detail.setAsnNo(asnNo);
            detail.setInboundNo(inboundNo);
            detail.setInboundDetailNo(inboundDetailNo);
            detail.setSkuCode(skuCode);
            detail.setSkuName(skuName);
            detail.setQty(qty);
            detail.setPutawayQty(BigDecimal.ZERO);
            detail.setShippedQty(BigDecimal.ZERO);
            detail.setRemainingQty(qty);
            detail.setBatchNo(batchNo);
            detail.setSerialNo(serialNo);
            detail.setStatus("ON_PALLET");
            detail.setPalletizedBy(operator);
            detail.setPalletizedTime(LocalDateTime.now());
            detail.setCreatedBy(operator);
            palletDetailMapper.insert(detail);
        }

        // 更新托盘汇总
        updatePalletSummary(pallet, operator);

        // 记录操作
        recordOperation(
                lpnNo,
                PalletOperationType.PALLETIZE.getCode(),
                beforeStatus,
                pallet.getStatus(),
                pallet.getLocationCode(),
                pallet.getLocationCode(),
                beforeQty,
                pallet.getTotalQty(),
                qty,
                asnNo,
                inboundNo,
                skuCode,
                "收货码盘",
                operator);

        log.info(
                "收货码盘完成: lpn={}, sku={}, qty={}, 托盘总数量={}",
                lpnNo,
                skuCode,
                qty,
                pallet.getTotalQty());
        return detail;
    }

    // ============================================================

    // 3. 拆盘操作
    // ============================================================

    /** 拆盘（从托盘上移除商品） */
    @Transactional(rollbackFor = Exception.class)
    public PalletDetail depalletize(
            String lpnNo, String detailNo, BigDecimal qty, String reason, String operator) {
        log.info("拆盘: lpn={}, detail={}, qty={}", lpnNo, detailNo, qty);

        Pallet pallet = palletMapper.selectByLpnNo(lpnNo);
        if (pallet == null) throw new RuntimeException("托盘不存在: " + lpnNo);

        if ("Y".equals(pallet.getSealed())) {
            throw new RuntimeException("托盘已封存，不允许拆盘: " + lpnNo);
        }

        PalletDetail detail = palletDetailMapper.selectByDetailNo(detailNo);
        if (detail == null) throw new RuntimeException("托盘明细不存在: " + detailNo);

        if (qty.compareTo(detail.getRemainingQty()) > 0) {
            throw new RuntimeException(
                    String.format("拆盘数量超过剩余数量: 剩余=%s, 请求=%s", detail.getRemainingQty(), qty));
        }

        String beforeStatus = pallet.getStatus();
        BigDecimal beforeQty = pallet.getTotalQty();

        // 更新明细
        detail.setQty(detail.getQty().subtract(qty));
        detail.setRemainingQty(detail.getRemainingQty().subtract(qty));
        if (detail.getRemainingQty().compareTo(BigDecimal.ZERO) <= 0) {
            detail.setStatus("REMOVED");
        }
        detail.setUpdatedBy(operator);
        detail.setUpdatedTime(LocalDateTime.now());
        palletDetailMapper.updateById(detail);

        // 更新托盘汇总
        updatePalletSummary(pallet, operator);

        // 记录操作
        recordOperation(
                lpnNo,
                PalletOperationType.DEPALLETIZE.getCode(),
                beforeStatus,
                pallet.getStatus(),
                pallet.getLocationCode(),
                pallet.getLocationCode(),
                beforeQty,
                pallet.getTotalQty(),
                qty.negate(),
                detail.getAsnNo(),
                detail.getInboundNo(),
                detail.getSkuCode(),
                reason != null ? reason : "拆盘",
                operator);

        log.info("拆盘完成: lpn={}, detail={}, qty={}", lpnNo, detailNo, qty);
        return detail;
    }

    // ============================================================

    // 4. 托盘合并
    // ============================================================

    /** 托盘合并（将源托盘商品合并到目标托盘） */
    @Transactional(rollbackFor = Exception.class)
    public Pallet mergePallet(String sourceLpn, String targetLpn, String operator) {
        log.info("托盘合并: source={}, target={}", sourceLpn, targetLpn);

        Pallet sourcePallet = palletMapper.selectByLpnNo(sourceLpn);
        if (sourcePallet == null) throw new RuntimeException("源托盘不存在: " + sourceLpn);

        Pallet targetPallet = palletMapper.selectByLpnNo(targetLpn);
        if (targetPallet == null) throw new RuntimeException("目标托盘不存在: " + targetLpn);

        if ("Y".equals(targetPallet.getSealed())) {
            throw new RuntimeException("目标托盘已封存，不允许合并: " + targetLpn);
        }

        String sourceBeforeStatus = sourcePallet.getStatus();
        String targetBeforeStatus = targetPallet.getStatus();

        // 获取源托盘所有明细
        List<PalletDetail> sourceDetails = palletDetailMapper.selectByLpnNo(sourceLpn);

        for (PalletDetail sourceDetail : sourceDetails) {
            if (sourceDetail.getRemainingQty().compareTo(BigDecimal.ZERO) <= 0) continue;

            // 检查目标托盘是否已有同SKU
            List<PalletDetail> targetDetails =
                    palletDetailMapper.selectByLpnAndSku(targetLpn, sourceDetail.getSkuCode());
            if (targetDetails != null && !targetDetails.isEmpty()) {
                // 累加数量
                PalletDetail targetDetail = targetDetails.get(0);
                targetDetail.setQty(targetDetail.getQty().add(sourceDetail.getRemainingQty()));
                targetDetail.setRemainingQty(
                        targetDetail.getRemainingQty().add(sourceDetail.getRemainingQty()));
                targetDetail.setUpdatedBy(operator);
                targetDetail.setUpdatedTime(LocalDateTime.now());
                palletDetailMapper.updateById(targetDetail);
            } else {
                // 复制明细到目标托盘
                PalletDetail newDetail = new PalletDetail();
                newDetail.setDetailNo(generateDetailNo());
                newDetail.setLpnNo(targetLpn);
                newDetail.setLineNo(getNextLineNo(targetLpn));
                newDetail.setAsnNo(sourceDetail.getAsnNo());
                newDetail.setInboundNo(sourceDetail.getInboundNo());
                newDetail.setInboundDetailNo(sourceDetail.getInboundDetailNo());
                newDetail.setSkuCode(sourceDetail.getSkuCode());
                newDetail.setSkuName(sourceDetail.getSkuName());
                newDetail.setQty(sourceDetail.getRemainingQty());
                newDetail.setPutawayQty(BigDecimal.ZERO);
                newDetail.setShippedQty(BigDecimal.ZERO);
                newDetail.setRemainingQty(sourceDetail.getRemainingQty());
                newDetail.setBatchNo(sourceDetail.getBatchNo());
                newDetail.setSerialNo(sourceDetail.getSerialNo());
                newDetail.setStatus("ON_PALLET");
                newDetail.setPalletizedBy(operator);
                newDetail.setPalletizedTime(LocalDateTime.now());
                newDetail.setCreatedBy(operator);
                palletDetailMapper.insert(newDetail);
            }

            // 标记源明细已移除
            sourceDetail.setStatus("REMOVED");
            sourceDetail.setRemainingQty(BigDecimal.ZERO);
            sourceDetail.setUpdatedBy(operator);
            sourceDetail.setUpdatedTime(LocalDateTime.now());
            palletDetailMapper.updateById(sourceDetail);
        }

        // 更新目标托盘汇总
        updatePalletSummary(targetPallet, operator);

        // 源托盘变为空托盘
        sourcePallet.setStatus(PalletStatus.EMPTY.getCode());
        sourcePallet.setSkuCount(0);
        sourcePallet.setTotalQty(BigDecimal.ZERO);
        sourcePallet.setTotalWeight(BigDecimal.ZERO);
        sourcePallet.setTotalVolume(BigDecimal.ZERO);
        sourcePallet.setMixedSku("N");
        sourcePallet.setMixedBatch("N");
        sourcePallet.setUpdatedBy(operator);
        sourcePallet.setUpdatedTime(LocalDateTime.now());
        palletMapper.updateById(sourcePallet);

        // 记录操作
        recordOperation(
                targetLpn,
                PalletOperationType.MERGE.getCode(),
                targetBeforeStatus,
                targetPallet.getStatus(),
                targetPallet.getLocationCode(),
                targetPallet.getLocationCode(),
                null,
                targetPallet.getTotalQty(),
                sourcePallet.getTotalQty(),
                null,
                null,
                null,
                "托盘合并: 源=" + sourceLpn,
                operator);

        recordOperation(
                sourceLpn,
                PalletOperationType.MERGE.getCode(),
                sourceBeforeStatus,
                PalletStatus.EMPTY.getCode(),
                sourcePallet.getLocationCode(),
                sourcePallet.getLocationCode(),
                sourcePallet.getTotalQty(),
                BigDecimal.ZERO,
                sourcePallet.getTotalQty().negate(),
                null,
                null,
                null,
                "托盘合并到: 目标=" + targetLpn,
                operator);

        log.info("托盘合并完成: source={}, target={}", sourceLpn, targetLpn);
        return targetPallet;
    }

    // ============================================================

    // 5. 托盘移动
    // ============================================================

    /** 托盘移动/转移 */
    @Transactional(rollbackFor = Exception.class)
    public Pallet movePallet(
            String lpnNo, String targetLocation, String targetArea, String operator) {
        log.info("托盘移动: lpn={}, targetLocation={}", lpnNo, targetLocation);

        Pallet pallet = palletMapper.selectByLpnNo(lpnNo);
        if (pallet == null) throw new RuntimeException("托盘不存在: " + lpnNo);

        String beforeLocation = pallet.getLocationCode();
        String beforeStatus = pallet.getStatus();

        // 更新托盘位置
        pallet.setSourceLocation(beforeLocation);
        pallet.setTargetLocation(targetLocation);
        pallet.setLocationCode(targetLocation);
        pallet.setAreaCode(targetArea);
        pallet.setStatus(PalletStatus.IN_TRANSIT.getCode());
        pallet.setLastOperator(operator);
        pallet.setLastOperationTime(LocalDateTime.now());
        pallet.setUpdatedBy(operator);
        pallet.setUpdatedTime(LocalDateTime.now());
        palletMapper.updateById(pallet);

        // 记录操作
        recordOperation(
                lpnNo,
                PalletOperationType.MOVE.getCode(),
                beforeStatus,
                PalletStatus.IN_TRANSIT.getCode(),
                beforeLocation,
                targetLocation,
                pallet.getTotalQty(),
                pallet.getTotalQty(),
                BigDecimal.ZERO,
                null,
                null,
                null,
                "托盘移动: " + beforeLocation + "→" + targetLocation,
                operator);

        log.info("托盘移动完成: lpn={}, {}→{}", lpnNo, beforeLocation, targetLocation);
        return pallet;
    }

    /** 确认托盘到达目标库位 */
    @Transactional(rollbackFor = Exception.class)
    public Pallet confirmPalletArrival(String lpnNo, String operator) {
        log.info("确认托盘到达: lpn={}", lpnNo);

        Pallet pallet = palletMapper.selectByLpnNo(lpnNo);
        if (pallet == null) throw new RuntimeException("托盘不存在: " + lpnNo);

        String beforeStatus = pallet.getStatus();

        pallet.setStatus(PalletStatus.STORED.getCode());
        pallet.setLastOperator(operator);
        pallet.setLastOperationTime(LocalDateTime.now());
        pallet.setUpdatedBy(operator);
        pallet.setUpdatedTime(LocalDateTime.now());
        palletMapper.updateById(pallet);

        // 记录操作
        recordOperation(
                lpnNo,
                PalletOperationType.MOVE.getCode(),
                beforeStatus,
                PalletStatus.STORED.getCode(),
                pallet.getLocationCode(),
                pallet.getLocationCode(),
                pallet.getTotalQty(),
                pallet.getTotalQty(),
                BigDecimal.ZERO,
                null,
                null,
                null,
                "确认托盘到达库位",
                operator);

        log.info("确认托盘到达完成: lpn={}, location={}", lpnNo, pallet.getLocationCode());
        return pallet;
    }

    // ============================================================

    // 6. 托盘封存/解封
    // ============================================================

    /** 封存托盘（封存后不允许再放货） */
    @Transactional(rollbackFor = Exception.class)
    public Pallet sealPallet(String lpnNo, String reason, String operator) {
        log.info("封存托盘: lpn={}, reason={}", lpnNo, reason);

        Pallet pallet = palletMapper.selectByLpnNo(lpnNo);
        if (pallet == null) throw new RuntimeException("托盘不存在: " + lpnNo);

        String beforeStatus = pallet.getStatus();

        pallet.setSealed("Y");
        pallet.setSealedTime(LocalDateTime.now());
        pallet.setSealedBy(operator);
        pallet.setStatus(PalletStatus.FULL.getCode());
        pallet.setLastOperator(operator);
        pallet.setLastOperationTime(LocalDateTime.now());
        pallet.setUpdatedBy(operator);
        pallet.setUpdatedTime(LocalDateTime.now());
        palletMapper.updateById(pallet);

        // 记录操作
        recordOperation(
                lpnNo,
                PalletOperationType.SEAL.getCode(),
                beforeStatus,
                PalletStatus.FULL.getCode(),
                pallet.getLocationCode(),
                pallet.getLocationCode(),
                pallet.getTotalQty(),
                pallet.getTotalQty(),
                BigDecimal.ZERO,
                null,
                null,
                null,
                reason != null ? reason : "封存托盘",
                operator);

        log.info("封存托盘完成: lpn={}", lpnNo);
        return pallet;
    }

    /** 解封托盘 */
    @Transactional(rollbackFor = Exception.class)
    public Pallet unsealPallet(String lpnNo, String reason, String operator) {
        log.info("解封托盘: lpn={}, reason={}", lpnNo, reason);

        Pallet pallet = palletMapper.selectByLpnNo(lpnNo);
        if (pallet == null) throw new RuntimeException("托盘不存在: " + lpnNo);

        String beforeStatus = pallet.getStatus();

        pallet.setSealed("N");
        pallet.setStatus(PalletStatus.IN_USE.getCode());
        pallet.setLastOperator(operator);
        pallet.setLastOperationTime(LocalDateTime.now());
        pallet.setUpdatedBy(operator);
        pallet.setUpdatedTime(LocalDateTime.now());
        palletMapper.updateById(pallet);

        // 记录操作
        recordOperation(
                lpnNo,
                PalletOperationType.UNSEAL.getCode(),
                beforeStatus,
                PalletStatus.IN_USE.getCode(),
                pallet.getLocationCode(),
                pallet.getLocationCode(),
                pallet.getTotalQty(),
                pallet.getTotalQty(),
                BigDecimal.ZERO,
                null,
                null,
                null,
                reason != null ? reason : "解封托盘",
                operator);

        log.info("解封托盘完成: lpn={}", lpnNo);
        return pallet;
    }

    // ============================================================

    // 7. 码盘预约
    // ============================================================

    /** 创建码盘预约（收货前预计算码盘方案和上架库位） */
    @Transactional(rollbackFor = Exception.class)
    public PalletReservation createReservation(
            String asnNo,
            String inboundNo,
            String poNo,
            String warehouseCode,
            String reservedReceiveArea,
            String reservedDockNo,
            String palletizeStrategy,
            String putawayStrategy,
            String operator) {
        log.info("创建码盘预约: asn={}, warehouse={}", asnNo, warehouseCode);

        PalletReservation reservation = new PalletReservation();
        reservation.setReservationNo(generateReservationNo());
        reservation.setAsnNo(asnNo);
        reservation.setInboundNo(inboundNo);
        reservation.setPoNo(poNo);
        reservation.setWarehouseCode(warehouseCode);
        reservation.setReservedReceiveArea(reservedReceiveArea);
        reservation.setReservedDockNo(reservedDockNo);
        reservation.setPalletizeStrategy(palletizeStrategy != null ? palletizeStrategy : "MIX_SKU");
        reservation.setPutawayStrategy(putawayStrategy != null ? putawayStrategy : "NEAREST");
        reservation.setStatus("PENDING");
        reservation.setReservationTime(LocalDateTime.now());
        reservation.setReservedBy(operator);
        reservation.setCreatedBy(operator);
        reservationMapper.insert(reservation);

        log.info("创建码盘预约完成: reservationNo={}", reservation.getReservationNo());
        return reservation;
    }

    /** 确认码盘预约（预计算托盘数和库位） */
    @Transactional(rollbackFor = Exception.class)
    public PalletReservation confirmReservation(
            String reservationNo,
            int reservedPalletCount,
            BigDecimal reservedTotalQty,
            String reservedPutawayLocations,
            String operator) {
        log.info("确认码盘预约: reservationNo={}, palletCount={}", reservationNo, reservedPalletCount);

        PalletReservation reservation = reservationMapper.selectByReservationNo(reservationNo);
        if (reservation == null) throw new RuntimeException("码盘预约不存在: " + reservationNo);

        reservation.setReservedPalletCount(reservedPalletCount);
        reservation.setReservedTotalQty(reservedTotalQty);
        reservation.setReservedPutawayLocations(reservedPutawayLocations);
        reservation.setStatus("RESERVED");
        reservation.setUpdatedBy(operator);
        reservation.setUpdatedTime(LocalDateTime.now());
        reservationMapper.updateById(reservation);

        log.info("确认码盘预约完成: reservationNo={}", reservationNo);
        return reservation;
    }

    /** 开始码盘预约 */
    @Transactional(rollbackFor = Exception.class)
    public PalletReservation startReservation(String reservationNo, String operator) {
        PalletReservation reservation = reservationMapper.selectByReservationNo(reservationNo);
        if (reservation == null) throw new RuntimeException("码盘预约不存在: " + reservationNo);

        reservation.setStatus("IN_PROGRESS");
        reservation.setStartTime(LocalDateTime.now());
        reservation.setOperator(operator);
        reservation.setUpdatedBy(operator);
        reservation.setUpdatedTime(LocalDateTime.now());
        reservationMapper.updateById(reservation);

        log.info("开始码盘预约: reservationNo={}", reservationNo);
        return reservation;
    }

    /** 完成码盘预约 */
    @Transactional(rollbackFor = Exception.class)
    public PalletReservation completeReservation(
            String reservationNo,
            int actualPalletCount,
            BigDecimal actualTotalQty,
            String operator) {
        PalletReservation reservation = reservationMapper.selectByReservationNo(reservationNo);
        if (reservation == null) throw new RuntimeException("码盘预约不存在: " + reservationNo);

        reservation.setActualPalletCount(actualPalletCount);
        reservation.setActualTotalQty(actualTotalQty);
        reservation.setStatus("COMPLETED");
        reservation.setCompleteTime(LocalDateTime.now());
        reservation.setOperator(operator);
        reservation.setUpdatedBy(operator);
        reservation.setUpdatedTime(LocalDateTime.now());
        reservationMapper.updateById(reservation);

        log.info("完成码盘预约: reservationNo={}", reservationNo);
        return reservation;
    }

    public PalletReservation getReservationByNo(String reservationNo) {
        return reservationMapper.selectByReservationNo(reservationNo);
    }

    public List<PalletReservation> getReservationsByStatus(String status) {
        return reservationMapper.selectByStatus(status);
    }

    // ============================================================

    // 8. 托盘操作记录查询
    // ============================================================

    public List<PalletOperation> getOperationHistory(String lpnNo) {
        return operationMapper.selectByLpnNo(lpnNo);
    }

    // ============================================================

    // 9. 公共方法
    // ============================================================

    /** 更新托盘汇总信息 */
    private void updatePalletSummary(Pallet pallet, String operator) {
        List<PalletDetail> details = palletDetailMapper.selectByLpnNo(pallet.getLpnNo());

        // 过滤有效明细
        List<PalletDetail> validDetails =
                details.stream()
                        .filter(d -> !"REMOVED".equals(d.getStatus()))
                        .filter(d -> d.getRemainingQty().compareTo(BigDecimal.ZERO) > 0)
                        .collect(Collectors.toList());

        // 计算SKU种类数
        long skuCount = validDetails.stream().map(PalletDetail::getSkuCode).distinct().count();

        // 计算总数量
        BigDecimal totalQty =
                validDetails.stream()
                        .map(PalletDetail::getRemainingQty)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算总重量
        BigDecimal totalWeight =
                validDetails.stream()
                        .map(
                                d ->
                                        d.getProductWeight() != null
                                                ? d.getProductWeight().multiply(d.getRemainingQty())
                                                : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 判断是否混SKU
        String mixedSku = skuCount > 1 ? "Y" : "N";

        // 判断是否混批次
        long batchCount =
                validDetails.stream()
                        .map(PalletDetail::getBatchNo)
                        .filter(b -> b != null && !b.isEmpty())
                        .distinct()
                        .count();
        String mixedBatch = batchCount > 1 ? "Y" : "N";

        // 更新托盘
        pallet.setSkuCount((int) skuCount);
        pallet.setTotalQty(totalQty);
        pallet.setTotalWeight(totalWeight);
        pallet.setMixedSku(mixedSku);
        pallet.setMixedBatch(mixedBatch);

        // 更新状态
        if (totalQty.compareTo(BigDecimal.ZERO) == 0) {
            pallet.setStatus(PalletStatus.EMPTY.getCode());
        } else if ("Y".equals(pallet.getSealed())) {
            pallet.setStatus(PalletStatus.FULL.getCode());
        } else {
            pallet.setStatus(PalletStatus.IN_USE.getCode());
        }

        pallet.setPalletizedBy(operator);
        pallet.setPalletizedTime(LocalDateTime.now());
        pallet.setLastOperator(operator);
        pallet.setLastOperationTime(LocalDateTime.now());
        pallet.setUpdatedBy(operator);
        pallet.setUpdatedTime(LocalDateTime.now());
        palletMapper.updateById(pallet);
    }

    /** 记录托盘操作 */
    private void recordOperation(
            String lpnNo,
            String operationType,
            String beforeStatus,
            String afterStatus,
            String beforeLocation,
            String afterLocation,
            BigDecimal beforeQty,
            BigDecimal afterQty,
            BigDecimal operationQty,
            String asnNo,
            String inboundNo,
            String skuCode,
            String reason,
            String operator) {
        PalletOperation operation = new PalletOperation();
        operation.setOperationNo(generateOperationNo());
        operation.setLpnNo(lpnNo);
        operation.setOperationType(operationType);
        operation.setBeforeStatus(beforeStatus);
        operation.setAfterStatus(afterStatus);
        operation.setBeforeLocation(beforeLocation);
        operation.setAfterLocation(afterLocation);
        operation.setBeforeQty(beforeQty);
        operation.setAfterQty(afterQty);
        operation.setOperationQty(operationQty);
        operation.setAsnNo(asnNo);
        operation.setInboundNo(inboundNo);
        operation.setSkuCode(skuCode);
        operation.setReason(reason);
        operation.setOperator(operator);
        operation.setOperationTime(LocalDateTime.now());
        operation.setCreatedBy(operator);
        operationMapper.insert(operation);
    }

    /** 获取下一个行号 */
    private int getNextLineNo(String lpnNo) {
        List<PalletDetail> details = palletDetailMapper.selectByLpnNo(lpnNo);
        if (details == null || details.isEmpty()) return 1;
        return details.size() + 1;
    }

    // ============================================================

    // 10. 编号生成
    // ============================================================

    private String generateLpnNo() {
        return "LPN"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateDetailNo() {
        return "PLD"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateOperationNo() {
        return "PLO"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateReservationNo() {
        return "PLR"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
