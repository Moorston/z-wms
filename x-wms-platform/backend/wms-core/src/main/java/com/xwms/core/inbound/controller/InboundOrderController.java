package com.xwms.core.inbound.controller;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.PageResult;
import com.xwms.common.core.Result;
import com.xwms.core.inbound.entity.InboundOrder;
import com.xwms.core.inbound.mapper.InboundOrderMapper;
import com.xwms.core.inbound.service.InboundOrderService;

import lombok.RequiredArgsConstructor;

/** 入库单管理Controller 入库流程：创建→确认收货→质检→上架完成 */
@RestController
@RequestMapping("/api/inbound")
@RequiredArgsConstructor
public class InboundOrderController {

    private final InboundOrderService inboundOrderService;
    private final InboundOrderMapper inboundOrderMapper;

    /** 创建入库单 */
    @PostMapping
    public Result<InboundOrder> create(@RequestBody InboundOrder order) {
        return Result.success(inboundOrderService.create(order));
    }

    /** 分页查询入库单 */
    @GetMapping("/page")
    public Result<PageResult<InboundOrder>> page(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String inboundType,
            @RequestParam(required = false) String warehouse,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<InboundOrder> page =
                inboundOrderMapper.selectPage(
                        new Page<>(pageNum, pageSize),
                        new LambdaQueryWrapper<InboundOrder>()
                                .like(orderNo != null, InboundOrder::getInboundNo, orderNo)
                                .eq(status != null, InboundOrder::getStatus, status)
                                .eq(inboundType != null, InboundOrder::getInboundType, inboundType)
                                .eq(warehouse != null, InboundOrder::getWarehouseCode, warehouse)
                                .orderByDesc(InboundOrder::getCreatedTime));
        return Result.success(
                PageResult.of(
                        page.getRecords(),
                        page.getTotal(),
                        (int) page.getCurrent(),
                        (int) page.getSize()));
    }

    /** 查询入库单详情 */
    @GetMapping("/{orderNo}")
    public Result<InboundOrder> getByOrderNo(@PathVariable String orderNo) {
        return Result.success(
                inboundOrderMapper.selectOne(
                        new LambdaQueryWrapper<InboundOrder>()
                                .eq(InboundOrder::getInboundNo, orderNo)));
    }

    /** 确认收货 */
    @PostMapping("/{orderNo}/receive")
    public Result<Void> confirmReceive(
            @PathVariable String orderNo, @RequestParam BigDecimal receivedQty) {
        inboundOrderService.confirmReceive(orderNo, receivedQty);
        return Result.success();
    }

    /** 上架完成 */
    @PostMapping("/{orderNo}/putaway")
    public Result<Void> completePutaway(
            @PathVariable String orderNo, @RequestParam BigDecimal putawayQty) {
        inboundOrderService.completePutaway(orderNo, putawayQty);
        return Result.success();
    }

    /** 取消入库单 */
    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancel(@PathVariable String orderNo) {
        inboundOrderService.cancel(orderNo);
        return Result.success();
    }
}
