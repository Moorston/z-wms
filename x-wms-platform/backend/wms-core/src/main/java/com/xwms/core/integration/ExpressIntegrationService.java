package com.xwms.core.integration;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.xwms.common.core.Result;
import com.xwms.common.feign.ExpressFeignClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 快递单集成服务 封装wms-core对wms-integration快递单服务的Feign调用 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpressIntegrationService {

    private final ExpressFeignClient expressFeignClient;

    /** 批量获取快递单号（异步，走Kafka） 大促场景下，1000+订单批量取号 */
    public String batchGetTrackingNo(List<Map<String, Object>> orders) {
        log.info("批量获取快递单号: count={}", orders.size());
        Result<String> result = expressFeignClient.batchGetTrackingNo(orders);
        return result != null ? result.getData() : null;
    }

    /** 查询批量取号结果 */
    public Map<String, Object> getBatchResult(String batchId) {
        Result<Map<String, Object>> result = expressFeignClient.getBatchResult(batchId);
        return result != null ? result.getData() : null;
    }

    /** 单个获取快递单号（同步，实时性要求高的场景） */
    public Map<String, Object> getTrackingNoSync(Map<String, Object> order) {
        try {
            Result<Map<String, Object>> result = expressFeignClient.getTrackingNo(order);
            return result != null ? result.getData() : null;
        } catch (Exception e) {
            log.error("同步获取快递单号失败: order={}, error={}", order, e.getMessage());
            return null;
        }
    }

    /** 批量打印快递单 */
    public String batchPrint(List<String> orderNos) {
        log.info("批量打印快递单: count={}", orderNos.size());
        Result<String> result = expressFeignClient.batchPrint(orderNos);
        return result != null ? result.getData() : null;
    }
}
