package com.xwms.core.liteflow.component.plugin;

import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.plugin.service.PluginService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow行业插件执行组件 - 业务流程中执行行业插件 入库/出库/库存等业务节点触发行业特性校验和处理 */
@Slf4j
@LiteflowComponent("industryPlugin")
@RequiredArgsConstructor
public class IndustryPluginComponent extends NodeComponent {

    private final PluginService pluginService;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null || context.get("industry") == null) {
            log.info("无行业上下文, 跳过插件执行");
            return;
        }
        try {
            String industry = context.get("industry").toString();
            String triggerPoint =
                    context.get("triggerPoint") != null
                            ? context.get("triggerPoint").toString()
                            : this.getNodeId();
            String businessType =
                    context.get("businessType") != null
                            ? context.get("businessType").toString()
                            : null;
            String businessNo =
                    context.get("businessNo") != null ? context.get("businessNo").toString() : null;
            Object inputData = context.get("inputData");
            String traceId =
                    context.get("traceId") != null ? context.get("traceId").toString() : null;

            pluginService.executeIndustryPlugins(
                    industry, triggerPoint, businessType, businessNo, inputData, traceId);
            log.info("流程行业插件执行: 行业={}, 触发点={}", industry, triggerPoint);
        } catch (Exception e) {
            log.error("流程行业插件执行失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
