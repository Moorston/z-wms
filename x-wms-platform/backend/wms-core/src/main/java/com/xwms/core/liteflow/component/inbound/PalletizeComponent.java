package com.xwms.core.liteflow.component.inbound;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.InboundContext;

import lombok.extern.slf4j.Slf4j;

/** 码盘组件 质检合格后，将商品码放到托盘上 */
@Slf4j
@LiteflowComponent("palletize")
public class PalletizeComponent extends NodeComponent {

    @Override
    public void process() {
        InboundContext context = this.getContextBean(InboundContext.class);

        log.info("[入库流程] 码盘开始");

        // TODO: 码盘逻辑
        // 1. 按码盘规则组盘（同SKU/同批次/同库位）
        // 2. 生成托盘号
        // 3. 记录托盘明细

        log.info("[入库流程] 码盘完成, 托盘数={}", context.getPallets().size());
    }
}
