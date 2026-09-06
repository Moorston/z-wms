package com.xwms.core.qc.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import com.xwms.core.qc.dto.QcCreateRequest;
import com.xwms.core.qc.entity.*;
import com.xwms.core.qc.enums.QcOrderStatus;
import com.xwms.core.qc.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 质检流程服务 实现收货前/后质检与入库流程的完整集成
 *
 * <p>质检时机控制（QC_RCV_CTL参数）： - Y: 收货前质检（IQC来料检验），质检合格后才允许收货 - C: 收货后质检（上架前质检），质检合格后才允许上架 - N: 不质检
 *
 * <p>质检类型： - FULL: 全检 - SAMPLE: 抽检（AQL抽样标准） - NONE: 免检
 *
 * <p>不合格品处理方式： - RETURN: 退货（退回供应商） - REWORK: 返工（重新加工） - SCRAP: 报废（销毁） - DOWNGRADE: 降级（降等使用） -
 * CONCESSION: 让步接收（特采）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QcProcessService {

    private final QcTaskMapper qcTaskMapper;
    private final QcSampleMapper qcSampleMapper;
    private final QcOrderMapper qcOrderMapper;
    private final QcRuleMapper qcRuleMapper;
    private final QcUnqualifiedMapper unqualifiedMapper;
    private final AsnMapper asnMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final InboundDetailMapper inboundDetailMapper;
    private final QcOrderService qcOrderService;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 收货前质检（IQC - 来料检验）
    // ============================================================

    /** 创建收货前质检任务 ASN到货后，根据质检规则自动创建收货前质检任务 质检合格后才允许收货 */
    @Transactional(rollbackFor = Exception.class)
    public List<QcTask> createBeforeReceiveQcTasks(String asnNo, String operator) {
        log.info("创建收货前质检任务: asnNo={}", asnNo);

        Asn asn = asnMapper.selectByAsnNo(asnNo);
        if (asn == null) throw new RuntimeException("ASN不存在: " + asnNo);

        // 获取入库单明细
        List<InboundDetail> details = inboundDetailMapper.selectByInboundNo(asn.getInboundNo());
        List<QcTask> tasks = new ArrayList<>();

        for (InboundDetail detail : details) {
            // 匹配质检规则
            QcRule rule = matchQcRule(detail.getSkuCode(), asn.getSupplierCode());
            String qcType = rule != null ? rule.getQcType() : "SAMPLE";

            // 免检跳过
            if ("NONE".equals(qcType)) {
                log.info("商品{}配置免检, 跳过收货前质检", detail.getSkuCode());
                continue;
            }

            // 创建质检任务
            QcTask task = createQcTask(asn, detail, "BEFORE_RECEIVE", qcType, rule, operator);
            task.setBlockReceive("Y"); // 收货前质检阻塞收货
            task.setBlockPutaway("N");
            qcTaskMapper.insert(task);

            // 创建质检单
            QcCreateRequest request = new QcCreateRequest();
            request.setRefType("ASN");
            request.setRefNo(asnNo);
            request.setRefItemId(detail.getId());
            request.setSku(detail.getSkuCode());
            request.setBarcode(detail.getBarcode());
            request.setProductName(detail.getSkuName());
            request.setSupplierCode(asn.getSupplierCode());
            request.setOwnerCode(asn.getOwnerCode());
            request.setWarehouseCode(asn.getWarehouseCode());
            request.setLocationCode(asn.getReceiveLocation());
            request.setBatchNo(detail.getBatchNo());
            request.setLotQty(detail.getExpectedQty());
            QcOrder qcOrder = qcOrderService.createQcOrder(request);

            if (qcOrder != null) {
                task.setQcNo(qcOrder.getQcNo());
                qcTaskMapper.updateById(task);
            }

            tasks.add(task);
        }

        log.info("创建收货前质检任务完成: asnNo={}, 任务数={}", asnNo, tasks.size());
        return tasks;
    }

    /** 收货前质检完成 质检合格后解除收货阻塞，允许收货 */
    @Transactional(rollbackFor = Exception.class)
    public QcTask completeBeforeReceiveQc(
            String taskNo, String qcResult, String inspector, String remark) {
        log.info("收货前质检完成: taskNo={}, result={}", taskNo, qcResult);

        QcTask task = qcTaskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("质检任务不存在: " + taskNo);

        task.setQcResult(qcResult);
        task.setInspector(inspector);
        task.setFinishTime(LocalDateTime.now());
        task.setRemark(remark);

        if ("PASSED".equals(qcResult)) {
            task.setStatus("PASSED");
            task.setBlockReceive("N"); // 合格后解除收货阻塞
        } else if ("FAILED".equals(qcResult)) {
            task.setStatus("FAILED");
            task.setBlockReceive("Y"); // 不合格继续阻塞收货
            // 创建不合格品记录
            createUnqualifiedRecord(task, "BEFORE_RECEIVE");
        } else if ("CONCESSION".equals(qcResult)) {
            task.setStatus("CONCESSION");
            task.setBlockReceive("N"); // 让步接收后解除收货阻塞
        }

        qcTaskMapper.updateById(task);

        // 更新质检单状态
        if (task.getQcNo() != null) {
            QcOrder qcOrder = qcOrderMapper.selectByQcNo(task.getQcNo());
            if (qcOrder != null) {
                qcOrder.setResult(qcResult);
                qcOrder.setStatus(
                        "PASSED".equals(qcResult)
                                ? QcOrderStatus.PASSED.getCode()
                                : "FAILED".equals(qcResult)
                                        ? QcOrderStatus.FAILED.getCode()
                                        : QcOrderStatus.CONCESSION_APPROVED.getCode());
                qcOrder.setFinishTime(LocalDateTime.now());
                qcOrderMapper.updateById(qcOrder);
            }
        }

        log.info(
                "收货前质检完成: taskNo={}, result={}, blockReceive={}",
                taskNo,
                qcResult,
                task.getBlockReceive());
        return task;
    }

    // ============================================================

    // 2. 收货后质检（上架前质检）
    // ============================================================

    /** 创建收货后质检任务 收货完成后，根据质检规则自动创建收货后质检任务 质检合格后才允许上架 */
    @Transactional(rollbackFor = Exception.class)
    public List<QcTask> createAfterReceiveQcTasks(
            String inboundNo, String receiptTaskNo, String operator) {
        log.info("创建收货后质检任务: inboundNo={}, receiptTaskNo={}", inboundNo, receiptTaskNo);

        InboundOrder inboundOrder = inboundOrderMapper.selectByInboundNo(inboundNo);
        if (inboundOrder == null) throw new RuntimeException("入库单不存在: " + inboundNo);

        List<InboundDetail> details = inboundDetailMapper.selectByInboundNo(inboundNo);
        List<QcTask> tasks = new ArrayList<>();

        for (InboundDetail detail : details) {
            if (detail.getReceivedQty() == null
                    || detail.getReceivedQty().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // 匹配质检规则
            QcRule rule = matchQcRule(detail.getSkuCode(), inboundOrder.getSupplierCode());
            String qcType = rule != null ? rule.getQcType() : "SAMPLE";

            // 免检跳过
            if ("NONE".equals(qcType)) {
                log.info("商品{}配置免检, 跳过收货后质检", detail.getSkuCode());
                continue;
            }

            // 创建质检任务
            Asn asn =
                    inboundOrder.getAsnNo() != null
                            ? asnMapper.selectByAsnNo(inboundOrder.getAsnNo())
                            : null;
            QcTask task = new QcTask();
            task.setTaskNo(generateTaskNo());
            task.setQcTiming("AFTER_RECEIVE");
            task.setQcType(qcType);
            task.setAsnNo(inboundOrder.getAsnNo());
            task.setInboundNo(inboundNo);
            task.setInboundDetailNo(detail.getDetailNo());
            task.setReceiptTaskNo(receiptTaskNo);
            task.setSkuCode(detail.getSkuCode());
            task.setSkuName(detail.getSkuName());
            task.setBarcode(detail.getBarcode());
            task.setSupplierCode(inboundOrder.getSupplierCode());
            task.setOwnerCode(inboundOrder.getOwnerCode());
            task.setWarehouseCode(inboundOrder.getWarehouseCode());
            task.setBatchNo(detail.getBatchNo());
            task.setLotQty(detail.getReceivedQty());
            task.setSampleQty(
                    qcType.equals("FULL")
                            ? detail.getReceivedQty()
                            : calculateSampleQty(detail.getReceivedQty(), rule));
            task.setInspectedQty(BigDecimal.ZERO);
            task.setQualifiedQty(BigDecimal.ZERO);
            task.setUnqualifiedQty(BigDecimal.ZERO);
            task.setStatus("PENDING");
            task.setBlockReceive("N");
            task.setBlockPutaway("Y"); // 收货后质检阻塞上架
            task.setCreatedBy(operator);
            qcTaskMapper.insert(task);

            // 创建质检单
            QcCreateRequest request = new QcCreateRequest();
            request.setRefType("INBOUND");
            request.setRefNo(inboundNo);
            request.setRefItemId(detail.getId());
            request.setSku(detail.getSkuCode());
            request.setBarcode(detail.getBarcode());
            request.setProductName(detail.getSkuName());
            request.setSupplierCode(inboundOrder.getSupplierCode());
            request.setOwnerCode(inboundOrder.getOwnerCode());
            request.setWarehouseCode(inboundOrder.getWarehouseCode());
            request.setLocationCode(detail.getReceiveLocation());
            request.setBatchNo(detail.getBatchNo());
            request.setLotQty(detail.getReceivedQty());
            QcOrder qcOrder = qcOrderService.createQcOrder(request);

            if (qcOrder != null) {
                task.setQcNo(qcOrder.getQcNo());
                qcTaskMapper.updateById(task);
            }

            tasks.add(task);
        }

        log.info("创建收货后质检任务完成: inboundNo={}, 任务数={}", inboundNo, tasks.size());
        return tasks;
    }

    /** 收货后质检完成 质检合格后解除上架阻塞，允许上架 */
    @Transactional(rollbackFor = Exception.class)
    public QcTask completeAfterReceiveQc(
            String taskNo, String qcResult, String inspector, String remark) {
        log.info("收货后质检完成: taskNo={}, result={}", taskNo, qcResult);

        QcTask task = qcTaskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("质检任务不存在: " + taskNo);

        task.setQcResult(qcResult);
        task.setInspector(inspector);
        task.setFinishTime(LocalDateTime.now());
        task.setRemark(remark);

        if ("PASSED".equals(qcResult)) {
            task.setStatus("PASSED");
            task.setBlockPutaway("N"); // 合格后解除上架阻塞
        } else if ("FAILED".equals(qcResult)) {
            task.setStatus("FAILED");
            task.setBlockPutaway("Y"); // 不合格继续阻塞上架
            createUnqualifiedRecord(task, "AFTER_RECEIVE");
        } else if ("CONCESSION".equals(qcResult)) {
            task.setStatus("CONCESSION");
            task.setBlockPutaway("N"); // 让步接收后解除上架阻塞
        }

        qcTaskMapper.updateById(task);

        // 更新质检单状态
        if (task.getQcNo() != null) {
            QcOrder qcOrder = qcOrderMapper.selectByQcNo(task.getQcNo());
            if (qcOrder != null) {
                qcOrder.setResult(qcResult);
                qcOrder.setStatus(
                        "PASSED".equals(qcResult)
                                ? QcOrderStatus.PASSED.getCode()
                                : "FAILED".equals(qcResult)
                                        ? QcOrderStatus.FAILED.getCode()
                                        : QcOrderStatus.CONCESSION_APPROVED.getCode());
                qcOrder.setFinishTime(LocalDateTime.now());
                qcOrderMapper.updateById(qcOrder);
            }
        }

        log.info(
                "收货后质检完成: taskNo={}, result={}, blockPutaway={}",
                taskNo,
                qcResult,
                task.getBlockPutaway());
        return task;
    }

    // ============================================================

    // 3. 质检权限校验
    // ============================================================

    /** 校验是否允许收货 收货前质检未完成或不合格时不允许收货 */
    public boolean canReceive(String asnNo) {
        List<QcTask> tasks = qcTaskMapper.selectByAsnNo(asnNo);
        if (tasks == null || tasks.isEmpty()) return true; // 无质检任务，允许收货

        for (QcTask task : tasks) {
            if ("BEFORE_RECEIVE".equals(task.getQcTiming()) && "Y".equals(task.getBlockReceive())) {
                log.warn("收货前质检未完成或不合格，不允许收货: asnNo={}, taskNo={}", asnNo, task.getTaskNo());
                return false;
            }
        }
        return true;
    }

    /** 校验是否允许上架 收货后质检未完成或不合格时不允许上架 */
    public boolean canPutaway(String inboundNo) {
        List<QcTask> tasks = qcTaskMapper.selectByInboundNo(inboundNo);
        if (tasks == null || tasks.isEmpty()) return true; // 无质检任务，允许上架

        for (QcTask task : tasks) {
            if ("AFTER_RECEIVE".equals(task.getQcTiming()) && "Y".equals(task.getBlockPutaway())) {
                log.warn(
                        "收货后质检未完成或不合格，不允许上架: inboundNo={}, taskNo={}", inboundNo, task.getTaskNo());
                return false;
            }
        }
        return true;
    }

    // ============================================================

    // 4. 不合格品处理
    // ============================================================

    /** 不合格品处理 处理方式：RETURN退货/REWORK返工/SCRAP报废/DOWNGRADE降级 */
    @Transactional(rollbackFor = Exception.class)
    public QcUnqualified handleUnqualified(
            Long unqualifiedId,
            String handleMethod,
            BigDecimal handleQty,
            String handler,
            String cert,
            String remark) {
        log.info("不合格品处理: id={}, method={}, qty={}", unqualifiedId, handleMethod, handleQty);

        QcUnqualified unq = unqualifiedMapper.selectById(unqualifiedId);
        if (unq == null) throw new RuntimeException("不合格品记录不存在: " + unqualifiedId);

        unq.setHandleMethod(handleMethod);
        unq.setHandleQty(handleQty);
        unq.setHandleStatus("COMPLETED");
        unq.setHandler(handler);
        unq.setHandleTime(LocalDateTime.now());
        unq.setDisposeCert(cert);
        unq.setRemark(remark);
        unqualifiedMapper.updateById(unq);

        // 更新质检任务状态
        if (unq.getQcId() != null) {
            QcOrder qcOrder = qcOrderMapper.selectById(unq.getQcId());
            if (qcOrder != null) {
                qcOrder.setStatus(QcOrderStatus.DISPOSED.getCode());
                qcOrderMapper.updateById(qcOrder);
            }
        }

        log.info("不合格品处理完成: id={}, method={}", unqualifiedId, handleMethod);
        return unq;
    }

    private void createUnqualifiedRecord(QcTask task, String qcTiming) {
        QcUnqualified unq = new QcUnqualified();
        unq.setQcId(task.getId());
        unq.setSku(task.getSkuCode());
        unq.setBarcode(task.getBarcode());
        unq.setBatchNo(task.getBatchNo());
        unq.setQty(task.getUnqualifiedQty() != null ? task.getUnqualifiedQty() : task.getLotQty());
        unq.setUnqualifiedType("QC_" + qcTiming);
        unq.setDefectDesc(task.getRemark());
        unq.setHandleStatus("PENDING");
        unqualifiedMapper.insert(unq);
    }

    // ============================================================

    // 5. 质检样本管理
    // ============================================================

    /** 抽取质检样本 */
    @Transactional(rollbackFor = Exception.class)
    public List<QcSample> drawSamples(String taskNo, int sampleCount, String drawer) {
        log.info("抽取质检样本: taskNo={}, count={}", taskNo, sampleCount);

        QcTask task = qcTaskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("质检任务不存在: " + taskNo);

        List<QcSample> samples = new ArrayList<>();
        for (int i = 0; i < sampleCount; i++) {
            QcSample sample = new QcSample();
            sample.setSampleNo(generateSampleNo());
            sample.setTaskNo(taskNo);
            sample.setQcNo(task.getQcNo());
            sample.setSkuCode(task.getSkuCode());
            sample.setSkuName(task.getSkuName());
            sample.setBatchNo(task.getBatchNo());
            sample.setSampleQty(BigDecimal.ONE);
            sample.setStatus("DRAWN");
            sample.setDrawnBy(drawer);
            sample.setDrawnTime(LocalDateTime.now());
            sample.setCreatedBy(drawer);
            qcSampleMapper.insert(sample);
            samples.add(sample);
        }

        // 更新任务状态
        task.setStatus("INSPECTING");
        task.setStartTime(LocalDateTime.now());
        qcTaskMapper.updateById(task);

        log.info("抽取质检样本完成: taskNo={}, count={}", taskNo, samples.size());
        return samples;
    }

    /** 录入样本检测结果 */
    @Transactional(rollbackFor = Exception.class)
    public QcSample submitSampleResult(
            String sampleNo,
            String testValue,
            String isQualified,
            String defectType,
            String defectDesc,
            String tester) {
        log.info("录入样本检测结果: sampleNo={}, qualified={}", sampleNo, isQualified);

        QcSample sample = qcSampleMapper.selectBySampleNo(sampleNo);
        if (sample == null) throw new RuntimeException("质检样本不存在: " + sampleNo);

        sample.setTestValue(testValue);
        sample.setIsQualified(isQualified);
        sample.setDefectType(defectType);
        sample.setDefectDesc(defectDesc);
        sample.setStatus("Y".equals(isQualified) ? "PASSED" : "FAILED");
        sample.setTestedBy(tester);
        sample.setTestedTime(LocalDateTime.now());
        qcSampleMapper.updateById(sample);

        // 汇总任务检测结果
        updateTaskInspectionResult(sample.getTaskNo());

        log.info("录入样本检测结果完成: sampleNo={}, qualified={}", sampleNo, isQualified);
        return sample;
    }

    /** 归还质检样本 */
    @Transactional(rollbackFor = Exception.class)
    public QcSample returnSample(String sampleNo, String returner) {
        QcSample sample = qcSampleMapper.selectBySampleNo(sampleNo);
        if (sample == null) throw new RuntimeException("质检样本不存在: " + sampleNo);

        sample.setStatus("RETURNED");
        sample.setReturnedBy(returner);
        sample.setReturnedTime(LocalDateTime.now());
        qcSampleMapper.updateById(sample);

        return sample;
    }

    private void updateTaskInspectionResult(String taskNo) {
        List<QcSample> samples = qcSampleMapper.selectByTaskNo(taskNo);
        if (samples == null || samples.isEmpty()) return;

        QcTask task = qcTaskMapper.selectByTaskNo(taskNo);
        if (task == null) return;

        BigDecimal inspectedQty = new BigDecimal(samples.size());
        long qualifiedCount = samples.stream().filter(s -> "Y".equals(s.getIsQualified())).count();
        BigDecimal qualifiedQty = new BigDecimal(qualifiedCount);
        BigDecimal unqualifiedQty = inspectedQty.subtract(qualifiedQty);

        task.setInspectedQty(inspectedQty);
        task.setQualifiedQty(qualifiedQty);
        task.setUnqualifiedQty(unqualifiedQty);
        qcTaskMapper.updateById(task);
    }

    // ============================================================

    // 6. 查询
    // ============================================================

    public QcTask getQcTaskByNo(String taskNo) {
        return qcTaskMapper.selectByTaskNo(taskNo);
    }

    public List<QcTask> getQcTasksByAsn(String asnNo) {
        return qcTaskMapper.selectByAsnNo(asnNo);
    }

    public List<QcTask> getQcTasksByInbound(String inboundNo) {
        return qcTaskMapper.selectByInboundNo(inboundNo);
    }

    public Page<QcTask> pageQcTasks(
            Page<QcTask> page,
            String status,
            String qcTiming,
            String warehouseCode,
            String asnNo,
            String inboundNo) {
        LambdaQueryWrapper<QcTask> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(QcTask::getStatus, status);
        if (qcTiming != null) wrapper.eq(QcTask::getQcTiming, qcTiming);
        if (warehouseCode != null) wrapper.eq(QcTask::getWarehouseCode, warehouseCode);
        if (asnNo != null) wrapper.eq(QcTask::getAsnNo, asnNo);
        if (inboundNo != null) wrapper.eq(QcTask::getInboundNo, inboundNo);
        wrapper.orderByDesc(QcTask::getCreatedTime);
        return qcTaskMapper.selectPage(page, wrapper);
    }

    public List<QcSample> getSamplesByTask(String taskNo) {
        return qcSampleMapper.selectByTaskNo(taskNo);
    }

    // ============================================================

    // 7. 公共方法
    // ============================================================

    private QcTask createQcTask(
            Asn asn,
            InboundDetail detail,
            String qcTiming,
            String qcType,
            QcRule rule,
            String operator) {
        QcTask task = new QcTask();
        task.setTaskNo(generateTaskNo());
        task.setQcTiming(qcTiming);
        task.setQcType(qcType);
        task.setAsnNo(asn.getAsnNo());
        task.setInboundNo(asn.getInboundNo());
        task.setInboundDetailNo(detail.getDetailNo());
        task.setSkuCode(detail.getSkuCode());
        task.setSkuName(detail.getSkuName());
        task.setBarcode(detail.getBarcode());
        task.setSupplierCode(asn.getSupplierCode());
        task.setOwnerCode(asn.getOwnerCode());
        task.setWarehouseCode(asn.getWarehouseCode());
        task.setBatchNo(detail.getBatchNo());
        task.setLotQty(detail.getExpectedQty());
        task.setSampleQty(
                qcType.equals("FULL")
                        ? detail.getExpectedQty()
                        : calculateSampleQty(detail.getExpectedQty(), rule));
        task.setInspectedQty(BigDecimal.ZERO);
        task.setQualifiedQty(BigDecimal.ZERO);
        task.setUnqualifiedQty(BigDecimal.ZERO);
        task.setStatus("PENDING");
        task.setCreatedBy(operator);
        return task;
    }

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

    private BigDecimal calculateSampleQty(BigDecimal lotQty, QcRule rule) {
        if (rule == null) return lotQty.min(new BigDecimal(20)); // 默认抽检20或全检
        // 简化计算，实际应根据AQL抽样表计算
        String inspectionLevel =
                rule.getInspectionLevel() != null ? rule.getInspectionLevel() : "II";
        int sampleSize =
                switch (inspectionLevel) {
                    case "I" -> Math.min(lotQty.intValue(), 8);
                    case "II" -> Math.min(lotQty.intValue(), 20);
                    case "III" -> Math.min(lotQty.intValue(), 32);
                    default -> Math.min(lotQty.intValue(), 20);
                };
        return new BigDecimal(sampleSize);
    }

    private String generateTaskNo() {
        return "QCT"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateSampleNo() {
        return "QCS"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
