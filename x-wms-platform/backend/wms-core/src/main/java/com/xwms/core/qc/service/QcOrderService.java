package com.xwms.core.qc.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.exception.BizException;
import com.xwms.core.qc.dto.*;
import com.xwms.core.qc.entity.*;
import com.xwms.core.qc.enums.QcOrderStatus;
import com.xwms.core.qc.enums.QcResult;
import com.xwms.core.qc.enums.QcType;
import com.xwms.core.qc.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 质检单核心服务 包含: 创建质检单/抽样计算/质检执行/结果判定/让步接收/不合格品处理 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QcOrderService {

    private final QcOrderMapper qcOrderMapper;
    private final QcItemMapper qcItemMapper;
    private final QcRuleMapper qcRuleMapper;
    private final QcSamplingPlanMapper samplingPlanMapper;
    private final QcUnqualifiedMapper unqualifiedMapper;
    private final QcConcessionMapper concessionMapper;
    private final SupplierQualityMapper supplierQualityMapper;

    private static final AtomicInteger QC_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter QC_NO_FMT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 创建质检单
    // ============================================================

    /** 根据入库单创建质检单 匹配规则: SKU+供应商 > SKU > 通用 > 默认抽检 */
    @Transactional(rollbackFor = Exception.class)
    public QcOrder createQcOrder(QcCreateRequest request) {
        // 1. 匹配质检规则
        QcRule rule = matchQcRule(request.getSku(), request.getSupplierCode());
        QcType qcType = QcType.of(rule != null ? rule.getQcType() : "SAMPLE");

        // 免检直接返回
        if (qcType == QcType.NONE) {
            log.info("商品{}配置免检, 跳过质检", request.getSku());
            return null;
        }

        // 2. 生成质检单号
        String qcNo = generateQcNo();

        // 3. 计算抽样方案
        BigDecimal sampleQty = request.getLotQty();
        Integer acceptNumber = null;
        Integer rejectNumber = null;
        String aqlLevel = rule != null ? rule.getAqlLevel() : "2.5";

        if (qcType == QcType.SAMPLE) {
            QcSamplingPlan plan =
                    samplingPlanMapper.selectByLotSize(
                            rule != null ? rule.getInspectionLevel() : "II",
                            request.getLotQty().intValue(),
                            rule != null ? rule.getStrictness() : "NORMAL");
            if (plan != null) {
                sampleQty = new BigDecimal(plan.getSampleSize());
                acceptNumber = getAcceptNumber(plan, aqlLevel);
                rejectNumber = getRejectNumber(plan, aqlLevel);
            }
        }

        // 4. 创建质检单
        QcOrder order = new QcOrder();
        order.setQcNo(qcNo);
        order.setRefType(request.getRefType());
        order.setRefNo(request.getRefNo());
        order.setRefItemId(request.getRefItemId());
        order.setSku(request.getSku());
        order.setBarcode(request.getBarcode());
        order.setProductName(request.getProductName());
        order.setSupplierCode(request.getSupplierCode());
        order.setOwnerCode(request.getOwnerCode());
        order.setWarehouseCode(request.getWarehouseCode());
        order.setLocationCode(request.getLocationCode());
        order.setBatchNo(request.getBatchNo());
        order.setQcType(qcType.getCode());
        order.setLotQty(request.getLotQty());
        order.setSampleQty(sampleQty);
        order.setInspectedQty(BigDecimal.ZERO);
        order.setQualifiedQty(BigDecimal.ZERO);
        order.setUnqualifiedQty(BigDecimal.ZERO);
        order.setAqlLevel(aqlLevel);
        order.setAcceptNumber(acceptNumber);
        order.setRejectNumber(rejectNumber);
        order.setDefectCount(0);
        order.setStatus(QcOrderStatus.PENDING.getCode());
        order.setOwnerCodeCol(request.getOwnerCode());
        order.setWarehouseCodeCol(request.getWarehouseCode());

        qcOrderMapper.insert(order);
        log.info("创建质检单: {}, 类型: {}, 抽样数: {}", qcNo, qcType.getDesc(), sampleQty);
        return order;
    }

    /** 匹配质检规则 (优先级: SKU+供应商 > SKU > 通用 > 默认抽检) */
    private QcRule matchQcRule(String sku, String supplierCode) {
        if (sku != null && supplierCode != null) {
            QcRule rule = qcRuleMapper.matchRule(sku, supplierCode);
            if (rule != null) return rule;
        }
        if (sku != null) {
            QcRule rule = qcRuleMapper.matchSkuRule(sku);
            if (rule != null) return rule;
        }
        return qcRuleMapper.matchDefaultRule();
    }

    // ============================================================

    // 2. 开始质检
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public QcOrder startQc(Long qcId, String inspector) {
        QcOrder order = getQcOrder(qcId);
        if (!QcOrderStatus.PENDING.getCode().equals(order.getStatus())) {
            throw new BizException("质检单状态不允许开始: " + order.getStatus());
        }
        order.setStatus(QcOrderStatus.INSPECTING.getCode());
        order.setInspector(inspector);
        order.setStartTime(LocalDateTime.now());
        qcOrderMapper.updateById(order);
        return order;
    }

    // ============================================================

    // 3. 录入质检结果
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public QcOrder submitQcResult(QcSubmitRequest request) {
        QcOrder order = getQcOrder(request.getQcId());
        if (!QcOrderStatus.INSPECTING.getCode().equals(order.getStatus())) {
            throw new BizException("质检单状态不允许提交: " + order.getStatus());
        }

        // 1. 保存检验明细
        if (request.getItems() != null) {
            for (QcItem item : request.getItems()) {
                item.setQcId(order.getId());
                qcItemMapper.insert(item);
            }
        }

        // 2. 统计结果
        BigDecimal inspectedQty =
                request.getInspectedQty() != null
                        ? request.getInspectedQty()
                        : order.getSampleQty();
        BigDecimal qualifiedQty =
                request.getQualifiedQty() != null ? request.getQualifiedQty() : BigDecimal.ZERO;
        BigDecimal unqualifiedQty = inspectedQty.subtract(qualifiedQty);
        int defectCount =
                request.getDefectCount() != null
                        ? request.getDefectCount()
                        : unqualifiedQty.intValue();

        order.setInspectedQty(inspectedQty);
        order.setQualifiedQty(qualifiedQty);
        order.setUnqualifiedQty(unqualifiedQty);
        order.setDefectCount(defectCount);
        order.setInspectTime(LocalDateTime.now());
        order.setFinishTime(LocalDateTime.now());

        // 3. 判定结果
        String result = judgeResult(order, defectCount);
        order.setResult(result);

        if (QcResult.PASSED.getCode().equals(result)) {
            order.setStatus(QcOrderStatus.PASSED.getCode());
            log.info("质检单{}合格", order.getQcNo());
        } else if (QcResult.FAILED.getCode().equals(result)) {
            order.setStatus(QcOrderStatus.FAILED.getCode());
            // 创建不合格品记录
            createUnqualified(
                    order, unqualifiedQty, request.getUnqualifiedType(), request.getDefectDesc());
            log.info("质检单{}不合格, 不合格数: {}", order.getQcNo(), unqualifiedQty);
        }

        qcOrderMapper.updateById(order);

        // 4. 更新供应商质量评分(异步)
        // updateSupplierQuality(order.getSupplierCode());

        return order;
    }

    /** 判定质检结果 全检: 不合格数=0则合格 抽检: 不良数 <= 接收数则合格, >= 拒收数则不合格 */
    private String judgeResult(QcOrder order, int defectCount) {
        if (QcType.FULL.getCode().equals(order.getQcType())) {
            return defectCount == 0 ? QcResult.PASSED.getCode() : QcResult.FAILED.getCode();
        }
        // 抽检
        if (order.getAcceptNumber() != null && defectCount <= order.getAcceptNumber()) {
            return QcResult.PASSED.getCode();
        }
        if (order.getRejectNumber() != null && defectCount >= order.getRejectNumber()) {
            return QcResult.FAILED.getCode();
        }
        // 介于接收数和拒收数之间(GB2828箭头指向), 按箭头方向判定
        return defectCount == 0 ? QcResult.PASSED.getCode() : QcResult.FAILED.getCode();
    }

    // ============================================================

    // 4. 让步接收
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public QcConcession applyConcession(QcConcessionRequest request) {
        QcOrder order = getQcOrder(request.getQcId());
        if (!QcOrderStatus.FAILED.getCode().equals(order.getStatus())) {
            throw new BizException("只有不合格的质检单才能申请让步接收");
        }

        QcConcession concession = new QcConcession();
        concession.setQcId(order.getId());
        concession.setSku(order.getSku());
        concession.setBatchNo(order.getBatchNo());
        concession.setQty(request.getQty());
        concession.setReason(request.getReason());
        concession.setDefectDesc(request.getDefectDesc());
        concession.setImpactLevel(request.getImpactLevel());
        concession.setApplicant(request.getApplicant());
        concession.setApplyTime(LocalDateTime.now());
        concession.setStatus("PENDING");
        concession.setOwnerCodeCol(order.getOwnerCodeCol());
        concession.setWarehouseCodeCol(order.getWarehouseCodeCol());
        concessionMapper.insert(concession);

        order.setStatus(QcOrderStatus.CONCESSION_PENDING.getCode());
        qcOrderMapper.updateById(order);

        return concession;
    }

    @Transactional(rollbackFor = Exception.class)
    public QcConcession approveConcession(
            Long concessionId, boolean approved, String approver, String opinion) {
        QcConcession concession = concessionMapper.selectById(concessionId);
        if (concession == null) throw new BizException("让步接收单不存在");
        if (!"PENDING".equals(concession.getStatus())) throw new BizException("状态不允许审批");

        concession.setApprover(approver);
        concession.setApproveTime(LocalDateTime.now());
        concession.setApproveOpinion(opinion);
        concession.setStatus(approved ? "APPROVED" : "REJECTED");
        concessionMapper.updateById(concession);

        QcOrder order = qcOrderMapper.selectById(concession.getQcId());
        if (approved) {
            order.setStatus(QcOrderStatus.CONCESSION_APPROVED.getCode());
            order.setResult(QcResult.CONCESSION.getCode());
            // 让步接收后不合格品转为可用
        } else {
            order.setStatus(QcOrderStatus.FAILED.getCode());
        }
        qcOrderMapper.updateById(order);

        return concession;
    }

    // ============================================================

    // 5. 不合格品处理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public QcUnqualified handleUnqualified(
            Long unqualifiedId, String handleMethod, String handler, String cert) {
        QcUnqualified unq = unqualifiedMapper.selectById(unqualifiedId);
        if (unq == null) throw new BizException("不合格品记录不存在");

        unq.setHandleMethod(handleMethod);
        unq.setHandleStatus("PROCESSING");
        unq.setHandler(handler);
        unq.setHandleTime(LocalDateTime.now());
        unq.setDisposeCert(cert);
        unqualifiedMapper.updateById(unq);

        // 更新质检单状态
        QcOrder order = qcOrderMapper.selectById(unq.getQcId());
        if (order != null) {
            order.setStatus(QcOrderStatus.DISPOSED.getCode());
            qcOrderMapper.updateById(order);
        }

        return unq;
    }

    private void createUnqualified(QcOrder order, BigDecimal qty, String type, String desc) {
        QcUnqualified unq = new QcUnqualified();
        unq.setQcId(order.getId());
        unq.setSku(order.getSku());
        unq.setBarcode(order.getBarcode());
        unq.setBatchNo(order.getBatchNo());
        unq.setQty(qty);
        unq.setUnqualifiedType(type != null ? type : "OTHER");
        unq.setDefectDesc(desc);
        unq.setHandleStatus("PENDING");
        unq.setOwnerCodeCol(order.getOwnerCodeCol());
        unq.setWarehouseCodeCol(order.getWarehouseCodeCol());
        unqualifiedMapper.insert(unq);
    }

    // ============================================================

    // 6. 查询
    // ============================================================

    public QcOrder getQcOrder(Long id) {
        QcOrder order = qcOrderMapper.selectById(id);
        if (order == null) throw new BizException("质检单不存在: " + id);
        return order;
    }

    public Page<QcOrder> pageQcOrders(Page<QcOrder> page, String status, String sku, String refNo) {
        LambdaQueryWrapper<QcOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(QcOrder::getStatus, status);
        if (sku != null) wrapper.like(QcOrder::getSku, sku);
        if (refNo != null) wrapper.eq(QcOrder::getRefNo, refNo);
        wrapper.orderByDesc(QcOrder::getCreatedAt);
        return qcOrderMapper.selectPage(page, wrapper);
    }

    public List<QcItem> getQcItems(Long qcId) {
        return qcItemMapper.selectByQcId(qcId);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateQcNo() {
        return "QC"
                + LocalDateTime.now().format(QC_NO_FMT)
                + String.format("%03d", QC_SEQ.incrementAndGet() % 1000);
    }

    private Integer getAcceptNumber(QcSamplingPlan plan, String aql) {
        return switch (aql) {
            case "0.065" -> plan.getAql0065Accept();
            case "0.10" -> plan.getAql010Accept();
            case "0.15" -> plan.getAql015Accept();
            case "0.25" -> plan.getAql025Accept();
            case "0.40" -> plan.getAql040Accept();
            case "0.65" -> plan.getAql065Accept();
            case "1.0" -> plan.getAql10Accept();
            case "1.5" -> plan.getAql15Accept();
            case "2.5" -> plan.getAql25Accept();
            case "4.0" -> plan.getAql40Accept();
            case "6.5" -> plan.getAql65Accept();
            default -> null;
        };
    }

    private Integer getRejectNumber(QcSamplingPlan plan, String aql) {
        return switch (aql) {
            case "0.065" -> plan.getAql0065Reject();
            case "0.10" -> plan.getAql010Reject();
            case "0.15" -> plan.getAql015Reject();
            case "0.25" -> plan.getAql025Reject();
            case "0.40" -> plan.getAql040Reject();
            case "0.65" -> plan.getAql065Reject();
            case "1.0" -> plan.getAql10Reject();
            case "1.5" -> plan.getAql15Reject();
            case "2.5" -> plan.getAql25Reject();
            case "4.0" -> plan.getAql40Reject();
            case "6.5" -> plan.getAql65Reject();
            default -> null;
        };
    }
}
