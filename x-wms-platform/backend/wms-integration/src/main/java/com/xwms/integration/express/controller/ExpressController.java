package com.xwms.integration.express.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.xwms.common.core.Result;
import com.xwms.integration.express.service.ExpressGetService;

import lombok.RequiredArgsConstructor;

/** 快递单管理Controller 快递单号批量获取、打印、查询 */
@RestController
@RequestMapping("/api/express")
@RequiredArgsConstructor
public class ExpressController {

    private final ExpressGetService expressGetService;

    /** 批量获取快递单号（异步，走Kafka） */
    @PostMapping("/batch-get")
    public Result<String> batchGetTrackingNo(@RequestBody List<Map<String, Object>> orders) {
        String batchId = expressGetService.batchGetTrackingNo(orders);
        return Result.success(batchId);
    }

    /** 查询批量获取结果 */
    @GetMapping("/batch-result/{batchId}")
    public Result<Map<String, Object>> getBatchResult(@PathVariable String batchId) {
        return Result.success(expressGetService.getBatchResult(batchId));
    }

    /** 单个获取快递单号（同步） */
    @PostMapping("/get")
    public Result<Map<String, Object>> getTrackingNo(@RequestBody Map<String, Object> order) {
        return Result.success(expressGetService.getTrackingNoSync(order));
    }

    /** 批量打印快递单 */
    @PostMapping("/batch-print")
    public Result<String> batchPrint(@RequestBody List<String> orderNos) {
        String printTaskId = expressGetService.batchPrint(orderNos);
        return Result.success(printTaskId);
    }
}
