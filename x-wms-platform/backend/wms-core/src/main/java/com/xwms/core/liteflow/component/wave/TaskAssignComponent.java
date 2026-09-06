package com.xwms.core.liteflow.component.wave;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.WaveExecuteContext;
import com.xwms.core.operation.entity.WorkTask;

import lombok.extern.slf4j.Slf4j;

/** 拣货员分配组件 根据拣货员当前负载、位置、技能，将任务分配给最合适的拣货员 */
@Slf4j
@LiteflowComponent("taskAssign")
public class TaskAssignComponent extends NodeComponent {

    @Override
    public void process() {
        WaveExecuteContext context = this.getContextBean(WaveExecuteContext.class);

        log.info("[波次流程] 分配拣货员开始, 任务数={}", context.getWorkTasks().size());

        // TODO: 调用任务分配算法
        // 1. 获取在线拣货员列表
        // 2. 按负载均衡、就近原则分配
        for (WorkTask task : context.getWorkTasks()) {
            // task.setAssignee(pickerSelector.select(task));
            task.setStatus("PENDING");
        }

        log.info("[波次流程] 分配拣货员完成");
    }
}
