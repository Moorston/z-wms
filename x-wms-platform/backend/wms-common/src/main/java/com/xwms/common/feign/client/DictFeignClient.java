package com.xwms.common.feign.client;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.xwms.common.core.Result;
import com.xwms.common.feign.fallback.DictFeignFallbackFactory;

/** 数据字典Feign客户端 供wms-core/wms-analytics/wms-integration调用wms-base的字典服务 */
@FeignClient(
        name = "wms-base",
        path = "/api/dict",
        fallbackFactory = DictFeignFallbackFactory.class)
public interface DictFeignClient {

    /** 根据字典类型编码查询字典项列表 */
    @GetMapping("/items/{dictCode}")
    Result<List<Map<String, Object>>> listItems(@PathVariable("dictCode") String dictCode);

    /** 字典翻译（value → label） */
    @GetMapping("/translate/{dictCode}/{itemValue}")
    Result<String> translate(
            @PathVariable("dictCode") String dictCode, @PathVariable("itemValue") String itemValue);

    /** 批量翻译（返回Map） */
    @GetMapping("/translate-map/{dictCode}")
    Result<Map<String, String>> translateMap(@PathVariable("dictCode") String dictCode);
}
