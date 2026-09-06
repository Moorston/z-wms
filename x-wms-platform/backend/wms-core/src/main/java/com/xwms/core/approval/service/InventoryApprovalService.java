package com.xwms.core.approval.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.approval.entity.*;
import com.xwms.core.approval.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库存审批流程管理核心服务 核心能力: 审批流程/审批节点/审批实例/审批记录 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryApprovalService {

    private final ApprovalProcessMapper processMapper;
    private final ApprovalNodeMapper nodeMapper;
    private final ApprovalInstanceMapper instanceMapper;
    private final ApprovalRecordMapper recordMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 审批流程管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ApprovalProcess createProcess(ApprovalProcess process) {
        process.setStatus("ACTIVE");
        if (process.getVersion() == null) process.setVersion(1);
        processMapper.insert(process);
        log.info(
                "创建审批流程: code={}, name={}, type={}, version={}",
                process.getProcessCode(),
                process.getProcessName(),
                process.getProcessType(),
                process.getVersion());
        return process;
    }

    public ApprovalProcess getProcessByCodeAndVersion(String processCode, Integer version) {
        return processMapper.selectByCodeAndVersion(processCode, version);
    }

    public ApprovalProcess getLatestProcessByType(String processType) {
        return processMapper.selectLatestByType(processType);
    }

    public ApprovalProcess getLatestProcessByWarehouseAndType(
            String warehouseCode, String processType) {
        return processMapper.selectLatestByWarehouseAndType(warehouseCode, processType);
    }

    public List<ApprovalProcess> getProcessesByType(String processType) {
        return processMapper.selectByType(processType);
    }

    public Page<ApprovalProcess> pageProcesses(
            Page<ApprovalProcess> page, String processType, String warehouseCode) {
        LambdaQueryWrapper<ApprovalProcess> wrapper = new LambdaQueryWrapper<>();
        if (processType != null) wrapper.eq(ApprovalProcess::getProcessType, processType);
        if (warehouseCode != null) wrapper.eq(ApprovalProcess::getWarehouseCode, warehouseCode);
        wrapper.eq(ApprovalProcess::getStatus, "ACTIVE");
        wrapper.orderByDesc(ApprovalProcess::getCreatedTime);
        return processMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. 审批节点管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ApprovalNode createNode(ApprovalNode node) {
        node.setStatus("ACTIVE");
        nodeMapper.insert(node);
        log.info(
                "创建审批节点: code={}, name={}, process={}, order={}",
                node.getNodeCode(),
                node.getNodeName(),
                node.getProcessCode(),
                node.getNodeOrder());
        return node;
    }

    public List<ApprovalNode> getNodesByProcess(String processCode, Integer version) {
        return nodeMapper.selectByProcess(processCode, version);
    }

    public ApprovalNode getNodeByProcessAndCode(
            String processCode, Integer version, String nodeCode) {
        return nodeMapper.selectByProcessAndNode(processCode, version, nodeCode);
    }

    public ApprovalNode getStartNode(String processCode, Integer version) {
        return nodeMapper.selectStartNode(processCode, version);
    }

    public ApprovalNode getEndNode(String processCode, Integer version) {
        return nodeMapper.selectEndNode(processCode, version);
    }

    // ============================================================

    // 3. 审批实例管理（核心）
    // ============================================================

    /** 提交审批 */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalInstance submitApproval(
            String processType,
            String warehouseCode,
            String ownerCode,
            String bizType,
            String bizNo,
            String bizData,
            String title,
            String submitter) {
        // 获取最新流程
        ApprovalProcess process =
                processMapper.selectLatestByWarehouseAndType(warehouseCode, processType);
        if (process == null) {
            process = processMapper.selectLatestByType(processType);
        }
        if (process == null) {
            throw new RuntimeException("审批流程不存在: type=" + processType);
        }

        // 获取起始节点
        ApprovalNode startNode =
                nodeMapper.selectStartNode(process.getProcessCode(), process.getVersion());
        if (startNode == null) {
            throw new RuntimeException("审批流程起始节点不存在: process=" + process.getProcessCode());
        }

        // 获取下一节点（第一个审批节点）
        ApprovalNode firstApproveNode = null;
        if (startNode.getNextNode() != null) {
            firstApproveNode =
                    nodeMapper.selectByProcessAndNode(
                            process.getProcessCode(),
                            process.getVersion(),
                            startNode.getNextNode());
        }

        // 创建审批实例
        ApprovalInstance instance = new ApprovalInstance();
        instance.setInstanceId(generateInstanceId());
        instance.setProcessCode(process.getProcessCode());
        instance.setProcessVersion(process.getVersion());
        instance.setProcessName(process.getProcessName());
        instance.setProcessType(processType);
        instance.setWarehouseCode(warehouseCode);
        instance.setOwnerCode(ownerCode);
        instance.setBizType(bizType);
        instance.setBizNo(bizNo);
        instance.setBizData(bizData);
        instance.setTitle(title);
        instance.setCurrentNode(
                firstApproveNode != null
                        ? firstApproveNode.getNodeCode()
                        : startNode.getNodeCode());
        instance.setCurrentNodeName(
                firstApproveNode != null
                        ? firstApproveNode.getNodeName()
                        : startNode.getNodeName());
        instance.setStatus("APPROVING");
        instance.setSubmitter(submitter);
        instance.setSubmitTime(LocalDateTime.now());
        instance.setCurrentApprovers(
                firstApproveNode != null ? firstApproveNode.getApproveUsers() : null);
        instance.setApproveCount(0);
        instance.setTotalApprovers(
                firstApproveNode != null ? countApprovers(firstApproveNode.getApproveUsers()) : 0);
        instanceMapper.insert(instance);

        // 记录提交
        ApprovalRecord record = new ApprovalRecord();
        record.setRecordId(generateRecordId());
        record.setInstanceId(instance.getInstanceId());
        record.setNodeCode(startNode.getNodeCode());
        record.setNodeName(startNode.getNodeName());
        record.setNodeOrder(startNode.getNodeOrder());
        record.setApprover(submitter);
        record.setAction("WITHDRAW");
        record.setOpinion("提交审批");
        record.setApproveTime(LocalDateTime.now());
        record.setFromNode(startNode.getNodeCode());
        record.setToNode(firstApproveNode != null ? firstApproveNode.getNodeCode() : null);
        recordMapper.insert(record);

        log.info(
                "提交审批: instanceId={}, process={}, biz={}/{}, submitter={}",
                instance.getInstanceId(),
                process.getProcessCode(),
                bizType,
                bizNo,
                submitter);
        return instance;
    }

    /** 审批操作 */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalInstance approve(
            String instanceId, String approver, String action, String opinion) {
        ApprovalInstance instance = instanceMapper.selectByInstanceId(instanceId);
        if (instance == null) throw new RuntimeException("审批实例不存在: " + instanceId);
        if (!"APPROVING".equals(instance.getStatus()) && !"PENDING".equals(instance.getStatus())) {
            throw new RuntimeException("审批实例状态不正确: " + instance.getStatus());
        }

        // 获取当前节点
        ApprovalNode currentNode =
                nodeMapper.selectByProcessAndNode(
                        instance.getProcessCode(),
                        instance.getProcessVersion(),
                        instance.getCurrentNode());
        if (currentNode == null)
            throw new RuntimeException("审批节点不存在: " + instance.getCurrentNode());

        // 记录审批
        ApprovalRecord record = new ApprovalRecord();
        record.setRecordId(generateRecordId());
        record.setInstanceId(instanceId);
        record.setNodeCode(currentNode.getNodeCode());
        record.setNodeName(currentNode.getNodeName());
        record.setNodeOrder(currentNode.getNodeOrder());
        record.setApprover(approver);
        record.setAction(action);
        record.setOpinion(opinion);
        record.setApproveTime(LocalDateTime.now());
        recordMapper.insert(record);

        if ("REJECT".equals(action)) {
            // 驳回
            String rejectNode =
                    currentNode.getRejectNode() != null ? currentNode.getRejectNode() : "START";
            instanceMapper.completeInstance(
                    instanceId,
                    "REJECTED",
                    System.currentTimeMillis()
                            - instance.getSubmitTime()
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli());
            log.info("审批驳回: instanceId={}, approver={}", instanceId, approver);
            return instanceMapper.selectByInstanceId(instanceId);
        }

        if ("APPROVE".equals(action)) {
            // 检查是否满足审批通过条件
            int approveCount =
                    recordMapper.countApproveByNode(instanceId, currentNode.getNodeCode()) + 1;
            int totalApprovers = countApprovers(currentNode.getApproveUsers());
            boolean nodeApproved = false;

            if ("ANY".equals(currentNode.getApproveType())) {
                nodeApproved = approveCount >= 1;
            } else if ("ALL".equals(currentNode.getApproveType())) {
                nodeApproved = approveCount >= totalApprovers;
            } else if ("MAJORITY".equals(currentNode.getApproveType())) {
                nodeApproved = approveCount > totalApprovers / 2;
            }

            if (nodeApproved) {
                // 进入下一节点
                if (currentNode.getNextNode() == null || "END".equals(currentNode.getNextNode())) {
                    // 审批完成
                    instanceMapper.completeInstance(
                            instanceId,
                            "APPROVED",
                            System.currentTimeMillis()
                                    - instance.getSubmitTime()
                                            .atZone(java.time.ZoneId.systemDefault())
                                            .toInstant()
                                            .toEpochMilli());
                    log.info("审批通过完成: instanceId={}", instanceId);
                } else {
                    ApprovalNode nextNode =
                            nodeMapper.selectByProcessAndNode(
                                    instance.getProcessCode(),
                                    instance.getProcessVersion(),
                                    currentNode.getNextNode());
                    if (nextNode != null) {
                        instanceMapper.updateCurrentNode(
                                instanceId,
                                "APPROVING",
                                nextNode.getNodeCode(),
                                nextNode.getNodeName(),
                                nextNode.getApproveUsers(),
                                0,
                                countApprovers(nextNode.getApproveUsers()));
                        log.info(
                                "审批进入下一节点: instanceId={}, node={}",
                                instanceId,
                                nextNode.getNodeCode());
                    }
                }
            } else {
                // 更新已审批人数
                instanceMapper.updateCurrentNode(
                        instanceId,
                        instance.getStatus(),
                        instance.getCurrentNode(),
                        instance.getCurrentNodeName(),
                        instance.getCurrentApprovers(),
                        approveCount,
                        totalApprovers);
            }
        }

        return instanceMapper.selectByInstanceId(instanceId);
    }

    /** 撤回审批 */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalInstance withdraw(String instanceId, String submitter, String reason) {
        ApprovalInstance instance = instanceMapper.selectByInstanceId(instanceId);
        if (instance == null) throw new RuntimeException("审批实例不存在: " + instanceId);
        if (!instance.getSubmitter().equals(submitter)) {
            throw new RuntimeException("只有提交人可以撤回审批");
        }
        if (!"APPROVING".equals(instance.getStatus()) && !"PENDING".equals(instance.getStatus())) {
            throw new RuntimeException("审批实例状态不正确: " + instance.getStatus());
        }

        instanceMapper.completeInstance(
                instanceId,
                "CANCELLED",
                System.currentTimeMillis()
                        - instance.getSubmitTime()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli());

        ApprovalRecord record = new ApprovalRecord();
        record.setRecordId(generateRecordId());
        record.setInstanceId(instanceId);
        record.setNodeCode(instance.getCurrentNode());
        record.setNodeName(instance.getCurrentNodeName());
        record.setApprover(submitter);
        record.setAction("WITHDRAW");
        record.setOpinion(reason);
        record.setApproveTime(LocalDateTime.now());
        recordMapper.insert(record);

        log.info("撤回审批: instanceId={}, submitter={}", instanceId, submitter);
        return instanceMapper.selectByInstanceId(instanceId);
    }

    private int countApprovers(String approvers) {
        if (approvers == null || approvers.isEmpty()) return 0;
        return approvers.split(",").length;
    }

    public ApprovalInstance getInstanceById(String instanceId) {
        return instanceMapper.selectByInstanceId(instanceId);
    }

    public List<ApprovalInstance> getInstancesByBiz(String bizType, String bizNo) {
        return instanceMapper.selectByBiz(bizType, bizNo);
    }

    public List<ApprovalInstance> getInstancesBySubmitter(String submitter, int limit) {
        return instanceMapper.selectBySubmitter(submitter, limit);
    }

    public List<ApprovalInstance> getTodoByApprover(String approver) {
        return instanceMapper.selectTodoByApprover(approver);
    }

    public int countTodoByApprover(String approver) {
        return instanceMapper.countTodoByApprover(approver);
    }

    public Page<ApprovalInstance> pageInstances(
            Page<ApprovalInstance> page,
            String processType,
            String status,
            String warehouseCode,
            String submitter) {
        LambdaQueryWrapper<ApprovalInstance> wrapper = new LambdaQueryWrapper<>();
        if (processType != null) wrapper.eq(ApprovalInstance::getProcessType, processType);
        if (status != null) wrapper.eq(ApprovalInstance::getStatus, status);
        if (warehouseCode != null) wrapper.eq(ApprovalInstance::getWarehouseCode, warehouseCode);
        if (submitter != null) wrapper.eq(ApprovalInstance::getSubmitter, submitter);
        wrapper.orderByDesc(ApprovalInstance::getCreatedTime);
        return instanceMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 4. 审批记录管理
    // ============================================================

    public List<ApprovalRecord> getRecordsByInstance(String instanceId) {
        return recordMapper.selectByInstanceId(instanceId);
    }

    public List<ApprovalRecord> getRecordsByInstanceAndNode(String instanceId, String nodeCode) {
        return recordMapper.selectByInstanceAndNode(instanceId, nodeCode);
    }

    public List<ApprovalRecord> getRecordsByApprover(String approver, int limit) {
        return recordMapper.selectByApprover(approver, limit);
    }

    public int countApproveByNode(String instanceId, String nodeCode) {
        return recordMapper.countApproveByNode(instanceId, nodeCode);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateInstanceId() {
        return "AI"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateRecordId() {
        return "AR"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
