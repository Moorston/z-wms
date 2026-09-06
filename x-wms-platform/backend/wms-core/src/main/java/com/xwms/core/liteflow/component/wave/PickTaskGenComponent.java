package com.xwms.core.liteflow.component.wave;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.WaveExecuteContext;
import com.xwms.core.operation.entity.WorkTask;

import lombok.extern.slf4j.Slf4j;

/** 拣货任务生成组件 根据路径规划结果，生成具体的拣货作业任务 */
@Slf4j
@LiteflowComponent("pickTaskGen")
public class PickTaskGenComponent extends NodeComponent {

    @Override
    public void process() {
        WaveExecuteContext context = this.getContextBean(WaveExecuteContext.class);

        log.info("[波次流程] 生成拣货任务开始, 路径点数={}", context.getPickPath().size());

        // TODO: 根据路径和订单明细生成拣货任务
        // 每个库位生成一个拣货任务，包含SKU、数量、库位
        for (String location : context.getPickPath()) {
            WorkTask task = new WorkTask();
            task.setTaskType("PICK");
            task.setStatus("PENDING");
            task.setFromLocation(location);
            context.addWorkTask(task);
        }

        log.info("[波次流程] 生成拣货任务完成, 任务数={}", context.getWorkTasks().size());
    }
}
