package com.xwms.core.liteflow.component.vas;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.vas.service.VasOrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow VAS组件 - 出入库流程中触发VAS加工 入库时: 贴标/重新包装等前置加工 出库时: 组合套装/定制化等后置加工 */
@Slf4j
@LiteflowComponent("vasTrigger")
@RequiredArgsConstructor
public class VasTriggerComponent extends NodeComponent {

    private final VasOrderService vasOrderService;

    @Override
    public void process() {
        // 从上下文获取VAS需求
        String serviceCode = this.getContextBean(String.class);
        if (serviceCode == null) {
            log.info("无VAS服务需求, 跳过");
            return;
        }
        log.info("触发VAS加工: serviceCode={}", serviceCode);
        // TODO: 根据上下文创建VAS工单并执行
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
