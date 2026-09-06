package com.xwms.common.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.xwms.common.core.Result;
import com.xwms.common.feign.config.FeignConfig;
import com.xwms.common.feign.dto.ProductDTO;
import com.xwms.common.feign.fallback.ProductFeignFallbackFactory;

/** 商品档案Feign客户端 wms-core调用wms-base获取商品信息 */
@FeignClient(
        name = "wms-base",
        contextId = "productFeignClient",
        path = "/api/product",
        configuration = FeignConfig.class,
        fallbackFactory = ProductFeignFallbackFactory.class)
public interface ProductFeignClient {

    @GetMapping("/{sku}")
    Result<ProductDTO> getBySku(@PathVariable("sku") String sku);

    @GetMapping("/page")
    Result<?> page(
            @RequestParam(value = "sku", required = false) String sku,
            @RequestParam(value = "ownerCode", required = false) String ownerCode,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize);
}
