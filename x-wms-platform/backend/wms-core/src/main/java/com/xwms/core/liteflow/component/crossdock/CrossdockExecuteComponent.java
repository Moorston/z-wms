package com.xwms.core.liteflow.component.crossdock;

import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.crossdock.entity.Crossdock;
import com.xwms.core.crossdock.service.CrossdockService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow越库执行组件 - 越库流程编排 包含: 匹配→收货→分拣→发运 越库特点: 货物从入库月台直接转到出库月台，不经过存储环节 */
@Slf4j
@LiteflowComponent("crossdockExecute")
@RequiredArgsConstructor
public class CrossdockExecuteComponent extends NodeComponent {

    private final CrossdockService crossdockService;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null || context.get("crossdockNo") == null) {
            log.info("无越库执行上下文, 跳过");
            return;
        }
        try {
            String crossdockNo = context.get("crossdockNo").toString();
            String action =
                    context.get("action") != null ? context.get("action").toString() : "MATCH";

            Crossdock crossdock = crossdockService.getCrossdockByNo(crossdockNo);
            if (crossdock == null) {
                context.put("crossdockError", "越库单不存在: " + crossdockNo);
                log.warn("越库执行失败: 越库单不存在 {}", crossdockNo);
                return;
            }

            switch (action) {
                case "MATCH":
                    crossdockService.autoMatch(crossdockNo);
                    context.put("crossdockStatus", "MATCHED");
                    log.info("越库匹配: {}", crossdockNo);
                    break;
                case "RECEIVE":
                    context.put("crossdockStatus", crossdock.getStatus());
                    log.info("越库收货: {}", crossdockNo);
                    break;
                case "SORT":
                    context.put("crossdockStatus", crossdock.getStatus());
                    log.info("越库分拣: {}", crossdockNo);
                    break;
                case "SHIP":
                    context.put("crossdockStatus", crossdock.getStatus());
                    log.info("越库发运: {}", crossdockNo);
                    break;
                default:
                    context.put("crossdockStatus", crossdock.getStatus());
                    break;
            }

            context.put("crossdockExecuted", true);
        } catch (Exception e) {
            log.error("越库执行异常: {}", e.getMessage());
            context.put("crossdockError", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
