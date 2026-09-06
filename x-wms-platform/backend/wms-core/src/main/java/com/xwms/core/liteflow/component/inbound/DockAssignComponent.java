package com.xwms.core.liteflow.component.inbound;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.InboundContext;

import lombok.extern.slf4j.Slf4j;

/** 月台分配组件 为入库车辆分配收货月台 */
@Slf4j
@LiteflowComponent("dockAssign")
public class DockAssignComponent extends NodeComponent {

    @Override
    public void process() {
        InboundContext context = this.getContextBean(InboundContext.class);

        log.info("[入库流程] 月台分配开始");

        // TODO: 月台分配逻辑
        // 1. 查询空闲月台
        // 2. 按入库类型分配（冷藏车→冷链月台）
        // 3. 预约时间冲突检测

        context.setDockNo("DOCK-01");
        log.info("[入库流程] 月台分配完成: dockNo={}", context.getDockNo());
    }
}
