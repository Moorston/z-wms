package com.xwms.core.inbound.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.PageQuery;
import com.xwms.common.core.PageResult;
import com.xwms.common.exception.BizException;
import com.xwms.common.exception.ErrorCode;
import com.xwms.core.inbound.entity.InboundOrder;
import com.xwms.core.inbound.mapper.InboundOrderMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 入库单服务 核心流程：创建 -> 收货 -> 质检 -> 上架 -> 完成 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboundOrderService {

    private final InboundOrderMapper inboundOrderMapper;

    /** 创建入库单 */
    @Transactional(rollbackFor = Exception.class)
    public InboundOrder create(InboundOrder order) {
        // 幂等检查
        InboundOrder exist =
                inboundOrderMapper.selectOne(
                        new LambdaQueryWrapper<InboundOrder>()
                                .eq(InboundOrder::getInboundNo, order.getInboundNo()));
        if (exist != null) {
            throw new BizException(ErrorCode.ORDER_ALREADY_EXISTS);
        }
        order.setStatus("CREATED");
        inboundOrderMapper.insert(order);
        log.info("入库单创建: orderNo={}, type={}", order.getInboundNo(), order.getInboundType());
        return order;
    }

    /** 确认收货 */
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(String orderNo, java.math.BigDecimal receivedQty) {
        InboundOrder order = getByOrderNo(orderNo);
        if (!"RECEIVING".equals(order.getStatus()) && !"CREATED".equals(order.getStatus())) {
            throw new BizException("入库单状态不允许收货: " + order.getStatus());
        }
        order.setReceivedQty(receivedQty);
        order.setReceiveTime(LocalDateTime.now());
        order.setStatus("RECEIVED");
        inboundOrderMapper.updateById(order);
        log.info("入库单收货完成: orderNo={}, qty={}", orderNo, receivedQty);
    }

    /** 完成上架 */
    @Transactional(rollbackFor = Exception.class)
    public void completePutaway(String orderNo, java.math.BigDecimal putawayQty) {
        InboundOrder order = getByOrderNo(orderNo);
        order.setPutawayQty(putawayQty);
        order.setStatus("COMPLETED");
        inboundOrderMapper.updateById(order);
        log.info("入库单完成: orderNo={}", orderNo);
    }

    /** 取消入库单 */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(String orderNo) {
        InboundOrder order = getByOrderNo(orderNo);
        if (!"CREATED".equals(order.getStatus())) {
            throw new BizException("只有已创建状态的入库单可以取消");
        }
        order.setStatus("CANCELLED");
        inboundOrderMapper.updateById(order);
    }

    /** 分页查询 */
    public PageResult<InboundOrder> page(PageQuery query) {
        Page<InboundOrder> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<InboundOrder> wrapper = new LambdaQueryWrapper<>();
        if (query.getKeyword() != null) {
            wrapper.like(InboundOrder::getInboundNo, query.getKeyword());
        }
        wrapper.orderByDesc(InboundOrder::getCreatedTime);
        Page<InboundOrder> result = inboundOrderMapper.selectPage(page, wrapper);
        return PageResult.of(
                result.getRecords(), result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    public InboundOrder getByOrderNo(String orderNo) {
        InboundOrder order =
                inboundOrderMapper.selectOne(
                        new LambdaQueryWrapper<InboundOrder>()
                                .eq(InboundOrder::getInboundNo, orderNo));
        if (order == null) {
            throw new BizException(ErrorCode.ASN_NOT_FOUND);
        }
        return order;
    }
}
