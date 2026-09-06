package com.xwms.core.po.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.inbound.entity.Asn;
import com.xwms.core.inbound.entity.InboundDetail;
import com.xwms.core.inbound.entity.InboundOrder;
import com.xwms.core.inbound.mapper.AsnMapper;
import com.xwms.core.inbound.mapper.InboundDetailMapper;
import com.xwms.core.inbound.mapper.InboundOrderMapper;
import com.xwms.core.po.entity.*;
import com.xwms.core.po.enums.PoStatus;
import com.xwms.core.po.mapper.*;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 采购订单（PO）服务 核心功能： 1. PO管理（创建/更新/查询/审批/取消） 2. PO明细管理 3. PO提取生成ASN（一个ASN可从多个PO提取；一个PO可关联多个ASN） 4.
 * PO收货数量更新（ASN收货后自动更新PO收货数量） 5. PO状态控制（CREATED→RELEASED→PARTIAL_RECEIVED→FULLY_RECEIVED→COMPLETED）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderMapper poMapper;
    private final PurchaseOrderDetailMapper poDetailMapper;
    private final PoAsnRelationMapper poAsnRelationMapper;
    private final AsnMapper asnMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final InboundDetailMapper inboundDetailMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. PO管理
    // ============================================================

    /** 创建采购订单 */
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder createPo(PurchaseOrder po, List<PurchaseOrderDetail> details) {
        if (po.getPoNo() == null) {
            po.setPoNo(generatePoNo());
        }
        if (po.getStatus() == null) po.setStatus(PoStatus.CREATED.getCode());
        if (po.getReleaseStatus() == null) po.setReleaseStatus("UNRELEASED");
        if (po.getAsnLinked() == null) po.setAsnLinked("Y");
        if (po.getTotalQty() == null) po.setTotalQty(BigDecimal.ZERO);
        if (po.getReleasedQty() == null) po.setReleasedQty(BigDecimal.ZERO);
        if (po.getReceivedQty() == null) po.setReceivedQty(BigDecimal.ZERO);
        if (po.getPutawayQty() == null) po.setPutawayQty(BigDecimal.ZERO);

        poMapper.insert(po);

        // 保存明细
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (details != null) {
            for (int i = 0; i < details.size(); i++) {
                PurchaseOrderDetail detail = details.get(i);
                detail.setPoNo(po.getPoNo());
                detail.setLineNo(i + 1);
                detail.setDetailNo(generateDetailNo());
                if (detail.getOrderQty() == null) detail.setOrderQty(BigDecimal.ZERO);
                if (detail.getReleasedQty() == null) detail.setReleasedQty(BigDecimal.ZERO);
                if (detail.getReceivedQty() == null) detail.setReceivedQty(BigDecimal.ZERO);
                if (detail.getPutawayQty() == null) detail.setPutawayQty(BigDecimal.ZERO);
                if (detail.getStatus() == null) detail.setStatus(PoStatus.CREATED.getCode());
                poDetailMapper.insert(detail);

                totalQty = totalQty.add(detail.getOrderQty());
                if (detail.getAmount() != null) {
                    totalAmount = totalAmount.add(detail.getAmount());
                }
            }
        }

        // 更新PO汇总数量
        po.setTotalQty(totalQty);
        po.setTotalAmount(totalAmount);
        poMapper.updateById(po);

        log.info(
                "创建采购订单: {}, 明细{}行, 总数量{}",
                po.getPoNo(),
                details != null ? details.size() : 0,
                totalQty);
        return po;
    }

    /** 更新采购订单 */
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder updatePo(PurchaseOrder po) {
        poMapper.updateById(po);
        return poMapper.selectById(po.getId());
    }

    /** 审批采购订单 */
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder approvePo(String poNo, String approver, String approvalOpinion) {
        PurchaseOrder po = poMapper.selectByPoNo(poNo);
        if (po == null) throw new RuntimeException("采购订单不存在: " + poNo);

        po.setApprovalStatus("APPROVED");
        po.setApprover(approver);
        po.setApprovalTime(LocalDateTime.now());
        po.setStatus(PoStatus.RELEASED.getCode());
        po.setReleaseStatus("FULLY");
        poMapper.updateById(po);

        // 更新明细状态
        List<PurchaseOrderDetail> details = poDetailMapper.selectByPoNo(poNo);
        for (PurchaseOrderDetail detail : details) {
            detail.setStatus(PoStatus.RELEASED.getCode());
            poDetailMapper.updateById(detail);
        }

        log.info("审批采购订单: {}, 审批人: {}", poNo, approver);
        return po;
    }

    /** 取消采购订单 */
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder cancelPo(String poNo, String cancelReason, String operator) {
        PurchaseOrder po = poMapper.selectByPoNo(poNo);
        if (po == null) throw new RuntimeException("采购订单不存在: " + poNo);

        // 已收货的PO不允许取消
        if (po.getReceivedQty() != null && po.getReceivedQty().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("已收货的采购订单不允许取消: " + poNo);
        }

        po.setStatus(PoStatus.CANCELLED.getCode());
        po.setRemark(cancelReason);
        po.setUpdatedBy(operator);
        po.setUpdatedTime(LocalDateTime.now());
        poMapper.updateById(po);

        // 更新明细状态
        List<PurchaseOrderDetail> details = poDetailMapper.selectByPoNo(poNo);
        for (PurchaseOrderDetail detail : details) {
            detail.setStatus(PoStatus.CANCELLED.getCode());
            detail.setCloseReason(cancelReason);
            poDetailMapper.updateById(detail);
        }

        log.info("取消采购订单: {}, 原因: {}", poNo, cancelReason);
        return po;
    }

    public PurchaseOrder getPoByNo(String poNo) {
        return poMapper.selectByPoNo(poNo);
    }

    public Page<PurchaseOrder> pagePos(
            Page<PurchaseOrder> page,
            String poType,
            String status,
            String supplierCode,
            String warehouseCode,
            String ownerCode) {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        if (poType != null) wrapper.eq(PurchaseOrder::getPoType, poType);
        if (status != null) wrapper.eq(PurchaseOrder::getStatus, status);
        if (supplierCode != null) wrapper.eq(PurchaseOrder::getSupplierCode, supplierCode);
        if (warehouseCode != null) wrapper.eq(PurchaseOrder::getWarehouseCode, warehouseCode);
        if (ownerCode != null) wrapper.eq(PurchaseOrder::getOwnerCode, ownerCode);
        wrapper.orderByDesc(PurchaseOrder::getCreatedTime);
        return poMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. PO明细管理
    // ============================================================

    public List<PurchaseOrderDetail> getPoDetails(String poNo) {
        return poDetailMapper.selectByPoNo(poNo);
    }

    public PurchaseOrderDetail getPoDetailByNo(String detailNo) {
        return poDetailMapper.selectByDetailNo(detailNo);
    }

    /** 查询可释放的明细（orderQty > releasedQty） */
    public List<PurchaseOrderDetail> getReleasableDetails(String poNo) {
        return poDetailMapper.selectReleasableDetails(poNo);
    }

    // ============================================================

    // 3. PO提取生成ASN（核心功能）
    // ============================================================

    /**
     * 从PO提取生成ASN 支持一个ASN从多个PO提取，一个PO可关联多个ASN
     *
     * @param asnHeader ASN表头信息
     * @param releaseItems 提取明细列表（PO号+PO明细号+提取数量）
     * @return 生成的ASN
     */
    @Transactional(rollbackFor = Exception.class)
    public Asn releaseToAsn(Asn asnHeader, List<ReleaseItem> releaseItems) {
        log.info("从PO提取生成ASN: 提取明细{}条", releaseItems.size());

        if (releaseItems == null || releaseItems.isEmpty()) {
            throw new RuntimeException("提取明细不能为空");
        }

        // 1. 创建ASN
        if (asnHeader.getAsnNo() == null) {
            asnHeader.setAsnNo(generateAsnNo());
        }
        if (asnHeader.getStatus() == null) asnHeader.setStatus("CREATED");
        if (asnHeader.getReceivedQty() == null) asnHeader.setReceivedQty(BigDecimal.ZERO);
        asnMapper.insert(asnHeader);

        // 2. 创建入库单
        InboundOrder inboundOrder = new InboundOrder();
        inboundOrder.setInboundNo(generateInboundNo());
        inboundOrder.setAsnNo(asnHeader.getAsnNo());
        inboundOrder.setInboundType("PURCHASE");
        inboundOrder.setWarehouseCode(asnHeader.getWarehouseCode());
        inboundOrder.setOwnerCode(asnHeader.getOwnerCode());
        inboundOrder.setSupplierCode(asnHeader.getSupplierCode());
        inboundOrder.setStatus("CREATED");
        inboundOrder.setReceivedQty(BigDecimal.ZERO);
        inboundOrder.setPutawayQty(BigDecimal.ZERO);
        inboundOrderMapper.insert(inboundOrder);

        // 3. 处理每条提取明细
        BigDecimal totalReleaseQty = BigDecimal.ZERO;
        int lineNo = 1;

        for (ReleaseItem item : releaseItems) {
            // 3.1 校验PO明细
            PurchaseOrderDetail poDetail = poDetailMapper.selectByDetailNo(item.getPoDetailNo());
            if (poDetail == null) {
                throw new RuntimeException("PO明细不存在: " + item.getPoDetailNo());
            }

            // 3.2 校验可释放数量
            BigDecimal availableQty = poDetail.getOrderQty().subtract(poDetail.getReleasedQty());
            if (item.getReleaseQty().compareTo(availableQty) > 0) {
                throw new RuntimeException(
                        String.format(
                                "PO明细可释放数量不足: 明细=%s, 可释放=%s, 请求=%s",
                                item.getPoDetailNo(), availableQty, item.getReleaseQty()));
            }

            // 3.3 更新PO明细释放数量
            poDetail.setReleasedQty(poDetail.getReleasedQty().add(item.getReleaseQty()));
            // 判断是否完全释放
            if (poDetail.getReleasedQty().compareTo(poDetail.getOrderQty()) >= 0) {
                poDetail.setStatus("RELEASED");
            } else {
                poDetail.setStatus("PARTIAL_RELEASED");
            }
            poDetailMapper.updateById(poDetail);

            // 3.4 创建PO-ASN关联
            PoAsnRelation relation = new PoAsnRelation();
            relation.setRelationNo(generateRelationNo());
            relation.setPoNo(item.getPoNo());
            relation.setPoLineNo(poDetail.getLineNo());
            relation.setPoDetailNo(item.getPoDetailNo());
            relation.setAsnNo(asnHeader.getAsnNo());
            relation.setAsnLineNo(lineNo);
            relation.setSkuCode(poDetail.getSkuCode());
            relation.setReleaseQty(item.getReleaseQty());
            relation.setReceivedQty(BigDecimal.ZERO);
            relation.setPutawayQty(BigDecimal.ZERO);
            relation.setReleaseTime(LocalDateTime.now());
            relation.setStatus("RELEASED");
            poAsnRelationMapper.insert(relation);

            // 3.5 创建入库单明细
            InboundDetail inboundDetail = new InboundDetail();
            inboundDetail.setInboundNo(inboundOrder.getInboundNo());
            inboundDetail.setLineNo(lineNo);
            inboundDetail.setDetailNo(generateInboundDetailNo());
            inboundDetail.setSkuCode(poDetail.getSkuCode());
            inboundDetail.setSkuName(poDetail.getSkuName());
            inboundDetail.setBarcode(poDetail.getBarcode());
            inboundDetail.setUnit(poDetail.getUnit());
            inboundDetail.setExpectedQty(item.getReleaseQty());
            inboundDetail.setReceivedQty(BigDecimal.ZERO);
            inboundDetail.setPutawayQty(BigDecimal.ZERO);
            inboundDetail.setStatus("CREATED");
            inboundDetailMapper.insert(inboundDetail);

            totalReleaseQty = totalReleaseQty.add(item.getReleaseQty());
            lineNo++;
        }

        // 4. 更新PO汇总释放数量和状态
        updatePoReleaseStatus(releaseItems);

        // 5. 更新ASN和入库单汇总
        asnHeader.setExpectedQty(totalReleaseQty);
        asnMapper.updateById(asnHeader);

        inboundOrder.setTotalQty(totalReleaseQty);
        inboundOrderMapper.updateById(inboundOrder);

        log.info(
                "从PO提取生成ASN完成: asnNo={}, 总数量={}, 明细{}条",
                asnHeader.getAsnNo(),
                totalReleaseQty,
                releaseItems.size());

        return asnHeader;
    }

    /** 更新PO释放状态 */
    private void updatePoReleaseStatus(List<ReleaseItem> releaseItems) {
        // 按PO号分组
        java.util.Map<String, List<ReleaseItem>> poGroup =
                releaseItems.stream()
                        .collect(java.util.stream.Collectors.groupingBy(ReleaseItem::getPoNo));

        for (String poNo : poGroup.keySet()) {
            PurchaseOrder po = poMapper.selectByPoNo(poNo);
            if (po == null) continue;

            // 重新计算释放数量
            List<PurchaseOrderDetail> details = poDetailMapper.selectByPoNo(poNo);
            BigDecimal releasedQty =
                    details.stream()
                            .map(PurchaseOrderDetail::getReleasedQty)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            po.setReleasedQty(releasedQty);

            // 更新释放状态
            if (releasedQty.compareTo(BigDecimal.ZERO) == 0) {
                po.setReleaseStatus("UNRELEASED");
            } else if (releasedQty.compareTo(po.getTotalQty()) >= 0) {
                po.setReleaseStatus("FULLY");
                if (PoStatus.CREATED.getCode().equals(po.getStatus())) {
                    po.setStatus(PoStatus.RELEASED.getCode());
                }
            } else {
                po.setReleaseStatus("PARTIAL");
                if (PoStatus.CREATED.getCode().equals(po.getStatus())) {
                    po.setStatus(PoStatus.RELEASED.getCode());
                }
            }

            poMapper.updateById(po);
        }
    }

    // ============================================================

    // 4. PO收货数量更新（ASN收货后自动调用）
    // ============================================================

    /**
     * ASN收货后更新PO收货数量
     *
     * @param asnNo ASN编号
     * @param skuCode 商品编码
     * @param receivedQty 收货数量
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePoReceivedQty(String asnNo, String skuCode, BigDecimal receivedQty) {
        log.info("更新PO收货数量: asnNo={}, sku={}, qty={}", asnNo, skuCode, receivedQty);

        // 1. 查询PO-ASN关联
        List<PoAsnRelation> relations = poAsnRelationMapper.selectByAsnNo(asnNo);
        if (relations == null || relations.isEmpty()) {
            log.warn("未找到PO-ASN关联: asnNo={}", asnNo);
            return;
        }

        // 2. 找到对应商品的关联
        for (PoAsnRelation relation : relations) {
            if (skuCode.equals(relation.getSkuCode())) {
                // 更新关联收货数量
                relation.setReceivedQty(relation.getReceivedQty().add(receivedQty));
                if (relation.getReceivedQty().compareTo(relation.getReleaseQty()) >= 0) {
                    relation.setStatus("RECEIVED");
                }
                poAsnRelationMapper.updateById(relation);

                // 更新PO明细收货数量
                PurchaseOrderDetail poDetail =
                        poDetailMapper.selectByDetailNo(relation.getPoDetailNo());
                if (poDetail != null) {
                    poDetail.setReceivedQty(poDetail.getReceivedQty().add(receivedQty));
                    if (poDetail.getReceivedQty().compareTo(poDetail.getOrderQty()) >= 0) {
                        poDetail.setStatus("FULLY_RECEIVED");
                    } else {
                        poDetail.setStatus("PARTIAL_RECEIVED");
                    }
                    poDetailMapper.updateById(poDetail);
                }

                // 更新PO汇总收货数量和状态
                updatePoReceiveStatus(relation.getPoNo());

                log.info(
                        "更新PO收货数量完成: poNo={}, detailNo={}, receivedQty={}",
                        relation.getPoNo(),
                        relation.getPoDetailNo(),
                        receivedQty);
                break;
            }
        }
    }

    /** 更新PO收货状态 */
    private void updatePoReceiveStatus(String poNo) {
        PurchaseOrder po = poMapper.selectByPoNo(poNo);
        if (po == null) return;

        // 重新计算收货数量
        List<PurchaseOrderDetail> details = poDetailMapper.selectByPoNo(poNo);
        BigDecimal receivedQty =
                details.stream()
                        .map(PurchaseOrderDetail::getReceivedQty)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        po.setReceivedQty(receivedQty);

        // 更新收货状态
        if (receivedQty.compareTo(BigDecimal.ZERO) == 0) {
            // 未收货，保持原状态
        } else if (receivedQty.compareTo(po.getTotalQty()) >= 0) {
            po.setStatus(PoStatus.FULLY_RECEIVED.getCode());
        } else {
            po.setStatus(PoStatus.PARTIAL_RECEIVED.getCode());
        }

        poMapper.updateById(po);
    }

    // ============================================================

    // 5. PO-ASN关联查询
    // ============================================================

    /** 根据PO号查询关联的ASN列表 */
    public List<PoAsnRelation> getRelationsByPoNo(String poNo) {
        return poAsnRelationMapper.selectByPoNo(poNo);
    }

    /** 根据ASN号查询关联的PO列表 */
    public List<PoAsnRelation> getRelationsByAsnNo(String asnNo) {
        return poAsnRelationMapper.selectByAsnNo(asnNo);
    }

    // ============================================================

    // 6. 数据模型
    // ============================================================

    /** PO提取明细项 */
    @Data
    public static class ReleaseItem {
        /** PO号 */
        private String poNo;

        /** PO明细号 */
        private String poDetailNo;

        /** 提取数量 */
        private BigDecimal releaseQty;
    }

    // ============================================================

    // 7. 编号生成
    // ============================================================

    private String generatePoNo() {
        return "PO"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateDetailNo() {
        return "POD"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateAsnNo() {
        return "ASN"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateInboundNo() {
        return "IN"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateInboundDetailNo() {
        return "IND"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateRelationNo() {
        return "PAR"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
