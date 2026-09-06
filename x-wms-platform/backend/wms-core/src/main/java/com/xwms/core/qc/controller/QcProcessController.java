package com.xwms.core.qc.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.qc.entity.QcSample;
import com.xwms.core.qc.entity.QcTask;
import com.xwms.core.qc.entity.QcUnqualified;
import com.xwms.core.qc.service.QcProcessService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 质检流程控制器 收货前/后质检与入库流程集成 */
@Tag(name = "质检流程管理", description = "收货前/后质检/质检权限校验/不合格品处理/样本管理")
@RestController
@RequestMapping("/api/qc/process")
@RequiredArgsConstructor
public class QcProcessController {

    private final QcProcessService qcProcessService;

    // ============================================================

    // 1. 收货前质检
    // ============================================================

    @Operation(summary = "创建收货前质检任务")
    @PostMapping("/before-receive/{asnNo}")
    public Result<List<QcTask>> createBeforeReceiveQcTasks(
            @PathVariable String asnNo, @RequestParam String operator) {
        return Result.success(qcProcessService.createBeforeReceiveQcTasks(asnNo, operator));
    }

    @Operation(summary = "收货前质检完成")
    @PutMapping("/before-receive/{taskNo}/complete")
    public Result<QcTask> completeBeforeReceiveQc(
            @PathVariable String taskNo,
            @RequestParam String qcResult,
            @RequestParam String inspector,
            @RequestParam(required = false) String remark) {
        return Result.success(
                qcProcessService.completeBeforeReceiveQc(taskNo, qcResult, inspector, remark));
    }

    // ============================================================

    // 2. 收货后质检
    // ============================================================

    @Operation(summary = "创建收货后质检任务")
    @PostMapping("/after-receive/{inboundNo}")
    public Result<List<QcTask>> createAfterReceiveQcTasks(
            @PathVariable String inboundNo,
            @RequestParam(required = false) String receiptTaskNo,
            @RequestParam String operator) {
        return Result.success(
                qcProcessService.createAfterReceiveQcTasks(inboundNo, receiptTaskNo, operator));
    }

    @Operation(summary = "收货后质检完成")
    @PutMapping("/after-receive/{taskNo}/complete")
    public Result<QcTask> completeAfterReceiveQc(
            @PathVariable String taskNo,
            @RequestParam String qcResult,
            @RequestParam String inspector,
            @RequestParam(required = false) String remark) {
        return Result.success(
                qcProcessService.completeAfterReceiveQc(taskNo, qcResult, inspector, remark));
    }

    // ============================================================

    // 3. 质检权限校验
    // ============================================================

    @Operation(summary = "校验是否允许收货")
    @GetMapping("/can-receive/{asnNo}")
    public Result<Boolean> canReceive(@PathVariable String asnNo) {
        return Result.success(qcProcessService.canReceive(asnNo));
    }

    @Operation(summary = "校验是否允许上架")
    @GetMapping("/can-putaway/{inboundNo}")
    public Result<Boolean> canPutaway(@PathVariable String inboundNo) {
        return Result.success(qcProcessService.canPutaway(inboundNo));
    }

    // ============================================================

    // 4. 不合格品处理
    // ============================================================

    @Operation(summary = "不合格品处理")
    @PutMapping("/unqualified/{id}/handle")
    public Result<QcUnqualified> handleUnqualified(
            @PathVariable Long id,
            @RequestParam String handleMethod,
            @RequestParam BigDecimal handleQty,
            @RequestParam String handler,
            @RequestParam(required = false) String cert,
            @RequestParam(required = false) String remark) {
        return Result.success(
                qcProcessService.handleUnqualified(
                        id, handleMethod, handleQty, handler, cert, remark));
    }

    // ============================================================

    // 5. 质检样本管理
    // ============================================================

    @Operation(summary = "抽取质检样本")
    @PostMapping("/sample/{taskNo}/draw")
    public Result<List<QcSample>> drawSamples(
            @PathVariable String taskNo,
            @RequestParam int sampleCount,
            @RequestParam String drawer) {
        return Result.success(qcProcessService.drawSamples(taskNo, sampleCount, drawer));
    }

    @Operation(summary = "录入样本检测结果")
    @PutMapping("/sample/{sampleNo}/result")
    public Result<QcSample> submitSampleResult(
            @PathVariable String sampleNo,
            @RequestParam(required = false) String testValue,
            @RequestParam String isQualified,
            @RequestParam(required = false) String defectType,
            @RequestParam(required = false) String defectDesc,
            @RequestParam String tester) {
        return Result.success(
                qcProcessService.submitSampleResult(
                        sampleNo, testValue, isQualified, defectType, defectDesc, tester));
    }

    @Operation(summary = "归还质检样本")
    @PutMapping("/sample/{sampleNo}/return")
    public Result<QcSample> returnSample(
            @PathVariable String sampleNo, @RequestParam String returner) {
        return Result.success(qcProcessService.returnSample(sampleNo, returner));
    }

    @Operation(summary = "查询任务样本列表")
    @GetMapping("/sample/{taskNo}")
    public Result<List<QcSample>> getSamplesByTask(@PathVariable String taskNo) {
        return Result.success(qcProcessService.getSamplesByTask(taskNo));
    }

    // ============================================================

    // 6. 查询
    // ============================================================

    @Operation(summary = "根据任务号查询质检任务")
    @GetMapping("/task/{taskNo}")
    public Result<QcTask> getQcTaskByNo(@PathVariable String taskNo) {
        return Result.success(qcProcessService.getQcTaskByNo(taskNo));
    }

    @Operation(summary = "根据ASN号查询质检任务")
    @GetMapping("/task/asn/{asnNo}")
    public Result<List<QcTask>> getQcTasksByAsn(@PathVariable String asnNo) {
        return Result.success(qcProcessService.getQcTasksByAsn(asnNo));
    }

    @Operation(summary = "根据入库单号查询质检任务")
    @GetMapping("/task/inbound/{inboundNo}")
    public Result<List<QcTask>> getQcTasksByInbound(@PathVariable String inboundNo) {
        return Result.success(qcProcessService.getQcTasksByInbound(inboundNo));
    }

    @Operation(summary = "分页查询质检任务")
    @GetMapping("/task")
    public Result<Page<QcTask>> pageQcTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String qcTiming,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String asnNo,
            @RequestParam(required = false) String inboundNo) {
        return Result.success(
                qcProcessService.pageQcTasks(
                        new Page<>(page, size), status, qcTiming, warehouseCode, asnNo, inboundNo));
    }
}
