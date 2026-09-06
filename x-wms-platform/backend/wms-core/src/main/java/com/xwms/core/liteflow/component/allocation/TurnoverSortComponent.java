package com.xwms.core.liteflow.component.allocation;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.AllocationContext;

import lombok.extern.slf4j.Slf4j;

/** 周转规则排序组件 按周转规则对候选库存排序： - FIFO: 按入库时间升序 - FEFO: 按效期升序（先到期先出） - LIFO: 按入库时间降序 - 指定批次: 按指定批次优先 */
@Slf4j
@LiteflowComponent("turnoverSort")
public class TurnoverSortComponent extends NodeComponent {

    @Override
    public void process() {
        AllocationContext context = this.getContextBean(AllocationContext.class);

        log.info("[库存分配] 周转规则排序: strategy={}", context.getStrategy());

        // TODO: 查询该SKU在该仓库的所有库存
        // 按周转规则排序
        // FIFO: order by inbound_time asc
        // FEFO: order by expire_date asc
        // LIFO: order by inbound_time desc

        log.info("[库存分配] 周转规则排序完成, 候选数={}", context.getCandidateLocations().size());
    }
}
