package com.xwms.core.vas.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.vas.dto.VasOrderCreateRequest;
import com.xwms.core.vas.entity.*;
import com.xwms.core.vas.service.VasOrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** VAS增值服务 Controller */
@Tag(name = "VAS增值服务", description = "服务定义/工单管理/物料领用")
@RestController
@RequestMapping("/api/vas")
@RequiredArgsConstructor
public class VasController {

    private final VasOrderService vasOrderService;

    // ============================================================

    // VAS服务定义
    // ============================================================

    @Operation(summary = "创建VAS服务定义")
    @PostMapping("/services")
    public Result<VasService> createService(@RequestBody VasService service) {
        return Result.success(vasOrderService.createService(service));
    }

    @Operation(summary = "查询VAS服务详情")
    @GetMapping("/services/{id}")
    public Result<VasService> getService(@PathVariable Long id) {
        return Result.success(vasOrderService.getService(id));
    }

    @Operation(summary = "分页查询VAS服务")
    @GetMapping("/services")
    public Result<Page<VasService>> pageServices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return Result.success(vasOrderService.pageServices(new Page<>(page, size), type, status));
    }

    // ============================================================

    // VAS工单
    // ============================================================

    @Operation(summary = "创建VAS工单")
    @PostMapping("/orders")
    public Result<VasOrder> createOrder(@RequestBody VasOrderCreateRequest request) {
        return Result.success(vasOrderService.createOrder(request));
    }

    @Operation(summary = "分配工单")
    @PutMapping("/orders/{id}/assign")
    public Result<VasOrder> assignOrder(@PathVariable Long id, @RequestParam String assignee) {
        return Result.success(vasOrderService.assignOrder(id, assignee));
    }

    @Operation(summary = "开始执行工单")
    @PutMapping("/orders/{id}/start")
    public Result<VasOrder> startOrder(@PathVariable Long id) {
        return Result.success(vasOrderService.startOrder(id));
    }

    @Operation(summary = "暂停工单")
    @PutMapping("/orders/{id}/pause")
    public Result<VasOrder> pauseOrder(@PathVariable Long id, @RequestParam String reason) {
        return Result.success(vasOrderService.pauseOrder(id, reason));
    }

    @Operation(summary = "恢复工单")
    @PutMapping("/orders/{id}/resume")
    public Result<VasOrder> resumeOrder(@PathVariable Long id) {
        return Result.success(vasOrderService.resumeOrder(id));
    }

    @Operation(summary = "完成工单明细")
    @PutMapping("/items/{itemId}/complete")
    public Result<VasOrderItem> completeItem(
            @PathVariable Long itemId,
            @RequestParam BigDecimal actualQty,
            @RequestParam(required = false) String afterSpec) {
        return Result.success(vasOrderService.completeItem(itemId, actualQty, afterSpec));
    }

    @Operation(summary = "完成工单")
    @PutMapping("/orders/{id}/complete")
    public Result<VasOrder> completeOrder(
            @PathVariable Long id, @RequestParam(required = false) BigDecimal actualQty) {
        return Result.success(vasOrderService.completeOrder(id, actualQty));
    }

    @Operation(summary = "取消工单")
    @PutMapping("/orders/{id}/cancel")
    public Result<VasOrder> cancelOrder(@PathVariable Long id, @RequestParam String reason) {
        return Result.success(vasOrderService.cancelOrder(id, reason));
    }

    @Operation(summary = "工单异常")
    @PutMapping("/orders/{id}/exception")
    public Result<VasOrder> exceptionOrder(@PathVariable Long id, @RequestParam String reason) {
        return Result.success(vasOrderService.exceptionOrder(id, reason));
    }

    @Operation(summary = "查询工单详情")
    @GetMapping("/orders/{id}")
    public Result<VasOrder> getOrder(@PathVariable Long id) {
        return Result.success(vasOrderService.getOrder(id));
    }

    @Operation(summary = "分页查询工单")
    @GetMapping("/orders")
    public Result<Page<VasOrder>> pageOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String serviceCode) {
        return Result.success(
                vasOrderService.pageOrders(new Page<>(page, size), status, type, serviceCode));
    }

    @Operation(summary = "查询工单明细")
    @GetMapping("/orders/{id}/items")
    public Result<List<VasOrderItem>> getOrderItems(@PathVariable Long id) {
        return Result.success(vasOrderService.getOrderItems(id));
    }

    @Operation(summary = "查询工单物料")
    @GetMapping("/orders/{id}/materials")
    public Result<List<VasMaterial>> getOrderMaterials(@PathVariable Long id) {
        return Result.success(vasOrderService.getOrderMaterials(id));
    }

    // ============================================================

    // 物料领用
    // ============================================================

    @Operation(summary = "领用物料")
    @PutMapping("/materials/{id}/pick")
    public Result<VasMaterial> pickMaterial(@PathVariable Long id, @RequestParam String pickBy) {
        return Result.success(vasOrderService.pickMaterial(id, pickBy));
    }

    @Operation(summary = "消耗物料")
    @PutMapping("/materials/{id}/consume")
    public Result<VasMaterial> consumeMaterial(
            @PathVariable Long id, @RequestParam BigDecimal actualQty) {
        return Result.success(vasOrderService.consumeMaterial(id, actualQty));
    }
}
