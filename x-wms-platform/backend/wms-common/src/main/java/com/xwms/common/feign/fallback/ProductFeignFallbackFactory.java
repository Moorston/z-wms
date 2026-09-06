package com.xwms.common.feign.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import com.xwms.common.core.Result;
import com.xwms.common.feign.ProductFeignClient;
import com.xwms.common.feign.dto.ProductDTO;

import lombok.extern.slf4j.Slf4j;

/** 商品Feign降级工厂 */
@Slf4j
@Component
public class ProductFeignFallbackFactory implements FallbackFactory<ProductFeignClient> {

    @Override
    public ProductFeignClient create(Throwable cause) {
        log.error("ProductFeignClient调用失败，触发降级", cause);
        return new ProductFeignClient() {
            @Override
            public Result<ProductDTO> getBySku(String sku) {
                log.warn("商品查询降级: sku={}", sku);
                // 降级返回null，调用方需处理null情况
                return Result.success(null);
            }

            @Override
            public Result<?> page(
                    String sku,
                    String ownerCode,
                    String status,
                    Integer pageNum,
                    Integer pageSize) {
                return Result.success(null);
            }
        };
    }
}
