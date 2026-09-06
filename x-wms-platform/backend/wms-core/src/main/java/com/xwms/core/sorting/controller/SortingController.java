package com.xwms.core.sorting.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.xwms.core.sorting.entity.SortingDifference;
import com.xwms.core.sorting.entity.SortingGrid;
import com.xwms.core.sorting.entity.SortingWall;
import com.xwms.core.sorting.service.SortingExceptionService;
import com.xwms.core.sorting.service.SortingExecutionService;
import com.xwms.core.sorting.service.SortingWallService;
import com.xwms.core.sorting.service.VirtualLocationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 二次分拣（播种式）Controller 提供播种墙管理、分播执行、异常处理、虚拟库位查询等接口 */
@Tag(name = "二次分拣管理", description = "播种式分拣、播种墙、虚拟库位")
@RestController
@RequestMapping("/api/sorting")
@RequiredArgsConstructor
public class SortingController {

    private final SortingWallService sortingWallService;
    private final SortingExecutionService sortingExecutionService;
    private final SortingExceptionService sortingExceptionService;
    private final VirtualLocationService virtualLocationService;

    // ========== 播种墙管理 ==========

    @Operation(summary = "创建播种墙")
    @PostMapping("/wall/create")
    public SortingWall createWall(@RequestBody Map<String, Object> req) {
        return sortingWallService.createWall(
                (String) req.get("wallCode"),
                (String) req.get("wallName"),
                (String) req.get("warehouseCode"),
                (String) req.get("areaCode"),
                (Integer) req.get("rows"),
                (Integer) req.get("cols"));
    }

    @Operation(summary = "波次绑定播种墙")
    @PostMapping("/wall/bind")
    public List<SortingGrid> bindWave(@RequestBody Map<String, Object> req) {
        @SuppressWarnings("unchecked")
        List<String> orderNos = (List<String>) req.get("orderNos");
        return sortingWallService.bindWaveToWall(
                (String) req.get("waveNo"), (String) req.get("wallCode"), orderNos);
    }

    @Operation(summary = "释放播种墙")
    @PostMapping("/wall/release/{wallCode}")
    public void releaseWall(@PathVariable String wallCode) {
        sortingWallService.releaseWall(wallCode);
    }

    @Operation(summary = "查询播种墙格口状态")
    @GetMapping("/wall/grids/{wallCode}")
    public List<SortingGrid> listGrids(@PathVariable String wallCode) {
        return sortingWallService.listGrids(wallCode);
    }

    // ========== 分播执行 ==========

    @Operation(summary = "扫描商品分播到格口")
    @PostMapping("/scan")
    public SortingExecutionService.SortingResult scanAndSort(@RequestBody Map<String, Object> req) {
        return sortingExecutionService.scanAndSort(
                (String) req.get("waveNo"),
                (String) req.get("wallCode"),
                (String) req.get("gridNo"),
                (String) req.get("sku"),
                (String) req.get("batchNo"),
                new BigDecimal(req.get("qty").toString()),
                (String) req.get("warehouse"));
    }

    @Operation(summary = "第一次拣货完成，库存转入分拣虚拟库位")
    @PostMapping("/first-pick-complete")
    public void firstPickComplete(@RequestBody Map<String, Object> req) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) req.get("pickItems");
        List<VirtualLocationService.PickItem> pickItems =
                items.stream()
                        .map(
                                i ->
                                        new VirtualLocationService.PickItem(
                                                (String) i.get("fromLocation"),
                                                (String) i.get("sku"),
                                                (String) i.get("batchNo"),
                                                new BigDecimal(i.get("qty").toString())))
                        .toList();
        virtualLocationService.onFirstPickComplete(
                (String) req.get("waveNo"), (String) req.get("warehouse"), pickItems);
    }

    // ========== 异常处理 ==========

    @Operation(summary = "记录分拣差异")
    @PostMapping("/difference")
    public SortingDifference handleDifference(@RequestBody Map<String, Object> req) {
        return sortingExceptionService.handleDifference(
                (String) req.get("waveNo"),
                (String) req.get("orderNo"),
                (String) req.get("sku"),
                (String) req.get("batchNo"),
                (String) req.get("type"),
                new BigDecimal(req.get("qty").toString()),
                (String) req.get("reason"),
                (String) req.get("warehouse"));
    }

    @Operation(summary = "处理分拣差异")
    @PostMapping("/difference/resolve/{id}")
    public void resolveDifference(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        sortingExceptionService.resolveDifference(
                id,
                (String) req.get("handleMethod"),
                (String) req.get("targetLocation"),
                (String) req.get("warehouse"),
                (String) req.get("handler"));
    }

    @Operation(summary = "查询待处理差异")
    @GetMapping("/difference/pending")
    public List<SortingDifference> listPendingDifferences(
            @RequestParam(required = false) String waveNo) {
        return sortingExceptionService.listPendingDifferences(waveNo);
    }

    // ========== 虚拟库位查询 ==========

    @Operation(summary = "虚拟库位对账")
    @GetMapping("/virtual/reconcile")
    public BigDecimal reconcile(
            @RequestParam String waveNo,
            @RequestParam String warehouse,
            @RequestParam BigDecimal totalPickedQty,
            @RequestParam BigDecimal sortedQty) {
        return virtualLocationService.reconcileWaveSorting(
                waveNo, warehouse, totalPickedQty, sortedQty);
    }
}
