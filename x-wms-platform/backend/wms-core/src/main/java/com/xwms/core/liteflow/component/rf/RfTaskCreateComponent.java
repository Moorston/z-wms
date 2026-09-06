package com.xwms.core.liteflow.component.rf;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.rf.entity.RfTask;
import com.xwms.core.rf.service.RfService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow RF组件 - 业务流程中自动创建RF任务 波次释放后自动创建拣货RF任务/收货完成后自动创建上架RF任务 */
@Slf4j
@LiteflowComponent("rfTaskCreate")
@RequiredArgsConstructor
public class RfTaskCreateComponent extends NodeComponent {

    private final RfService rfService;

    @Override
    public void process() {
        RfTask task = this.getContextBean(RfTask.class);
        if (task == null) {
            log.info("无RF任务上下文, 跳过");
            return;
        }
        try {
            RfTask created = rfService.createTask(task);
            log.info("自动创建RF任务: {}, 类型={}", created.getTaskNo(), task.getTaskType());
        } catch (Exception e) {
            log.error("自动创建RF任务失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
