package com.xwms.core.liteflow.component.inbound;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.InboundContext;

import lombok.extern.slf4j.Slf4j;

/** 退货质检组件（退货入库专用） 退货质检更严格，需要核对原出库单、检查商品完好度 */
@Slf4j
@LiteflowComponent("returnQc")
public class ReturnQcComponent extends NodeComponent {

    @Override
    public void process() {
        InboundContext context = this.getContextBean(InboundContext.class);

        log.info("[入库流程-退货] 退货质检开始");

        // TODO: 退货质检逻辑
        // 1. 核对原出库单和退货申请
        // 2. 检查商品外观/功能完好度
        // 3. 判定可二次销售/残次/报废
        // 4. 残次品入残次区

        log.info("[入库流程-退货] 退货质检完成");
    }
}
