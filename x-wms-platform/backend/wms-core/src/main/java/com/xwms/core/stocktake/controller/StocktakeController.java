package com.xwms.core.stocktake.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.stocktake.dto.StocktakeCountRequest;
import com.xwms.core.stocktake.dto.StocktakeCreateRequest;
import com.xwms.core.stocktake.entity.*;
import com.xwms.core.stocktake.service.StocktakeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 鐩樼偣绠＄悊 Controller */
@Tag(name = "鐩樼偣绠＄悊", description = "鐩樼偣浠诲姟/鐩樼偣鏄庣粏/宸紓澶勭悊/搴撳瓨璋冩暣")
@RestController
@RequestMapping("/api/stocktake")
@RequiredArgsConstructor
public class StocktakeController {

    private final StocktakeService stocktakeService;

    // ============================================================

    // 鐩樼偣浠诲姟
    // ============================================================

    @Operation(summary = "鍒涘缓鐩樼偣浠诲姟")
    @PostMapping("/tasks")
    public Result<StocktakeTask> createTask(@RequestBody StocktakeCreateRequest request) {
        return Result.success(stocktakeService.createStocktake(request));
    }

    @Operation(summary = "鐢熸垚鐩樼偣鏄庣粏")
    @PostMapping("/tasks/{id}/generate")
    public Result<Integer> generateItems(@PathVariable Long id) {
        return Result.success(stocktakeService.generateItems(id));
    }

    @Operation(summary = "寮€濮嬬洏鐐")
    @PutMapping("/tasks/{id}/start")
    public Result<StocktakeTask> startStocktake(@PathVariable Long id) {
        return Result.success(stocktakeService.startStocktake(id));
    }

    @Operation(summary = "瀹屾垚鐩樼偣(鐢熸垚宸紓)")
    @PutMapping("/tasks/{id}/finish")
    public Result<Integer> finishStocktake(@PathVariable Long id) {
        return Result.success(stocktakeService.finishStocktake(id));
    }

    @Operation(summary = "瀹屾垚鐩樼偣浠诲姟")
    @PutMapping("/tasks/{id}/complete")
    public Result<StocktakeTask> completeTask(@PathVariable Long id) {
        return Result.success(stocktakeService.completeTask(id));
    }

    @Operation(summary = "鏌ヨ鐩樼偣浠诲姟璇︽儏")
    @GetMapping("/tasks/{id}")
    public Result<StocktakeTask> getTask(@PathVariable Long id) {
        return Result.success(stocktakeService.getTask(id));
    }

    @Operation(summary = "鍒嗛〉鏌ヨ鐩樼偣浠诲姟")
    @GetMapping("/tasks")
    public Result<Page<StocktakeTask>> pageTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String warehouse) {
        return Result.success(
                stocktakeService.pageTasks(new Page<>(page, size), status, type, warehouse));
    }

    // ============================================================

    // 鐩樼偣鏄庣粏
    // ============================================================

    @Operation(summary = "鏌ヨ鐩樼偣鏄庣粏")
    @GetMapping("/tasks/{id}/items")
    public Result<List<StocktakeItem>> getItems(@PathVariable Long id) {
        return Result.success(stocktakeService.getItems(id));
    }

    @Operation(summary = "褰曞叆鐩樼偣鏁伴噺")
    @PutMapping("/items/count")
    public Result<StocktakeItem> countItem(@RequestBody StocktakeCountRequest request) {
        return Result.success(stocktakeService.countItem(request));
    }

    // ============================================================
}
