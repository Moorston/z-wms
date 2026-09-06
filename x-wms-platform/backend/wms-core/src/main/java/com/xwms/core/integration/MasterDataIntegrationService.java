package com.xwms.core.integration;

import org.springframework.stereotype.Service;

import com.xwms.common.core.Result;
import com.xwms.common.exception.BizException;
import com.xwms.common.feign.ProductFeignClient;
import com.xwms.common.feign.dto.ProductDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 主数据集成服务 封装wms-core对wms-base主数据的Feign调用 提供缓存、降级、校验等能力 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MasterDataIntegrationService {

    private final ProductFeignClient productFeignClient;

    /** 校验商品是否存在且有效 入库/出库创建时调用，确保SKU合法 */
    public ProductDTO validateProduct(String sku) {
        Result<ProductDTO> result = productFeignClient.getBySku(sku);
        if (result == null || result.getData() == null) {
            throw new BizException(30001, "商品不存在或已停用: " + sku);
        }
        ProductDTO product = result.getData();
        if (!"ACTIVE".equals(product.getStatus())) {
            throw new BizException(30002, "商品已停用: " + sku);
        }
        return product;
    }

    /** 获取商品信息（带降级处理） 当wms-base不可用时，返回null由调用方决定是否继续 */
    public ProductDTO getProductSafe(String sku) {
        try {
            Result<ProductDTO> result = productFeignClient.getBySku(sku);
            return result != null ? result.getData() : null;
        } catch (Exception e) {
            log.warn("获取商品信息失败（降级）: sku={}, error={}", sku, e.getMessage());
            return null;
        }
    }

    /** 校验商品温区与库位温区是否匹配 上架时调用，确保商品存放在正确温区 */
    public void validateTemperatureZone(String sku, String locationTemperatureZone) {
        ProductDTO product = getProductSafe(sku);
        if (product == null || product.getTemperatureZone() == null) {
            return; // 商品无温区要求，不校验
        }
        if (!product.getTemperatureZone().equals(locationTemperatureZone)) {
            throw new BizException(
                    30003,
                    String.format(
                            "商品温区(%s)与库位温区(%s)不匹配: sku=%s",
                            product.getTemperatureZone(), locationTemperatureZone, sku));
        }
    }

    /** 判断商品是否需要批次管理 */
    public boolean isBatchManaged(String sku) {
        ProductDTO product = getProductSafe(sku);
        return product != null && Boolean.TRUE.equals(product.getBatchManaged());
    }

    /** 获取商品的分配规则编码 */
    public String getAllocationRule(String sku) {
        ProductDTO product = getProductSafe(sku);
        return product != null ? product.getAllocationRule() : null;
    }

    /** 获取商品的上架规则编码 */
    public String getPutawayRule(String sku) {
        ProductDTO product = getProductSafe(sku);
        return product != null ? product.getPutawayRule() : null;
    }
}
