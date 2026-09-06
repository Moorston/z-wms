package com.xwms.core.liteflow.component.inbound;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.InboundContext;

import lombok.extern.slf4j.Slf4j;

/** 收货组件 实际收货操作，核对实物与ASN/入库单 */
@Slf4j
@LiteflowComponent("receive")
public class ReceiveComponent extends NodeComponent {

    @Override
    public void process() {
        InboundContext context = this.getContextBean(InboundContext.class);

        log.info("[入库流程] 收货开始: inboundNo={}", context.getInboundNo());

        // TODO: 收货逻辑
        // 1. 扫描商品条码
        // 2. 核对数量（应收vs实收）
        // 3. 差异处理（少收/多收/破损）
        // 4. 生成收货记录

        log.info("[入库流程] 收货完成, 明细数={}", context.getReceivedItems().size());
    }
}
