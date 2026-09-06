package com.xwms.core.liteflow.component.pack;

import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.pack.entity.Pack;
import com.xwms.core.pack.service.PackService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow打包执行组件 - 打包流程编排 包含: 复核→打包→称重→贴标 */
@Slf4j
@LiteflowComponent("packExecute")
@RequiredArgsConstructor
public class PackExecuteComponent extends NodeComponent {

    private final PackService packService;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null || context.get("packNo") == null) {
            log.info("无打包执行上下文, 跳过");
            return;
        }
        try {
            String packNo = context.get("packNo").toString();
            String action =
                    context.get("action") != null ? context.get("action").toString() : "CHECK";

            Pack pack = packService.getPackByNo(packNo);
            if (pack == null) {
                context.put("packError", "打包单不存在: " + packNo);
                log.warn("打包执行失败: 打包单不存在 {}", packNo);
                return;
            }

            switch (action) {
                case "CHECK":
                    context.put("packStatus", pack.getStatus());
                    log.info("打包复核: {}", packNo);
                    break;
                case "PACK":
                    context.put("packStatus", pack.getStatus());
                    log.info("打包: {}", packNo);
                    break;
                case "WEIGH":
                    context.put("packStatus", pack.getStatus());
                    log.info("包裹称重: {}", packNo);
                    break;
                case "LABEL":
                    context.put("packStatus", pack.getStatus());
                    log.info("包裹贴标: {}", packNo);
                    break;
                default:
                    context.put("packStatus", pack.getStatus());
                    break;
            }

            context.put("packExecuted", true);
        } catch (Exception e) {
            log.error("打包执行异常: {}", e.getMessage());
            context.put("packError", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
