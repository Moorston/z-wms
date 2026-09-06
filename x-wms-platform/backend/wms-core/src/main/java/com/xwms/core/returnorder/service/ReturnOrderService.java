package com.xwms.core.returnorder.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.xwms.core.returnorder.entity.*;
import com.xwms.core.returnorder.enums.ReturnStatus;
import com.xwms.core.returnorder.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 退货入库服务 支持退货单管理、ASN编组、播种初分、二分、动态播种 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnOrderService {

    private final ReturnOrderMapper returnOrderMapper;
    private final ReturnOrderDetailMapper returnOrderDetailMapper;
    private final AsnGroupMapper asnGroupMapper;
    private final AsnGroupDetailMapper asnGroupDetailMapper;
    private final SowingTaskMapper sowingTaskMapper;

    // ==================== 退货单管理 ====================

    /** 创建退货单 */
    @Transactional(rollbackFor = Exception.class)
    public ReturnOrder createReturnOrder(ReturnOrder order, List<ReturnOrderDetail> details) {
        log.info("创建退货单: type={}, customer={}", order.getReturnType(), order.getCustomerCode());

        // 生成退货单号
        String returnNo = generateReturnNo();
        order.setReturnNo(returnNo);
        order.setStatus(ReturnStatus.CREATED.getCode());
        order.setReceivedQty(BigDecimal.ZERO);
        order.setPutawayQty(BigDecimal.ZERO);
        order.setQualifiedQty(BigDecimal.ZERO);
        order.setUnqualifiedQty(BigDecimal.ZERO);
        order.setCreatedTime(LocalDateTime.now());
        returnOrderMapper.insert(order);

        // 保存明细
        int lineNo = 1;
        for (ReturnOrderDetail detail : details) {
            detail.setReturnNo(returnNo);
            detail.setLineNo(lineNo++);
            detail.setReceivedQty(BigDecimal.ZERO);
            detail.setPutawayQty(BigDecimal.ZERO);
            detail.setQualifiedQty(BigDecimal.ZERO);
            detail.setUnqualifiedQty(BigDecimal.ZERO);
            detail.setStatus("PENDING");
            detail.setCreatedTime(LocalDateTime.now());
        }
        returnOrderDetailMapper.batchInsert(details);

        log.info("退货单创建完成: returnNo={}, 明细数={}", returnNo, details.size());
        return order;
    }

    /** 退货收货 */
    @Transactional(rollbackFor = Exception.class)
    public ReturnOrder receiveReturn(
            String returnNo, Map<String, BigDecimal> receiveQtys, String receiver) {
        log.info("退货收货: returnNo={}", returnNo);

        ReturnOrder order = returnOrderMapper.selectByReturnNo(returnNo);
        if (order == null) {
            throw new RuntimeException("退货单不存在: " + returnNo);
        }
        if (!ReturnStatus.CREATED.getCode().equals(order.getStatus())
                && !ReturnStatus.RECEIVING.getCode().equals(order.getStatus())) {
            throw new RuntimeException("退货单状态不允许收货: " + order.getStatus());
        }

        // 更新明细收货数量
        BigDecimal totalReceived = BigDecimal.ZERO;
        List<ReturnOrderDetail> details = returnOrderDetailMapper.selectByReturnNo(returnNo);
        for (ReturnOrderDetail detail : details) {
            BigDecimal qty = receiveQtys.get(detail.getSkuCode());
            if (qty != null && qty.compareTo(BigDecimal.ZERO) > 0) {
                detail.setReceivedQty(qty);
                detail.setStatus("RECEIVED");
                detail.setUpdatedTime(LocalDateTime.now());
                returnOrderDetailMapper.updateById(detail);
                totalReceived = totalReceived.add(qty);
            }
        }

        // 更新退货单
        order.setReceivedQty(totalReceived);
        order.setActualArrivalTime(LocalDateTime.now());
        if (totalReceived.compareTo(order.getTotalQty()) >= 0) {
            order.setStatus(ReturnStatus.RECEIVED.getCode());
            order.setReceiveCompleteTime(LocalDateTime.now());
        } else {
            order.setStatus(ReturnStatus.RECEIVING.getCode());
        }
        order.setUpdatedTime(LocalDateTime.now());
        returnOrderMapper.updateById(order);

        log.info("退货收货完成: returnNo={}, 收货数量={}", returnNo, totalReceived);
        return order;
    }

    /** 取消退货单 */
    @Transactional(rollbackFor = Exception.class)
    public void cancelReturn(String returnNo, String operator) {
        log.info("取消退货单: returnNo={}", returnNo);
        ReturnOrder order = returnOrderMapper.selectByReturnNo(returnNo);
        if (order == null) {
            throw new RuntimeException("退货单不存在: " + returnNo);
        }
        if (ReturnStatus.RECEIVED.getCode().equals(order.getStatus())
                || ReturnStatus.COMPLETED.getCode().equals(order.getStatus())) {
            throw new RuntimeException("已收货的退货单不允许取消");
        }
        order.setStatus(ReturnStatus.CANCELLED.getCode());
        order.setUpdatedTime(LocalDateTime.now());
        returnOrderMapper.updateById(order);
        log.info("退货单已取消: returnNo={}", returnNo);
    }

    // ==================== ASN编组 ====================

    /** 创建ASN编组（多张退货ASN编组成一组） */
    @Transactional(rollbackFor = Exception.class)
    public AsnGroup createAsnGroup(
            List<String> returnNos, String groupName, String sowingMode, String creator) {
        log.info("创建ASN编组: 退货单数={}, 播种模式={}", returnNos.size(), sowingMode);

        // 统计SKU重合度
        Map<String, Integer> skuAsnCount = new HashMap<>();
        List<AsnGroupDetail> groupDetails = new ArrayList<>();
        BigDecimal totalQty = BigDecimal.ZERO;
        Set<String> allSkus = new HashSet<>();

        for (String returnNo : returnNos) {
            ReturnOrder order = returnOrderMapper.selectByReturnNo(returnNo);
            if (order == null) {
                throw new RuntimeException("退货单不存在: " + returnNo);
            }
            List<ReturnOrderDetail> details = returnOrderDetailMapper.selectByReturnNo(returnNo);
            for (ReturnOrderDetail detail : details) {
                allSkus.add(detail.getSkuCode());
                skuAsnCount.merge(detail.getSkuCode(), 1, Integer::sum);
                totalQty = totalQty.add(detail.getReturnQty());

                AsnGroupDetail gd = new AsnGroupDetail();
                gd.setAsnNo(order.getAsnNo());
                gd.setReturnNo(returnNo);
                gd.setSkuCode(detail.getSkuCode());
                gd.setSkuName(detail.getSkuName());
                gd.setBatchNo(detail.getBatchNo());
                gd.setQty(detail.getReturnQty());
                gd.setSowedQty(BigDecimal.ZERO);
                gd.setStatus("PENDING");
                gd.setCreatedTime(LocalDateTime.now());
                groupDetails.add(gd);
            }
        }

        // 计算SKU重合度（出现在多个ASN中的SKU占比）
        int overlapCount = 0;
        for (Map.Entry<String, Integer> entry : skuAsnCount.entrySet()) {
            if (entry.getValue() > 1) {
                overlapCount++;
            }
        }
        BigDecimal overlapRate =
                allSkus.isEmpty()
                        ? BigDecimal.ZERO
                        : new BigDecimal(overlapCount)
                                .divide(new BigDecimal(allSkus.size()), 4, BigDecimal.ROUND_HALF_UP)
                                .multiply(new BigDecimal(100));

        // 创建编组
        String groupNo = generateGroupNo();
        AsnGroup group = new AsnGroup();
        group.setGroupNo(groupNo);
        group.setGroupName(groupName);
        group.setGroupType("RETURN");
        group.setAsnCount(returnNos.size());
        group.setSkuCount(allSkus.size());
        group.setTotalQty(totalQty);
        group.setSkuOverlapRate(overlapRate);
        group.setStatus("CREATED");
        group.setSowingMode(sowingMode);
        group.setSowingLocationCount(0);
        group.setOperator(creator);
        group.setCreatedBy(creator);
        group.setCreatedTime(LocalDateTime.now());
        asnGroupMapper.insert(group);

        // 保存编组明细
        for (AsnGroupDetail gd : groupDetails) {
            gd.setGroupNo(groupNo);
        }
        asnGroupDetailMapper.batchInsert(groupDetails);

        // 更新退货单的编组号
        for (String returnNo : returnNos) {
            ReturnOrder order = returnOrderMapper.selectByReturnNo(returnNo);
            if (order != null) {
                order.setGroupNo(groupNo);
                returnOrderMapper.updateById(order);
            }
        }

        log.info("ASN编组创建完成: groupNo={}, SKU重合度={}%", groupNo, overlapRate);
        return group;
    }

    // ==================== 播种初分 ====================

    /** 生成播种初分任务 按SKU汇总，每个SKU生成一个播种任务 */
    @Transactional(rollbackFor = Exception.class)
    public List<SowingTask> generateFirstSowingTasks(String groupNo, String operator) {
        log.info("生成播种初分任务: groupNo={}", groupNo);

        AsnGroup group = asnGroupMapper.selectByGroupNo(groupNo);
        if (group == null) {
            throw new RuntimeException("编组不存在: " + groupNo);
        }

        List<AsnGroupDetail> details = asnGroupDetailMapper.selectByGroupNo(groupNo);

        // 按SKU汇总
        Map<String, List<AsnGroupDetail>> skuGroup =
                details.stream().collect(Collectors.groupingBy(AsnGroupDetail::getSkuCode));

        List<SowingTask> tasks = new ArrayList<>();
        int seq = 1;
        for (Map.Entry<String, List<AsnGroupDetail>> entry : skuGroup.entrySet()) {
            String skuCode = entry.getKey();
            List<AsnGroupDetail> skuDetails = entry.getValue();
            BigDecimal totalQty =
                    skuDetails.stream()
                            .map(AsnGroupDetail::getQty)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            SowingTask task = new SowingTask();
            task.setTaskNo(generateSowingTaskNo());
            task.setGroupNo(groupNo);
            task.setSowingStage("FIRST");
            task.setSkuCode(skuCode);
            task.setSkuName(skuDetails.get(0).getSkuName());
            task.setTotalQty(totalQty);
            task.setSowedQty(BigDecimal.ZERO);
            task.setRemainingQty(totalQty);
            task.setStatus("PENDING");
            task.setSowingMode(group.getSowingMode());
            task.setSowingSeq(seq++);
            task.setOperator(operator);
            task.setCreatedBy(operator);
            task.setCreatedTime(LocalDateTime.now());
            tasks.add(task);
        }

        sowingTaskMapper.batchInsert(tasks);

        // 更新编组状态
        group.setStatus("SEEDING");
        group.setSowingStartTime(LocalDateTime.now());
        asnGroupMapper.updateById(group);

        log.info("播种初分任务生成完成: groupNo={}, 任务数={}", groupNo, tasks.size());
        return tasks;
    }

    /** 执行播种初分（扫描SKU，系统提示分货去向） */
    @Transactional(rollbackFor = Exception.class)
    public SowingTask executeFirstSowing(
            String taskNo, String sowingLocation, BigDecimal sowedQty, String operator) {
        log.info("执行播种初分: taskNo={}, 播种位={}, 数量={}", taskNo, sowingLocation, sowedQty);

        SowingTask task = sowingTaskMapper.selectByTaskNo(taskNo);
        if (task == null) {
            throw new RuntimeException("播种任务不存在: " + taskNo);
        }
        if (!"PENDING".equals(task.getStatus()) && !"SEEDING".equals(task.getStatus())) {
            throw new RuntimeException("播种任务状态不允许操作: " + task.getStatus());
        }

        // 更新任务
        task.setSowingLocation(sowingLocation);
        task.setSowedQty(task.getSowedQty().add(sowedQty));
        task.setRemainingQty(task.getTotalQty().subtract(task.getSowedQty()));
        task.setStatus("SEEDING");
        if (task.getRemainingQty().compareTo(BigDecimal.ZERO) <= 0) {
            task.setStatus("SEEDED");
            task.setFinishTime(LocalDateTime.now());
        }
        task.setUpdatedTime(LocalDateTime.now());
        sowingTaskMapper.updateById(task);

        log.info(
                "播种初分执行完成: taskNo={}, 已播种={}, 剩余={}",
                taskNo,
                task.getSowedQty(),
                task.getRemainingQty());
        return task;
    }

    // ==================== 播种二分 ====================

    /** 生成播种二分任务 初分完成后，按SKU+ASN细分，生成二分任务 */
    @Transactional(rollbackFor = Exception.class)
    public List<SowingTask> generateSecondSowingTasks(String groupNo, String operator) {
        log.info("生成播种二分任务: groupNo={}", groupNo);

        AsnGroup group = asnGroupMapper.selectByGroupNo(groupNo);
        if (group == null) {
            throw new RuntimeException("编组不存在: " + groupNo);
        }

        // 检查初分是否完成
        List<SowingTask> firstTasks = sowingTaskMapper.selectByGroupNoAndStage(groupNo, "FIRST");
        boolean allFirstDone =
                firstTasks.stream()
                        .allMatch(
                                t ->
                                        "SEEDED".equals(t.getStatus())
                                                || "COMPLETED".equals(t.getStatus()));
        if (!allFirstDone) {
            throw new RuntimeException("初分任务未全部完成，无法生成二分任务");
        }

        List<AsnGroupDetail> details = asnGroupDetailMapper.selectByGroupNo(groupNo);

        // 按SKU+ASN细分
        List<SowingTask> tasks = new ArrayList<>();
        int seq = 1;
        for (AsnGroupDetail detail : details) {
            SowingTask task = new SowingTask();
            task.setTaskNo(generateSowingTaskNo());
            task.setGroupNo(groupNo);
            task.setSowingStage("SECOND");
            task.setSkuCode(detail.getSkuCode());
            task.setSkuName(detail.getSkuName());
            task.setBatchNo(detail.getBatchNo());
            task.setTotalQty(detail.getQty());
            task.setSowedQty(BigDecimal.ZERO);
            task.setRemainingQty(detail.getQty());
            task.setStatus("PENDING");
            task.setSowingMode(group.getSowingMode());
            task.setSowingSeq(seq++);
            task.setOperator(operator);
            task.setCreatedBy(operator);
            task.setCreatedTime(LocalDateTime.now());
            tasks.add(task);
        }

        sowingTaskMapper.batchInsert(tasks);
        log.info("播种二分任务生成完成: groupNo={}, 任务数={}", groupNo, tasks.size());
        return tasks;
    }

    /** 执行播种二分 */
    @Transactional(rollbackFor = Exception.class)
    public SowingTask executeSecondSowing(
            String taskNo, String targetLocation, BigDecimal sowedQty, String operator) {
        log.info("执行播种二分: taskNo={}, 目标库位={}, 数量={}", taskNo, targetLocation, sowedQty);

        SowingTask task = sowingTaskMapper.selectByTaskNo(taskNo);
        if (task == null) {
            throw new RuntimeException("播种任务不存在: " + taskNo);
        }

        task.setTargetLocation(targetLocation);
        task.setSowedQty(task.getSowedQty().add(sowedQty));
        task.setRemainingQty(task.getTotalQty().subtract(task.getSowedQty()));
        task.setStatus("SEEDING");
        if (task.getRemainingQty().compareTo(BigDecimal.ZERO) <= 0) {
            task.setStatus("COMPLETED");
            task.setFinishTime(LocalDateTime.now());
        }
        task.setUpdatedTime(LocalDateTime.now());
        sowingTaskMapper.updateById(task);

        // 检查编组是否全部完成
        checkGroupComplete(task.getGroupNo());

        return task;
    }

    // ==================== 动态播种 ====================

    /** 动态播种（扫描产品时临时按次序绑定空播种位） */
    @Transactional(rollbackFor = Exception.class)
    public SowingTask dynamicSowing(
            String groupNo,
            String skuCode,
            String sowingLocation,
            BigDecimal qty,
            String operator) {
        log.info("动态播种: groupNo={}, sku={}, 播种位={}", groupNo, skuCode);

        // 查找该SKU的播种任务
        List<SowingTask> tasks = sowingTaskMapper.selectByGroupNoAndStage(groupNo, "FIRST");
        SowingTask task =
                tasks.stream().filter(t -> skuCode.equals(t.getSkuCode())).findFirst().orElse(null);

        if (task == null) {
            throw new RuntimeException("未找到SKU的播种任务: " + skuCode);
        }

        // 动态绑定播种位（如果尚未绑定）
        if (task.getSowingLocation() == null) {
            task.setSowingLocation(sowingLocation);
        }

        return executeFirstSowing(task.getTaskNo(), sowingLocation, qty, operator);
    }

    // ==================== 查询方法 ====================

    /** 查询退货单详情 */
    public ReturnOrder getReturnOrder(String returnNo) {
        return returnOrderMapper.selectByReturnNo(returnNo);
    }

    /** 查询退货单明细 */
    public List<ReturnOrderDetail> getReturnDetails(String returnNo) {
        return returnOrderDetailMapper.selectByReturnNo(returnNo);
    }

    /** 查询ASN编组详情 */
    public AsnGroup getAsnGroup(String groupNo) {
        return asnGroupMapper.selectByGroupNo(groupNo);
    }

    /** 查询ASN编组明细 */
    public List<AsnGroupDetail> getAsnGroupDetails(String groupNo) {
        return asnGroupDetailMapper.selectByGroupNo(groupNo);
    }

    /** 查询播种任务 */
    public List<SowingTask> getSowingTasks(String groupNo) {
        return sowingTaskMapper.selectByGroupNo(groupNo);
    }

    /** 查询所有退货单 */
    public List<ReturnOrder> getAllReturnOrders() {
        return returnOrderMapper.selectList(
                new LambdaQueryWrapper<ReturnOrder>().orderByDesc(ReturnOrder::getCreatedTime));
    }

    // ==================== 工具方法 ====================

    /** 检查编组是否全部完成 */
    private void checkGroupComplete(String groupNo) {
        List<SowingTask> tasks = sowingTaskMapper.selectByGroupNo(groupNo);
        boolean allDone =
                tasks.stream()
                        .allMatch(
                                t ->
                                        "COMPLETED".equals(t.getStatus())
                                                || "SEEDED".equals(t.getStatus()));
        if (allDone) {
            AsnGroup group = asnGroupMapper.selectByGroupNo(groupNo);
            if (group != null) {
                group.setStatus("COMPLETED");
                group.setSowingFinishTime(LocalDateTime.now());
                asnGroupMapper.updateById(group);
                log.info("编组播种全部完成: groupNo={}", groupNo);
            }
        }
    }

    /** 生成退货单号 */
    private String generateReturnNo() {
        return "RT" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }

    /** 生成编组号 */
    private String generateGroupNo() {
        return "GRP" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }

    /** 生成播种任务号 */
    private String generateSowingTaskNo() {
        return "SW" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }
}
