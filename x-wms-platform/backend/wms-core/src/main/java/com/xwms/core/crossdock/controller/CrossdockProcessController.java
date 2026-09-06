package com.xwms.core.crossdock.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.xwms.core.crossdock.entity.*;
import com.xwms.core.crossdock.service.CrossdockProcessService;

import lombok.RequiredArgsConstructor;

/** 越库流程深化Controller */
@RestController
@RequestMapping("/api/crossdock/process")
@RequiredArgsConstructor
public class CrossdockProcessController {

    private final CrossdockProcessService crossdockProcessService;

    // ==================== 越库预配 ====================

    /** 执行越库预配 */
    @PostMapping("/{crossdockNo}/pre-alloc")
    public List<CrossdockPreAllocation> executePreAllocation(
            @PathVariable String crossdockNo,
            @RequestParam(required = false) String asnNo,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return crossdockProcessService.executePreAllocation(crossdockNo, asnNo, operator);
    }

    /** 查询越库预配记录 */
    @GetMapping("/{crossdockNo}/pre-alloc")
    public List<CrossdockPreAllocation> getPreAllocations(@PathVariable String crossdockNo) {
        return crossdockProcessService.getPreAllocations(crossdockNo);
    }

    // ==================== 越库预分 ====================

    /** RF越库预分扫描 */
    @PostMapping("/{crossdockNo}/pre-sort/scan")
    public CrossdockPreSort executePreSortScan(
            @PathVariable String crossdockNo,
            @RequestParam String boxNo,
            @RequestParam(required = false) String boxSerialNo,
            @RequestParam String skuCode,
            @RequestParam BigDecimal boxQty,
            @RequestParam(required = false, defaultValue = "system") String scanner) {
        return crossdockProcessService.executePreSortScan(
                crossdockNo, boxNo, boxSerialNo, skuCode, boxQty, scanner);
    }

    /** 越库预分收货确认 */
    @PostMapping("/pre-sort/{sortNo}/receive")
    public CrossdockPreSort confirmPreSortReceive(
            @PathVariable String sortNo,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return crossdockProcessService.confirmPreSortReceive(sortNo, operator);
    }

    /** 越库预分分配出库 */
    @PostMapping("/pre-sort/{sortNo}/allocate")
    public CrossdockPreSort allocatePreSortToOutbound(
            @PathVariable String sortNo,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return crossdockProcessService.allocatePreSortToOutbound(sortNo, operator);
    }

    /** 查询越库预分记录 */
    @GetMapping("/{crossdockNo}/pre-sort")
    public List<CrossdockPreSort> getPreSorts(@PathVariable String crossdockNo) {
        return crossdockProcessService.getPreSorts(crossdockNo);
    }

    /** 根据箱号查询预分记录 */
    @GetMapping("/pre-sort/box/{boxNo}")
    public CrossdockPreSort getPreSortByBoxNo(@PathVariable String boxNo) {
        return crossdockProcessService.getPreSortByBoxNo(boxNo);
    }

    // ==================== 与出库联动 ====================

    /** 越库货物直接分拣到出库月台 */
    @PostMapping("/{crossdockNo}/direct-sort")
    public CrossdockTask directSortToOutbound(
            @PathVariable String crossdockNo,
            @RequestParam String sortNo,
            @RequestParam String fromLocation,
            @RequestParam String toLocation,
            @RequestParam BigDecimal sortQty,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return crossdockProcessService.directSortToOutbound(
                crossdockNo, sortNo, fromLocation, toLocation, sortQty, operator);
    }

    /** 越库货物直接发运 */
    @PostMapping("/{crossdockNo}/direct-ship")
    public CrossdockTask directShipFromDock(
            @PathVariable String crossdockNo,
            @RequestParam String fromLocation,
            @RequestParam BigDecimal shipQty,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return crossdockProcessService.directShipFromDock(
                crossdockNo, fromLocation, shipQty, operator);
    }
}
