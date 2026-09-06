package com.xwms.core.liteflow.component.allocation;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.AllocationContext;

import lombok.extern.slf4j.Slf4j;

/** 分配规则匹配组件 根据货主配置、订单类型，匹配适用的分配策略 */
@Slf4j
@LiteflowComponent("allocRuleMatch")
public class RuleMatchComponent extends NodeComponent {

    @Override
    public void process() {
        AllocationContext context = this.getContextBean(AllocationContext.class);

        log.info("[库存分配] 规则匹配开始: sku={}, strategy={}", context.getSku(), context.getStrategy());

        // TODO: 从规则引擎获取分配策略
        // 策略优先级：货主配置 > 订单类型 > 系统默认
        if (context.getStrategy() == null) {
            context.setStrategy("FIFO"); // 默认先进先出
        }

        log.info("[库存分配] 规则匹配完成, 策略={}", context.getStrategy());
    }
}
