package com.xwms.core.liteflow.component.label;

import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.print.service.PrintService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow标签打印组件 - 业务流程中自动打印标签 收货完成打印托盘标签/上架完成打印库位标签/打包完成打印快递单 */
@Slf4j
@LiteflowComponent("printLabel")
@RequiredArgsConstructor
public class PrintLabelComponent extends NodeComponent {

    private final PrintService printService;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null || context.get("templateCode") == null) {
            log.info("无打印上下文, 跳过");
            return;
        }
        try {
            String templateCode = context.get("templateCode").toString();
            String printerCode =
                    context.get("printerCode") != null
                            ? context.get("printerCode").toString()
                            : null;
            String businessType =
                    context.get("businessType") != null
                            ? context.get("businessType").toString()
                            : null;
            String businessNo =
                    context.get("businessNo") != null ? context.get("businessNo").toString() : null;
            Integer count =
                    context.get("printCount") != null
                            ? Integer.parseInt(context.get("printCount").toString())
                            : 1;
            String createdBy =
                    context.get("createdBy") != null
                            ? context.get("createdBy").toString()
                            : "SYSTEM";

            printService.createPrintTask(
                    templateCode,
                    businessType,
                    businessNo,
                    printerCode,
                    count,
                    context.toString(),
                    createdBy);
            log.info("流程标签打印: 模板={}, 业务={}", templateCode, businessNo);
        } catch (Exception e) {
            log.error("流程标签打印失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
