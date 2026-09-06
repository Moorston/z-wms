package com.xwms.core.liteflow.component.wave;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeSwitchComponent;

import com.xwms.core.liteflow.context.WaveExecuteContext;

import lombok.extern.slf4j.Slf4j;

/** 行业分支选择器 根据货主行业类型选择不同的质检流程： - PHARMA(医药) → gspQc（GSP质检） - 其他 → review（直接复核） */
@Slf4j
@LiteflowComponent("industrySwitch")
public class IndustrySwitchComponent extends NodeSwitchComponent {

    @Override
    public String processSwitch() {
        WaveExecuteContext context = this.getContextBean(WaveExecuteContext.class);
        String industry = context.getIndustryType();

        log.info("[波次流程] 行业分支选择: industry={}", industry);

        // 医药行业走GSP质检，其他行业直接复核
        if ("PHARMA".equalsIgnoreCase(industry)) {
            return "gspQc";
        }
        return "review";
    }
}
