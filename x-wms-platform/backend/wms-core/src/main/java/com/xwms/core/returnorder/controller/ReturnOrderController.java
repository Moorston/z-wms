package com.xwms.core.returnorder.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.xwms.core.returnorder.entity.*;
import com.xwms.core.returnorder.service.ReturnOrderService;

import lombok.RequiredArgsConstructor;

/** 退货入库Controller */
@RestController
@RequestMapping("/api/return")
@RequiredArgsConstructor
public class ReturnOrderController {

    private final ReturnOrderService returnOrderService;

    // ==================== 退货单管理 ====================

    /** 创建退货单 */
    @PostMapping("/create")
    public ReturnOrder createReturn(
            @RequestBody ReturnOrder order,
            @RequestParam(required = false) List<ReturnOrderDetail> details) {
        return returnOrderService.createReturnOrder(order, details);
    }

    /** 退货收货 */
    @PostMapping("/{returnNo}/receive")
    public ReturnOrder receiveReturn(
            @PathVariable String returnNo,
            @RequestBody Map<String, BigDecimal> receiveQtys,
            @RequestParam(required = false, defaultValue = "system") String receiver) {
        return returnOrderService.receiveReturn(returnNo, receiveQtys, receiver);
    }

    /** 取消退货单 */
    @PostMapping("/{returnNo}/cancel")
    public Map<String, Object> cancelReturn(
            @PathVariable String returnNo,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        returnOrderService.cancelReturn(returnNo, operator);
        return Map.of("success", true, "message", "退货单已取消");
    }

    /** 查询退货单详情 */
    @GetMapping("/{returnNo}")
    public ReturnOrder getReturnOrder(@PathVariable String returnNo) {
        return returnOrderService.getReturnOrder(returnNo);
    }

    /** 查询退货单明细 */
    @GetMapping("/{returnNo}/details")
    public List<ReturnOrderDetail> getReturnDetails(@PathVariable String returnNo) {
        return returnOrderService.getReturnDetails(returnNo);
    }

    /** 查询所有退货单 */
    @GetMapping("/list")
    public List<ReturnOrder> getAllReturnOrders() {
        return returnOrderService.getAllReturnOrders();
    }

    // ==================== ASN编组 ====================

    /** 创建ASN编组 */
    @PostMapping("/group/create")
    public AsnGroup createAsnGroup(
            @RequestParam List<String> returnNos,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false, defaultValue = "STATIC") String sowingMode,
            @RequestParam(required = false, defaultValue = "system") String creator) {
        return returnOrderService.createAsnGroup(returnNos, groupName, sowingMode, creator);
    }

    /** 查询ASN编组详情 */
    @GetMapping("/group/{groupNo}")
    public AsnGroup getAsnGroup(@PathVariable String groupNo) {
        return returnOrderService.getAsnGroup(groupNo);
    }

    /** 查询ASN编组明细 */
    @GetMapping("/group/{groupNo}/details")
    public List<AsnGroupDetail> getAsnGroupDetails(@PathVariable String groupNo) {
        return returnOrderService.getAsnGroupDetails(groupNo);
    }

    // ==================== 播种初分 ====================

    /** 生成播种初分任务 */
    @PostMapping("/group/{groupNo}/first-sowing/generate")
    public List<SowingTask> generateFirstSowingTasks(
            @PathVariable String groupNo,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return returnOrderService.generateFirstSowingTasks(groupNo, operator);
    }

    /** 执行播种初分 */
    @PostMapping("/sowing/{taskNo}/first/execute")
    public SowingTask executeFirstSowing(
            @PathVariable String taskNo,
            @RequestParam String sowingLocation,
            @RequestParam BigDecimal sowedQty,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return returnOrderService.executeFirstSowing(taskNo, sowingLocation, sowedQty, operator);
    }

    // ==================== 播种二分 ====================

    /** 生成播种二分任务 */
    @PostMapping("/group/{groupNo}/second-sowing/generate")
    public List<SowingTask> generateSecondSowingTasks(
            @PathVariable String groupNo,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return returnOrderService.generateSecondSowingTasks(groupNo, operator);
    }

    /** 执行播种二分 */
    @PostMapping("/sowing/{taskNo}/second/execute")
    public SowingTask executeSecondSowing(
            @PathVariable String taskNo,
            @RequestParam String targetLocation,
            @RequestParam BigDecimal sowedQty,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return returnOrderService.executeSecondSowing(taskNo, targetLocation, sowedQty, operator);
    }

    // ==================== 动态播种 ====================

    /** 动态播种 */
    @PostMapping("/group/{groupNo}/dynamic-sowing")
    public SowingTask dynamicSowing(
            @PathVariable String groupNo,
            @RequestParam String skuCode,
            @RequestParam String sowingLocation,
            @RequestParam BigDecimal qty,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return returnOrderService.dynamicSowing(groupNo, skuCode, sowingLocation, qty, operator);
    }

    // ==================== 查询方法 ====================

    /** 查询播种任务 */
    @GetMapping("/group/{groupNo}/sowing-tasks")
    public List<SowingTask> getSowingTasks(@PathVariable String groupNo) {
        return returnOrderService.getSowingTasks(groupNo);
    }
}
