package com.xwms.core.liteflow.component.wave;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.WaveExecuteContext;

import lombok.extern.slf4j.Slf4j;

/** 波次初始化组件 加载波次信息和待处理出库单 */
@Slf4j
@LiteflowComponent("waveInit")
public class WaveInitComponent extends NodeComponent {

    @Override
    public void process() {
        WaveExecuteContext context = this.getContextBean(WaveExecuteContext.class);
        log.info("[波次流程] 初始化波次: waveNo={}", context.getWaveNo());

        // TODO: 从数据库加载波次信息和关联的出库单
        // Wave wave = waveMapper.selectById(context.getWaveId());
        // List<OutboundOrder> orders = outboundOrderMapper.selectByWaveId(context.getWaveId());
        // context.setWave(wave);
        // context.setOrders(orders);

        context.setIndustryType("GENERAL"); // 默认为通用行业
        log.info("[波次流程] 初始化完成, 订单数={}", context.getOrders().size());
    }
}
