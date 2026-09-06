package com.xwms.core.wave.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.wave.entity.*;
import com.xwms.core.wave.service.WaveService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 波次管理 Controller */
@Tag(name = "波次管理", description = "波次创建/分组/分配/拣货任务/路径规划/执行")
@RestController
@RequestMapping("/api/wave")
@RequiredArgsConstructor
public class WaveController {

    private final WaveService waveService;

    // ============================================================

    // 波次创建与分组
    // ============================================================

    @Operation(summary = "创建波次（手动）")
    @PostMapping
    public Result<Wave> createWave(
            @RequestBody Wave wave,
            @RequestParam(required = false) List<String> outboundNos,
            @RequestParam(required = false) String operator) {
        return Result.success(waveService.createWave(wave, outboundNos, operator));
    }

    @Operation(summary = "自动波次分组")
    @PostMapping("/auto")
    public Result<List<Wave>> autoCreateWave(
            @RequestParam String warehouseCode,
            @RequestParam String ownerCode,
            @RequestParam String waveType,
            @RequestParam String pickMode,
            @RequestParam(defaultValue = "50") int maxOrderCount,
            @RequestParam(defaultValue = "10000") BigDecimal maxTotalQty,
            @RequestBody List<Map<String, Object>> pendingOrders,
            @RequestParam(required = false) String operator) {
        return Result.success(
                waveService.autoCreateWave(
                        warehouseCode,
                        ownerCode,
                        waveType,
                        pickMode,
                        maxOrderCount,
                        maxTotalQty,
                        pendingOrders,
                        operator));
    }

    // ============================================================

    // 波次分配
    // ============================================================

    @Operation(summary = "执行波次分配")
    @PostMapping("/{waveNo}/allocate")
    public Result<Wave> allocateWave(
            @PathVariable String waveNo, @RequestParam(required = false) String operator) {
        return Result.success(waveService.allocateWave(waveNo, operator));
    }

    // ============================================================

    // 拣货任务生成
    // ============================================================

    @Operation(summary = "生成拣货任务")
    @PostMapping("/{waveNo}/tasks")
    public Result<List<WavePickTask>> generatePickTasks(
            @PathVariable String waveNo, @RequestParam String pickMode) {
        return Result.success(waveService.generatePickTasks(waveNo, pickMode));
    }

    // ============================================================

    // 拣货路径规划
    // ============================================================

    @Operation(summary = "规划拣货路径")
    @PostMapping("/{waveNo}/path")
    public Result<List<WavePath>> planPath(@PathVariable String waveNo) {
        return Result.success(waveService.planPath(waveNo));
    }

    // ============================================================

    // 波次执行
    // ============================================================

    @Operation(summary = "开始波次拣货")
    @PostMapping("/{waveNo}/start")
    public Result<Wave> startWave(@PathVariable String waveNo, @RequestParam String picker) {
        return Result.success(waveService.startWave(waveNo, picker));
    }

    @Operation(summary = "完成波次拣货")
    @PostMapping("/{waveNo}/complete")
    public Result<Wave> completeWave(@PathVariable String waveNo) {
        return Result.success(waveService.completeWave(waveNo));
    }

    @Operation(summary = "执行单个拣货任务")
    @PostMapping("/task/{taskNo}/execute")
    public Result<WavePickTask> executePickTask(
            @PathVariable String taskNo,
            @RequestParam BigDecimal pickedQty,
            @RequestParam(required = false) BigDecimal differenceQty,
            @RequestParam(required = false) String picker) {
        return Result.success(
                waveService.executePickTask(taskNo, pickedQty, differenceQty, picker));
    }

    // ============================================================

    // 查询
    // ============================================================

    @Operation(summary = "分页查询波次")
    @GetMapping
    public Result<Page<Wave>> pageWaves(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String waveType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String ownerCode) {
        return Result.success(
                waveService.pageWaves(
                        new Page<>(page, size), waveType, status, warehouseCode, ownerCode));
    }

    @Operation(summary = "按波次号查询")
    @GetMapping("/{waveNo}")
    public Result<Wave> getWaveByNo(@PathVariable String waveNo) {
        return Result.success(waveService.getWaveByNo(waveNo));
    }

    @Operation(summary = "查询波次明细")
    @GetMapping("/{waveNo}/details")
    public Result<List<WaveDetail>> getWaveDetails(@PathVariable String waveNo) {
        return Result.success(waveService.getWaveDetails(waveNo));
    }

    @Operation(summary = "查询拣货任务")
    @GetMapping("/{waveNo}/tasks")
    public Result<List<WavePickTask>> getPickTasks(@PathVariable String waveNo) {
        return Result.success(waveService.getPickTasks(waveNo));
    }

    @Operation(summary = "查询拣货路径")
    @GetMapping("/{waveNo}/path")
    public Result<List<WavePath>> getWavePaths(@PathVariable String waveNo) {
        return Result.success(waveService.getWavePaths(waveNo));
    }
}
