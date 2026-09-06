package com.xwms.core.liteflow.component.inbound;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.InboundContext;

import lombok.extern.slf4j.Slf4j;

/** 直接上架组件（调拨入库免检） 调拨入库跳过质检，直接码盘上架 */
@Slf4j
@LiteflowComponent("directPutaway")
public class DirectPutawayComponent extends NodeComponent {

    @Override
    public void process() {
        InboundContext context = this.getContextBean(InboundContext.class);

        log.info("[入库流程-调拨] 直接上架开始（免检）");

        // 调拨入库：跳过质检，直接码盘+上架
        // TODO: 码盘 + 上架

        log.info("[入库流程-调拨] 直接上架完成");
    }
}
