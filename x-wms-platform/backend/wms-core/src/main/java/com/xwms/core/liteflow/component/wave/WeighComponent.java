package com.xwms.core.liteflow.component.wave;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.WaveExecuteContext;

import lombok.extern.slf4j.Slf4j;

/** 称重组件 打包后称重，校验重量是否与预期一致 */
@Slf4j
@LiteflowComponent("weigh")
public class WeighComponent extends NodeComponent {

    @Override
    public void process() {
        WaveExecuteContext context = this.getContextBean(WaveExecuteContext.class);

        log.info("[波次流程] 称重开始");

        // TODO: 称重逻辑
        // 1. 读取电子秤数据
        // 2. 与预期重量对比
        // 3. 差异超阈值则报警

        log.info("[波次流程] 称重完成");
    }
}
