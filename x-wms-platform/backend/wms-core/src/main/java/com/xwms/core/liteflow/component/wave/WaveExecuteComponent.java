package com.xwms.core.liteflow.component.wave;

import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.wave.entity.Wave;
import com.xwms.core.wave.service.WaveService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow波次执行组件 - 波次流程编排 包含: 分组→分配→生成任务→路径规划→拣货→完成 */
@Slf4j
@LiteflowComponent("waveExecute")
@RequiredArgsConstructor
public class WaveExecuteComponent extends NodeComponent {

    private final WaveService waveService;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null || context.get("waveNo") == null) {
            log.info("无波次执行上下文, 跳过");
            return;
        }
        try {
            String waveNo = context.get("waveNo").toString();
            String action =
                    context.get("action") != null ? context.get("action").toString() : "ALLOCATE";

            Wave wave = waveService.getWaveByNo(waveNo);
            if (wave == null) {
                context.put("waveError", "波次不存在: " + waveNo);
                log.warn("波次执行失败: 波次不存在 {}", waveNo);
                return;
            }

            switch (action) {
                case "ALLOCATE":
                    waveService.allocateWave(waveNo, "system");
                    context.put("waveStatus", "ALLOCATED");
                    log.info("波次分配: {}", waveNo);
                    break;
                case "GENERATE_TASKS":
                    String pickMode =
                            context.get("pickMode") != null
                                    ? context.get("pickMode").toString()
                                    : "PICK_BY_WAVE";
                    waveService.generatePickTasks(waveNo, pickMode);
                    context.put("waveStatus", "TASKS_GENERATED");
                    log.info("生成拣货任务: {}", waveNo);
                    break;
                case "PLAN_PATH":
                    waveService.planPath(waveNo);
                    context.put("waveStatus", "PATH_PLANNED");
                    log.info("规划拣货路径: {}", waveNo);
                    break;
                case "START":
                    String picker =
                            context.get("picker") != null
                                    ? context.get("picker").toString()
                                    : "system";
                    waveService.startWave(waveNo, picker);
                    context.put("waveStatus", "PICKING");
                    log.info("开始波次拣货: {}", waveNo);
                    break;
                case "COMPLETE":
                    waveService.completeWave(waveNo);
                    context.put("waveStatus", "PICKED");
                    log.info("完成波次拣货: {}", waveNo);
                    break;
                default:
                    context.put("waveStatus", wave.getStatus());
                    break;
            }

            context.put("waveExecuted", true);
        } catch (Exception e) {
            log.error("波次执行异常: {}", e.getMessage());
            context.put("waveError", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
