package com.xwms.core.liteflow.component.wave;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.WaveExecuteContext;

import lombok.extern.slf4j.Slf4j;

/** 快递单获取组件 批量获取快递单号，调用外部快递API */
@Slf4j
@LiteflowComponent("expressGet")
public class ExpressGetComponent extends NodeComponent {

    @Override
    public void process() {
        WaveExecuteContext context = this.getContextBean(WaveExecuteContext.class);

        log.info("[波次流程] 快递单获取开始, 订单数={}", context.getOrders().size());

        // TODO: 调用快递单获取服务（Kafka异步+限流）
        // expressGetService.batchGet(context.getOrders());

        log.info("[波次流程] 快递单获取完成");
    }

    @Override
    public boolean isContinueOnError() {
        return true; // 快递单获取失败不中断主流程，可后续重试
    }
}
