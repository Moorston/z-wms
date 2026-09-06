package com.xwms.base.container.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.container.entity.*;
import com.xwms.base.container.service.ContainerService;
import com.xwms.common.core.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 容器管理 Controller */
@Tag(name = "容器管理", description = "容器类型/容器档案/绑定/移动/追踪")
@RestController
@RequestMapping("/api/container")
@RequiredArgsConstructor
public class ContainerController {

    private final ContainerService containerService;

    // ============================================================
    // 容器类型
    // ============================================================

    @Operation(summary = "创建容器类型")
    @PostMapping("/type")
    public Result<ContainerType> createType(@RequestBody ContainerType type) {
        return Result.success(containerService.createType(type));
    }

    @Operation(summary = "分页查询容器类型")
    @GetMapping("/type")
    public Result<Page<ContainerType>> pageTypes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category) {
        return Result.success(containerService.pageTypes(new Page<>(page, size), category));
    }

    @Operation(summary = "按编码查询容器类型")
    @GetMapping("/type/{typeCode}")
    public Result<ContainerType> getTypeByCode(@PathVariable String typeCode) {
        return Result.success(containerService.getTypeByCode(typeCode));
    }

    // ============================================================
    // 容器档案
    // ============================================================

    @Operation(summary = "创建容器")
    @PostMapping
    public Result<Container> createContainer(@RequestBody Container container) {
        return Result.success(containerService.createContainer(container));
    }

    @Operation(summary = "分页查询容器")
    @GetMapping
    public Result<Page<Container>> pageContainers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String typeCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String warehouseCode) {
        return Result.success(
                containerService.pageContainers(
                        new Page<>(page, size), typeCode, status, location, warehouseCode));
    }

    @Operation(summary = "按编号查询容器")
    @GetMapping("/{containerNo}")
    public Result<Container> getContainerByNo(@PathVariable String containerNo) {
        return Result.success(containerService.getContainerByNo(containerNo));
    }

    @Operation(summary = "获取空容器")
    @GetMapping("/empty")
    public Result<Container> getEmptyContainer(
            @RequestParam String typeCode, @RequestParam String warehouseCode) {
        return Result.success(containerService.getEmptyContainer(typeCode, warehouseCode));
    }

    // ============================================================
    // 容器绑定
    // ============================================================

    @Operation(summary = "容器绑定（装入商品）")
    @PostMapping("/{containerNo}/bind")
    public Result<ContainerBind> bindContainer(
            @PathVariable String containerNo,
            @RequestParam String refType,
            @RequestParam String refNo,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam BigDecimal quantity,
            @RequestParam(required = false) String operator) {
        return Result.success(
                containerService.bindContainer(
                        containerNo, refType, refNo, skuCode, batchNo, quantity, operator));
    }

    @Operation(summary = "容器释放（取出商品）")
    @PostMapping("/{containerNo}/release")
    public Result<ContainerBind> releaseContainer(
            @PathVariable String containerNo,
            @RequestParam String refType,
            @RequestParam String refNo,
            @RequestParam BigDecimal quantity,
            @RequestParam(required = false) String operator) {
        return Result.success(
                containerService.releaseContainer(containerNo, refType, refNo, quantity, operator));
    }

    @Operation(summary = "查询容器绑定列表")
    @GetMapping("/{containerNo}/binds")
    public Result<List<ContainerBind>> getContainerBinds(@PathVariable String containerNo) {
        return Result.success(containerService.getContainerBinds(containerNo));
    }

    @Operation(summary = "按单据查询绑定")
    @GetMapping("/bind/ref")
    public Result<List<ContainerBind>> getBindsByRef(
            @RequestParam String refType, @RequestParam String refNo) {
        return Result.success(containerService.getBindsByRef(refType, refNo));
    }

    // ============================================================
    // 容器移动
    // ============================================================

    @Operation(summary = "容器移动")
    @PostMapping("/{containerNo}/move")
    public Result<Container> moveContainer(
            @PathVariable String containerNo,
            @RequestParam String fromLocation,
            @RequestParam String toLocation,
            @RequestParam(required = false) String operator) {
        return Result.success(
                containerService.moveContainer(containerNo, fromLocation, toLocation, operator));
    }

    // ============================================================
    // 容器清洁/维修/报废
    // ============================================================

    @Operation(summary = "容器清洁")
    @PostMapping("/{containerNo}/clean")
    public Result<Container> cleanContainer(
            @PathVariable String containerNo, @RequestParam(required = false) String operator) {
        return Result.success(containerService.cleanContainer(containerNo, operator));
    }

    @Operation(summary = "容器维修")
    @PostMapping("/{containerNo}/repair")
    public Result<Container> repairContainer(
            @PathVariable String containerNo, @RequestParam(required = false) String operator) {
        return Result.success(containerService.repairContainer(containerNo, operator));
    }

    @Operation(summary = "容器报废")
    @PostMapping("/{containerNo}/discard")
    public Result<Container> discardContainer(
            @PathVariable String containerNo, @RequestParam(required = false) String operator) {
        return Result.success(containerService.discardContainer(containerNo, operator));
    }

    // ============================================================
    // 容器追踪
    // ============================================================

    @Operation(summary = "查询容器流转记录")
    @GetMapping("/{containerNo}/trace")
    public Result<List<ContainerTrace>> getContainerTrace(@PathVariable String containerNo) {
        return Result.success(containerService.getContainerTrace(containerNo));
    }
}
