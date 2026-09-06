package com.xwms.common.feign.fallback;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import com.xwms.common.core.Result;
import com.xwms.common.feign.client.DictFeignClient;

import lombok.extern.slf4j.Slf4j;

/** 数据字典Feign降级工厂 当wms-base不可用时，返回空结果或原值，不影响业务 */
@Slf4j
@Component
public class DictFeignFallbackFactory implements FallbackFactory<DictFeignClient> {

    @Override
    public DictFeignClient create(Throwable cause) {
        log.error("数据字典Feign调用失败: {}", cause.getMessage());
        return new DictFeignClient() {
            @Override
            public Result<List<Map<String, Object>>> listItems(String dictCode) {
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<String> translate(String dictCode, String itemValue) {
                // 降级：返回原值
                return Result.success(itemValue);
            }

            @Override
            public Result<Map<String, String>> translateMap(String dictCode) {
                return Result.success(Collections.emptyMap());
            }
        };
    }
}
