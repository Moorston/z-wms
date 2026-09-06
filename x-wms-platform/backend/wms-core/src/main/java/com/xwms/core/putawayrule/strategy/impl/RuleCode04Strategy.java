package com.xwms.core.putawayrule.strategy.impl;

import java.util.List;

import org.springframework.stereotype.Component;

import com.xwms.core.putawayrule.dto.PutawayRecommendContext;
import com.xwms.core.putawayrule.entity.PutawayRuleLine;
import com.xwms.core.putawayrule.strategy.RuleCodeStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 规则代码04：兜底TEMP库位 所有规则都无可用库位时，返回临时库位（TEMP），避免上架任务无法执行 必须作为规则链的最后一行兜底规则 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleCode04Strategy implements RuleCodeStrategy {

    /** 默认临时库位编码 */
    private static final String DEFAULT_TEMP_LOCATION = "TEMP";

    @Override
    public String getRuleCode() {
        return "04";
    }

    @Override
    public String getDescription() {
        return "兜底TEMP库位（所有规则无结果时使用临时库位，必须作为规则链最后一行）";
    }

    @Override
    public List<String> getCandidateLocations(
            PutawayRuleLine line, PutawayRecommendContext context) {
        String targetLocation = line.getTargetLocation();
        if (targetLocation == null || targetLocation.isEmpty()) {
            targetLocation = DEFAULT_TEMP_LOCATION;
        }

        log.warn(
                "规则代码04兜底到临时库位: location={}, sku={}, warehouse={}",
                targetLocation,
                context.getSkuCode(),
                context.getWarehouseCode());

        return List.of(targetLocation);
    }
}
