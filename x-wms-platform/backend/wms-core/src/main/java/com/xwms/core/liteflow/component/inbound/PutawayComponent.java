package com.xwms.core.liteflow.component.inbound;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.InboundContext;

import lombok.extern.slf4j.Slf4j;

/** 上架组件 按上架规则将商品上架到指定库位 */
@Slf4j
@LiteflowComponent("putaway")
public class PutawayComponent extends NodeComponent {

    @Override
    public void process() {
        InboundContext context = this.getContextBean(InboundContext.class);

        log.info("[入库流程] 上架开始, 托盘数={}", context.getPallets().size());

        // TODO: 上架逻辑
        // 1. 按上架规则推荐库位（同SKU集中/就近/ABC分类）
        // 2. 生成上架任务
        // 3. 执行上架，更新库存
        // 4. 库存增加（Oracle原子操作）

        log.info("[入库流程] 上架完成");
    }
}
