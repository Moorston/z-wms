package com.xwms.core.liteflow.component.wave;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.WaveExecuteContext;

import lombok.extern.slf4j.Slf4j;

/** 复核组件 拣货完成后，对商品进行数量和质量复核 */
@Slf4j
@LiteflowComponent("review")
public class ReviewComponent extends NodeComponent {

    @Override
    public void process() {
        WaveExecuteContext context = this.getContextBean(WaveExecuteContext.class);

        log.info("[波次流程] 复核开始, 订单数={}", context.getOrders().size());

        // TODO: 复核逻辑
        // 1. 扫描商品条码，核对SKU和数量
        // 2. 检查商品外观质量
        // 3. 差异处理（少拣/多拣/错拣）

        log.info("[波次流程] 复核完成");
    }
}
