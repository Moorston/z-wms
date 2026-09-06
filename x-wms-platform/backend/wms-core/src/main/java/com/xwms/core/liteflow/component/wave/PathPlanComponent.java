package com.xwms.core.liteflow.component.wave;

import java.util.ArrayList;
import java.util.List;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.WaveExecuteContext;

import lombok.extern.slf4j.Slf4j;

/** 拣货路径规划组件 基于库位分布和拣货模式，规划最优拣货路径，避免重走和拥堵 */
@Slf4j
@LiteflowComponent("pathPlan")
public class PathPlanComponent extends NodeComponent {

    @Override
    public void process() {
        WaveExecuteContext context = this.getContextBean(WaveExecuteContext.class);

        log.info("[波次流程] 拣货路径规划开始");

        // TODO: 调用路径规划算法
        // 1. 收集所有待拣库位
        // 2. 按S型/Z型/分区策略规划路径
        // 3. 考虑拥堵点和热门库位
        List<String> path = new ArrayList<>();
        // path = pathPlanner.plan(context.getOrders());
        context.setPickPath(path);

        log.info("[波次流程] 拣货路径规划完成, 路径点数={}", path.size());
    }
}
