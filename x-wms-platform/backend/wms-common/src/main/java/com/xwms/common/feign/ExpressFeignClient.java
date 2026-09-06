package com.xwms.common.feign;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.xwms.common.core.Result;
import com.xwms.common.feign.config.FeignConfig;

/** 快递单Feign客户端 wms-core调用wms-integration获取快递单号/打印 */
@FeignClient(
        name = "wms-integration",
        contextId = "expressFeignClient",
        path = "/api/express",
        configuration = FeignConfig.class)
public interface ExpressFeignClient {

    /** 批量获取快递单号（异步） */
    @PostMapping("/batch-get")
    Result<String> batchGetTrackingNo(@RequestBody List<Map<String, Object>> orders);

    /** 查询批量获取结果 */
    @GetMapping("/batch-result/{batchId}")
    Result<Map<String, Object>> getBatchResult(@PathVariable("batchId") String batchId);

    /** 单个获取快递单号（同步） */
    @PostMapping("/get")
    Result<Map<String, Object>> getTrackingNo(@RequestBody Map<String, Object> order);

    /** 批量打印快递单 */
    @PostMapping("/batch-print")
    Result<String> batchPrint(@RequestBody List<String> orderNos);
}
