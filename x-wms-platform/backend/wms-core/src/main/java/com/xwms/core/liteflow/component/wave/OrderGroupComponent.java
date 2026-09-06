package com.xwms.core.liteflow.component.wave;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.WaveExecuteContext;
import com.xwms.core.outbound.entity.OutboundOrder;

import lombok.extern.slf4j.Slf4j;

/** 订单分组组件 按库位区域、商品类型等维度对订单进行分组，提高拣货效率 */
@Slf4j
@LiteflowComponent("orderGroup")
public class OrderGroupComponent extends NodeComponent {

    @Override
    public void process() {
        WaveExecuteContext context = this.getContextBean(WaveExecuteContext.class);
        List<OutboundOrder> orders = context.getOrders();

        log.info("[波次流程] 订单分组开始, 订单数={}", orders.size());

        // TODO: 按波次规则进行分组
        // 分组维度：库区、商品类型、订单优先级、拣货模式
        Map<String, List<OutboundOrder>> groups =
                orders.stream()
                        .collect(
                                Collectors.groupingBy(
                                        o ->
                                                o.getWarehouseCode() != null
                                                        ? o.getWarehouseCode()
                                                        : "DEFAULT"));

        context.getExtParams().put("orderGroups", groups);
        log.info("[波次流程] 订单分组完成, 组数={}", groups.size());
    }
}
