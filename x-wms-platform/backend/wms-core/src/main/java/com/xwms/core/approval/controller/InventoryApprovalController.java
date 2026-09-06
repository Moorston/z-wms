package com.xwms.core.approval.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.approval.entity.*;
import com.xwms.core.approval.service.InventoryApprovalService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 库存审批流程管理 Controller */
@Tag(name = "库存审批流程管理", description = "审批流程/审批节点/审批实例/审批记录")
@RestController
@RequestMapping("/api/approval")
@RequiredArgsConstructor
public class InventoryApprovalController {

    private final InventoryApprovalService approvalService;

    // ============================================================

    // 审批流程
    // ============================================================

    @Operation(summary = "创建审批流程")
    @PostMapping("/process")
    public Result<ApprovalProcess> createProcess(@RequestBody ApprovalProcess process) {
        return Result.success(approvalService.createProcess(process));
    }

    @Operation(summary = "按编码和版本查询审批流程")
    @GetMapping("/process/{processCode}/{version}")
    public Result<ApprovalProcess> getProcessByCodeAndVersion(
            @PathVariable String processCode, @PathVariable Integer version) {
        return Result.success(approvalService.getProcessByCodeAndVersion(processCode, version));
    }

    @Operation(summary = "按类型查询最新审批流程")
    @GetMapping("/process/latest/{processType}")
    public Result<ApprovalProcess> getLatestProcessByType(@PathVariable String processType) {
        return Result.success(approvalService.getLatestProcessByType(processType));
    }

    @Operation(summary = "按仓库和类型查询最新审批流程")
    @GetMapping("/process/latest-warehouse")
    public Result<ApprovalProcess> getLatestProcessByWarehouseAndType(
            @RequestParam String warehouseCode, @RequestParam String processType) {
        return Result.success(
                approvalService.getLatestProcessByWarehouseAndType(warehouseCode, processType));
    }

    @Operation(summary = "按类型查询审批流程列表")
    @GetMapping("/process/type/{processType}")
    public Result<List<ApprovalProcess>> getProcessesByType(@PathVariable String processType) {
        return Result.success(approvalService.getProcessesByType(processType));
    }

    @Operation(summary = "分页查询审批流程")
    @GetMapping("/process/list")
    public Result<Page<ApprovalProcess>> pageProcesses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String processType,
            @RequestParam(required = false) String warehouseCode) {
        return Result.success(
                approvalService.pageProcesses(new Page<>(page, size), processType, warehouseCode));
    }

    // ============================================================

    // 审批节点
    // ============================================================

    @Operation(summary = "创建审批节点")
    @PostMapping("/node")
    public Result<ApprovalNode> createNode(@RequestBody ApprovalNode node) {
        return Result.success(approvalService.createNode(node));
    }

    @Operation(summary = "按流程查询审批节点列表")
    @GetMapping("/node/process/{processCode}/{version}")
    public Result<List<ApprovalNode>> getNodesByProcess(
            @PathVariable String processCode, @PathVariable Integer version) {
        return Result.success(approvalService.getNodesByProcess(processCode, version));
    }

    @Operation(summary = "按流程和节点编码查询审批节点")
    @GetMapping("/node/{processCode}/{version}/{nodeCode}")
    public Result<ApprovalNode> getNodeByProcessAndCode(
            @PathVariable String processCode,
            @PathVariable Integer version,
            @PathVariable String nodeCode) {
        return Result.success(
                approvalService.getNodeByProcessAndCode(processCode, version, nodeCode));
    }

    @Operation(summary = "查询起始节点")
    @GetMapping("/node/start/{processCode}/{version}")
    public Result<ApprovalNode> getStartNode(
            @PathVariable String processCode, @PathVariable Integer version) {
        return Result.success(approvalService.getStartNode(processCode, version));
    }

    @Operation(summary = "查询结束节点")
    @GetMapping("/node/end/{processCode}/{version}")
    public Result<ApprovalNode> getEndNode(
            @PathVariable String processCode, @PathVariable Integer version) {
        return Result.success(approvalService.getEndNode(processCode, version));
    }

    // ============================================================

    // 审批实例
    // ============================================================

    @Operation(summary = "提交审批")
    @PostMapping("/instance/submit")
    public Result<ApprovalInstance> submitApproval(
            @RequestParam String processType,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String ownerCode,
            @RequestParam String bizType,
            @RequestParam String bizNo,
            @RequestParam(required = false) String bizData,
            @RequestParam String title,
            @RequestParam String submitter) {
        return Result.success(
                approvalService.submitApproval(
                        processType,
                        warehouseCode,
                        ownerCode,
                        bizType,
                        bizNo,
                        bizData,
                        title,
                        submitter));
    }

    @Operation(summary = "审批操作")
    @PostMapping("/instance/{instanceId}/approve")
    public Result<ApprovalInstance> approve(
            @PathVariable String instanceId,
            @RequestParam String approver,
            @RequestParam String action,
            @RequestParam(required = false) String opinion) {
        return Result.success(approvalService.approve(instanceId, approver, action, opinion));
    }

    @Operation(summary = "撤回审批")
    @PostMapping("/instance/{instanceId}/withdraw")
    public Result<ApprovalInstance> withdraw(
            @PathVariable String instanceId,
            @RequestParam String submitter,
            @RequestParam String reason) {
        return Result.success(approvalService.withdraw(instanceId, submitter, reason));
    }

    @Operation(summary = "按ID查询审批实例")
    @GetMapping("/instance/{instanceId}")
    public Result<ApprovalInstance> getInstanceById(@PathVariable String instanceId) {
        return Result.success(approvalService.getInstanceById(instanceId));
    }

    @Operation(summary = "按业务查询审批实例")
    @GetMapping("/instance/biz")
    public Result<List<ApprovalInstance>> getInstancesByBiz(
            @RequestParam String bizType, @RequestParam String bizNo) {
        return Result.success(approvalService.getInstancesByBiz(bizType, bizNo));
    }

    @Operation(summary = "按提交人查询审批实例")
    @GetMapping("/instance/submitter/{submitter}")
    public Result<List<ApprovalInstance>> getInstancesBySubmitter(
            @PathVariable String submitter, @RequestParam(defaultValue = "20") int limit) {
        return Result.success(approvalService.getInstancesBySubmitter(submitter, limit));
    }

    @Operation(summary = "查询我的待办")
    @GetMapping("/instance/todo/{approver}")
    public Result<List<ApprovalInstance>> getTodoByApprover(@PathVariable String approver) {
        return Result.success(approvalService.getTodoByApprover(approver));
    }

    @Operation(summary = "统计我的待办数量")
    @GetMapping("/instance/todo-count/{approver}")
    public Result<Integer> countTodoByApprover(@PathVariable String approver) {
        return Result.success(approvalService.countTodoByApprover(approver));
    }

    @Operation(summary = "分页查询审批实例")
    @GetMapping("/instance/list")
    public Result<Page<ApprovalInstance>> pageInstances(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String processType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String submitter) {
        return Result.success(
                approvalService.pageInstances(
                        new Page<>(page, size), processType, status, warehouseCode, submitter));
    }

    // ============================================================

    // 审批记录
    // ============================================================

    @Operation(summary = "按实例查询审批记录")
    @GetMapping("/record/instance/{instanceId}")
    public Result<List<ApprovalRecord>> getRecordsByInstance(@PathVariable String instanceId) {
        return Result.success(approvalService.getRecordsByInstance(instanceId));
    }

    @Operation(summary = "按实例和节点查询审批记录")
    @GetMapping("/record/instance-node")
    public Result<List<ApprovalRecord>> getRecordsByInstanceAndNode(
            @RequestParam String instanceId, @RequestParam String nodeCode) {
        return Result.success(approvalService.getRecordsByInstanceAndNode(instanceId, nodeCode));
    }

    @Operation(summary = "按审批人查询审批记录")
    @GetMapping("/record/approver/{approver}")
    public Result<List<ApprovalRecord>> getRecordsByApprover(
            @PathVariable String approver, @RequestParam(defaultValue = "20") int limit) {
        return Result.success(approvalService.getRecordsByApprover(approver, limit));
    }

    @Operation(summary = "统计节点通过人数")
    @GetMapping("/record/count-approve")
    public Result<Integer> countApproveByNode(
            @RequestParam String instanceId, @RequestParam String nodeCode) {
        return Result.success(approvalService.countApproveByNode(instanceId, nodeCode));
    }
}
