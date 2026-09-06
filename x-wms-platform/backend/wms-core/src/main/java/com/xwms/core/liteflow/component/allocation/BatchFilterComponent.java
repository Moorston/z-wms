package com.xwms.core.liteflow.component.allocation;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.AllocationContext;

import lombok.extern.slf4j.Slf4j;

/** 批次属性过滤组件 按批次属性过滤： - 批次状态（正常/非冻结） - 效期（近效期批次优先/排除已过期） - 质检状态（已质检合格） - 货主批次隔离 */
@Slf4j
@LiteflowComponent("batchFilter")
public class BatchFilterComponent extends NodeComponent {

    @Override
    public void process() {
        AllocationContext context = this.getContextBean(AllocationContext.class);

        log.info("[库存分配] 批次过滤开始, 候选数={}", context.getCandidateLocations().size());

        // TODO: 按批次属性过滤
        // 1. 排除已过期批次
        // 2. 排除冻结批次
        // 3. 效期预警批次标记

        log.info("[库存分配] 批次过滤完成");
    }
}
