package com.xwms.base.liteflow.component.product;

import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.base.product.entity.Product;
import com.xwms.base.product.service.ProductManagementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow产品校验组件 - 业务流程中校验产品状态和属性 适用于入库/出库/调拨等操作前的产品校验 */
@Slf4j
@LiteflowComponent("productValidate")
@RequiredArgsConstructor
public class ProductValidateComponent extends NodeComponent {

    private final ProductManagementService productManagementService;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null || context.get("skuCode") == null) {
            log.info("无产品校验上下文, 跳过");
            return;
        }
        try {
            String skuCode = context.get("skuCode").toString();
            Product product = productManagementService.getProductByCode(skuCode);

            if (product == null) {
                context.put("validateError", "产品不存在: " + skuCode);
                log.warn("产品校验失败: 不存在 {}", skuCode);
                return;
            }

            if (!"ACTIVE".equals(product.getStatus())) {
                context.put("validateError", "产品状态异常: " + product.getStatus());
                log.warn("产品校验失败: 状态异常 {}={}", skuCode, product.getStatus());
                return;
            }

            // 危险品提示
            if (product.getIsHazardous() != null && product.getIsHazardous() == 1) {
                context.put("validateWarning", "危险品作业需特殊处理: " + skuCode);
                log.warn("危险品作业提示: {}", skuCode);
            }

            // 易碎品提示
            if (product.getIsFragile() != null && product.getIsFragile() == 1) {
                context.put("validateWarning", "易碎品需轻拿轻放: " + skuCode);
            }

            // 批次管理标识
            if (product.getIsBatchMgmt() != null && product.getIsBatchMgmt() == 1) {
                context.put("batchRequired", true);
            }

            // 序列号管理标识
            if (product.getIsSerialMgmt() != null && product.getIsSerialMgmt() == 1) {
                context.put("serialRequired", true);
            }

            context.put("productValidated", true);
            context.put("productInfo", product);
            log.info("产品校验通过: {}", skuCode);
        } catch (Exception e) {
            log.error("产品校验失败: {}", e.getMessage());
            context.put("validateError", "校验异常: " + e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
