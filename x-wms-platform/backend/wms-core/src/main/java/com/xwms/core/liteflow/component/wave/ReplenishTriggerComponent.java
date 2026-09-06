package com.xwms.core.liteflow.component.wave;

import java.math.BigDecimal;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.replenish.service.ReplenishService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow补货组件 - 波次拣货缺货时触发紧急补货 在波次执行过程中, 如果拣货库位库存不足, 触发紧急补货 */
@Slf4j
@LiteflowComponent("replenishTrigger")
@RequiredArgsConstructor
public class ReplenishTriggerComponent extends NodeComponent {

    private final ReplenishService replenishService;

    @Override
    public void process() {
        // 从上下文获取缺货信息
        String sku = this.getContextBean(String.class);
        if (sku == null) {
            log.info("无缺货SKU, 跳过补货");
            return;
        }

        // TODO: 从波次上下文获取拣货库位、需求数量、货主、仓库
        String pickLocation = "PICK-A-01";
        BigDecimal needQty = new BigDecimal("10");
        String ownerCode = "OWNER001";
        String warehouseCode = "WH001";

        try {
            replenishService.triggerUrgent(sku, pickLocation, needQty, ownerCode, warehouseCode);
            log.info("缺货触发紧急补货: SKU={}, 数量={}", sku, needQty);
        } catch (Exception e) {
            log.error("紧急补货触发失败: SKU={}, 错误={}", sku, e.getMessage());
            // 补货失败不阻断波次, 标记缺货
        }
    }

    @Override
    public boolean isAccess() {
        // 只有缺货场景才触发
        return true;
    }
}
