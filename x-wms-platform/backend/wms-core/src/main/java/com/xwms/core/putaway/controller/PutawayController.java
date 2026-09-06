package com.xwms.core.putaway.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.putaway.entity.*;
import com.xwms.core.putaway.service.PutawayService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 上架管理控制器 支持7种上架方式 */
@Tag(name = "上架管理", description = "多种上架方式/库位推荐/集成上架规则")
@RestController
@RequestMapping("/api/putaway")
@RequiredArgsConstructor
public class PutawayController {

    private final PutawayService putawayService;

    // ============================================================

    // 1. 上架任务管理
    // ============================================================

    @Operation(summary = "从入库单生成上架任务")
    @PostMapping("/task/from-inbound/{inboundNo}")
    public Result<PutawayTask> createTaskFromInbound(
            @PathVariable String inboundNo,
            @RequestParam(required = false) String putawayType,
            @RequestParam(required = false) String putawayStrategy,
            @RequestParam String operator) {
        return Result.success(
                putawayService.createTaskFromInbound(
                        inboundNo, putawayType, putawayStrategy, operator));
    }

    @Operation(summary = "取消上架任务")
    @PutMapping("/task/{taskNo}/cancel")
    public Result<PutawayTask> cancelTask(
            @PathVariable String taskNo,
            @RequestParam String cancelReason,
            @RequestParam String operator) {
        return Result.success(putawayService.cancelTask(taskNo, cancelReason, operator));
    }

    @Operation(summary = "查询上架任务")
    @GetMapping("/task/{taskNo}")
    public Result<PutawayTask> getTaskByNo(@PathVariable String taskNo) {
        return Result.success(putawayService.getTaskByNo(taskNo));
    }

    @Operation(summary = "查询上架任务明细")
    @GetMapping("/task/{taskNo}/details")
    public Result<List<PutawayTaskDetail>> getTaskDetails(@PathVariable String taskNo) {
        return Result.success(putawayService.getTaskDetails(taskNo));
    }

    @Operation(summary = "分页查询上架任务")
    @GetMapping("/task")
    public Result<Page<PutawayTask>> pageTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String putawayType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String inboundNo) {
        return Result.success(
                putawayService.pageTasks(
                        new Page<>(page, size), putawayType, status, warehouseCode, inboundNo));
    }

    // ============================================================

    // 2. 库位推荐
    // ============================================================

    @Operation(summary = "重新推荐库位")
    @PutMapping("/detail/{detailNo}/recommend")
    public Result<PutawayTaskDetail> reRecommendLocation(
            @PathVariable String detailNo,
            @RequestParam(required = false) String strategy,
            @RequestParam String operator) {
        return Result.success(putawayService.reRecommendLocation(detailNo, strategy, operator));
    }

    // ============================================================

    // 3. C1 标准上架
    // ============================================================

    @Operation(summary = "标准上架")
    @PostMapping("/standard")
    public Result<PutawayRecord> standardPutaway(
            @RequestParam String taskNo,
            @RequestParam String detailNo,
            @RequestParam String targetLocation,
            @RequestParam BigDecimal putawayQty,
            @RequestParam String operator) {
        return Result.success(
                putawayService.standardPutaway(
                        taskNo, detailNo, targetLocation, putawayQty, operator));
    }

    // ============================================================

    // 4. C2 快捷上架
    // ============================================================

    @Operation(summary = "快捷上架（全部上架到推荐库位）")
    @PostMapping("/quick/{taskNo}")
    public Result<PutawayTask> quickPutaway(
            @PathVariable String taskNo, @RequestParam String operator) {
        return Result.success(putawayService.quickPutaway(taskNo, operator));
    }

    // ============================================================

    // 5. C3 合并上架
    // ============================================================

    @Operation(summary = "合并上架（多SKU同托）")
    @PostMapping("/merge")
    public Result<List<PutawayRecord>> mergePutaway(
            @RequestParam String taskNo,
            @RequestParam List<String> detailNos,
            @RequestParam String targetLocation,
            @RequestParam(required = false) String lpnNo,
            @RequestParam String operator) {
        return Result.success(
                putawayService.mergePutaway(taskNo, detailNos, targetLocation, lpnNo, operator));
    }

    // ============================================================

    // 6. C4 批量上架
    // ============================================================

    @Operation(summary = "批量上架（多托同库位）")
    @PostMapping("/batch")
    public Result<List<PutawayRecord>> batchPutaway(
            @RequestParam String taskNo,
            @RequestParam String targetLocation,
            @RequestParam(required = false) List<String> lpnNos,
            @RequestParam String operator) {
        return Result.success(
                putawayService.batchPutaway(taskNo, targetLocation, lpnNos, operator));
    }

    // ============================================================

    // 7. C5 按箱码/LPN上架
    // ============================================================

    @Operation(summary = "按箱码/LPN上架")
    @PostMapping("/lpn")
    public Result<PutawayRecord> putawayByLpn(
            @RequestParam String taskNo,
            @RequestParam String lpnNo,
            @RequestParam String targetLocation,
            @RequestParam BigDecimal putawayQty,
            @RequestParam String operator) {
        return Result.success(
                putawayService.putawayByLpn(taskNo, lpnNo, targetLocation, putawayQty, operator));
    }

    // ============================================================

    // 8. C6 直接收货到存储库位
    // ============================================================

    @Operation(summary = "直接收货到存储库位（免上架）")
    @PostMapping("/direct")
    public Result<PutawayRecord> directPutaway(
            @RequestParam String inboundNo,
            @RequestParam String inboundDetailNo,
            @RequestParam String targetLocation,
            @RequestParam BigDecimal putawayQty,
            @RequestParam String operator) {
        return Result.success(
                putawayService.directPutaway(
                        inboundNo, inboundDetailNo, targetLocation, putawayQty, operator));
    }

    // ============================================================

    // 9. C7 码盘预约库位
    // ============================================================

    @Operation(summary = "码盘预约库位")
    @PutMapping("/reservation")
    public Result<PutawayTaskDetail> reservationPutaway(
            @RequestParam String taskNo,
            @RequestParam String detailNo,
            @RequestParam String reservedLocation,
            @RequestParam String operator) {
        return Result.success(
                putawayService.reservationPutaway(taskNo, detailNo, reservedLocation, operator));
    }

    // ============================================================

    // 10. 上架完成
    // ============================================================

    @Operation(summary = "标记上架完成")
    @PutMapping("/task/{taskNo}/complete")
    public Result<PutawayTask> completePutaway(
            @PathVariable String taskNo, @RequestParam String operator) {
        return Result.success(putawayService.completePutaway(taskNo, operator));
    }
}
