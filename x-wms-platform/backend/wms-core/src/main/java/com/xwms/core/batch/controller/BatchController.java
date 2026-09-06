package com.xwms.core.batch.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.PageResult;
import com.xwms.common.core.Result;
import com.xwms.core.batch.entity.Batch;
import com.xwms.core.batch.mapper.BatchMapper;
import com.xwms.core.batch.service.BatchService;

import lombok.RequiredArgsConstructor;

/** 批次管理Controller 批次全链路追踪：创建→质检→冻结→FEFO查询→近效期预警 */
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;
    private final BatchMapper batchMapper;

    /** 创建批次 */
    @PostMapping
    public Result<Batch> create(@RequestBody Batch batch) {
        return Result.success(batchService.create(batch));
    }

    /** 分页查询批次 */
    @GetMapping("/page")
    public Result<PageResult<Batch>> page(
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String qcStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<Batch> page =
                batchMapper.selectPage(
                        new Page<>(pageNum, pageSize),
                        new LambdaQueryWrapper<Batch>()
                                .eq(sku != null, Batch::getSku, sku)
                                .like(batchNo != null, Batch::getBatchNo, batchNo)
                                .eq(warehouse != null, Batch::getWarehouse, warehouse)
                                .eq(status != null, Batch::getStatus, status)
                                .eq(qcStatus != null, Batch::getQcStatus, qcStatus)
                                .orderByDesc(Batch::getCreatedAt));
        return Result.success(
                PageResult.of(
                        page.getRecords(),
                        page.getTotal(),
                        (int) page.getCurrent(),
                        (int) page.getSize()));
    }

    /** 按SKU查询批次（FEFO近效期先出排序） */
    @GetMapping("/list")
    public Result<List<Batch>> listBySku(@RequestParam String sku, @RequestParam String warehouse) {
        return Result.success(batchService.listBySku(sku, warehouse));
    }

    /** 质检完成 */
    @PostMapping("/{batchNo}/qc")
    public Result<Void> completeQc(
            @PathVariable String batchNo,
            @RequestParam String sku,
            @RequestParam String warehouse,
            @RequestParam Boolean passed,
            @RequestParam String qcNo) {
        batchService.completeQc(batchNo, sku, warehouse, passed, qcNo);
        return Result.success();
    }

    /** 冻结/解冻批次 */
    @PostMapping("/{batchNo}/freeze")
    public Result<Void> freeze(
            @PathVariable String batchNo,
            @RequestParam String sku,
            @RequestParam String warehouse,
            @RequestParam Boolean freeze) {
        batchService.freeze(batchNo, sku, warehouse, freeze);
        return Result.success();
    }

    /** 近效期批次查询（预警用） */
    @GetMapping("/near-expire")
    public Result<List<Batch>> listNearExpire(
            @RequestParam String warehouse, @RequestParam(defaultValue = "30") Integer days) {
        return Result.success(batchService.listNearExpire(warehouse, days));
    }
}
