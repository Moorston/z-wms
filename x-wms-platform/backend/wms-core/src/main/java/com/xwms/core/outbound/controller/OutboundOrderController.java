package com.xwms.core.outbound.controller;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.PageResult;
import com.xwms.common.core.Result;
import com.xwms.core.outbound.entity.OutboundOrder;
import com.xwms.core.outbound.mapper.OutboundOrderMapper;
import com.xwms.core.outbound.service.OutboundOrderService;

import lombok.RequiredArgsConstructor;

/** 出库单管理Controller 出库流程：创建→分配预占→波次→拣货→复核→打包→发运扣减 */
@RestController
@RequestMapping("/api/outbound")
@RequiredArgsConstructor
public class OutboundOrderController {

    private final OutboundOrderService outboundOrderService;
    private final OutboundOrderMapper outboundOrderMapper;

    /** 创建出库单 */
    @PostMapping
    public Result<OutboundOrder> create(@RequestBody OutboundOrder order) {
        return Result.success(outboundOrderService.create(order));
    }

    /** 分页查询出库单 */
    @GetMapping("/page")
    public Result<PageResult<OutboundOrder>> page(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String outboundType,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) String waveNo,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<OutboundOrder> page =
                outboundOrderMapper.selectPage(
                        new Page<>(pageNum, pageSize),
                        new LambdaQueryWrapper<OutboundOrder>()
                                .like(orderNo != null, OutboundOrder::getOutboundNo, orderNo)
                                .eq(status != null, OutboundOrder::getStatus, status)
                                .eq(
                                        outboundType != null,
                                        OutboundOrder::getOutboundType,
                                        outboundType)
                                .eq(warehouse != null, OutboundOrder::getWarehouseCode, warehouse)
                                .eq(waveNo != null, OutboundOrder::getWaveNo, waveNo)
                                .orderByDesc(OutboundOrder::getCreatedTime));
        return Result.success(
                PageResult.of(
                        page.getRecords(),
                        page.getTotal(),
                        (int) page.getCurrent(),
                        (int) page.getSize()));
    }

    /** 查询出库单详情 */
    @GetMapping("/{orderNo}")
    public Result<OutboundOrder> getByOrderNo(@PathVariable String orderNo) {
        return Result.success(
                outboundOrderMapper.selectOne(
                        new LambdaQueryWrapper<OutboundOrder>()
                                .eq(OutboundOrder::getOutboundNo, orderNo)));
    }

    /** 分配库存（预占） */
    @PostMapping("/{orderNo}/allocate")
    public Result<Void> allocate(
            @PathVariable String orderNo,
            @RequestParam String sku,
            @RequestParam String warehouse,
            @RequestParam String locationCode,
            @RequestParam String batchNo,
            @RequestParam BigDecimal qty) {
        outboundOrderService.allocate(orderNo, sku, warehouse, locationCode, batchNo, qty);
        return Result.success();
    }

    /** 拣货确认 */
    @PostMapping("/{orderNo}/pick")
    public Result<Void> confirmPick(
            @PathVariable String orderNo, @RequestParam BigDecimal pickedQty) {
        outboundOrderService.confirmPick(orderNo, pickedQty);
        return Result.success();
    }

    /** 发运确认（实际扣减库存） */
    @PostMapping("/{orderNo}/ship")
    public Result<Void> confirmShip(
            @PathVariable String orderNo,
            @RequestParam String sku,
            @RequestParam String warehouse,
            @RequestParam String locationCode,
            @RequestParam String batchNo,
            @RequestParam BigDecimal qty) {
        outboundOrderService.confirmShip(orderNo, sku, warehouse, locationCode, batchNo, qty);
        return Result.success();
    }

    /** 取消出库单（释放预占） */
    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancel(
            @PathVariable String orderNo,
            @RequestParam String sku,
            @RequestParam String warehouse,
            @RequestParam String locationCode,
            @RequestParam String batchNo,
            @RequestParam BigDecimal qty) {
        outboundOrderService.cancel(orderNo, sku, warehouse, locationCode, batchNo, qty);
        return Result.success();
    }
}
