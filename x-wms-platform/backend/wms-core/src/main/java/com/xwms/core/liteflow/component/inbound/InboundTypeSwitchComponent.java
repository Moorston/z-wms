package com.xwms.core.liteflow.component.inbound;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeSwitchComponent;

import com.xwms.core.liteflow.context.InboundContext;

import lombok.extern.slf4j.Slf4j;

/**
 * 入库类型分支选择器 根据入库类型选择不同的后续流程： - PURCHASE(采购) → qc（质检） - RETURN(退货) → returnQc（退货质检，更严格） -
 * TRANSFER(调拨) → directPutaway（免检直接上架） - CROSS_DOCK(越库) → crossDock（直接转出库）
 */
@Slf4j
@LiteflowComponent("inboundTypeSwitch")
public class InboundTypeSwitchComponent extends NodeSwitchComponent {

    @Override
    public String processSwitch() {
        InboundContext context = this.getContextBean(InboundContext.class);
        String type = context.getInboundType();

        log.info("[入库流程] 入库类型分支: type={}", type);

        return switch (type != null ? type : "PURCHASE") {
            case "RETURN" -> "returnQc";
            case "TRANSFER" -> "directPutaway";
            case "CROSS_DOCK" -> "crossDock";
            default -> "qc";
        };
    }
}
