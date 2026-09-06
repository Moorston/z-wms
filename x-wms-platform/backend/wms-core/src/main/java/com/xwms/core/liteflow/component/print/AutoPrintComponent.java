package com.xwms.core.liteflow.component.print;

import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.print.service.PrintService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow打印组件 - 业务流程中自动触发打印 出库单完成后自动打印出库单/拣货单/标签 */
@Slf4j
@LiteflowComponent("autoPrint")
@RequiredArgsConstructor
public class AutoPrintComponent extends NodeComponent {

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
            String businessType =
                    context.get("businessType") != null
                            ? context.get("businessType").toString()
                            : null;
            String businessNo =
                    context.get("businessNo") != null ? context.get("businessNo").toString() : null;
            String printerCode =
                    context.get("printerCode") != null
                            ? context.get("printerCode").toString()
                            : null;
            Integer copies =
                    context.get("copies") != null
                            ? Integer.parseInt(context.get("copies").toString())
                            : null;
            String printData =
                    context.get("printData") != null ? context.get("printData").toString() : null;
            String createdBy =
                    context.get("createdBy") != null
                            ? context.get("createdBy").toString()
                            : "SYSTEM";

            printService.createPrintTask(
                    templateCode,
                    businessType,
                    businessNo,
                    printerCode,
                    copies,
                    printData,
                    createdBy);
            log.info("流程自动打印: 模板={}, 业务={}", templateCode, businessNo);
        } catch (Exception e) {
            log.error("流程自动打印失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
