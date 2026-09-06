package com.xwms.core.putawayrule.strategy.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.xwms.core.putawayrule.dto.PutawayRecommendContext;
import com.xwms.core.putawayrule.entity.PutawayRuleLine;
import com.xwms.core.putawayrule.service.LocationQueryService;
import com.xwms.core.putawayrule.strategy.RuleCodeStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 规则代码03：指定库区随机分配 在指定库区内查询所有可用库位，按上架顺序排序 最常用的上架策略，适用于分区随机存储场景 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleCode03Strategy implements RuleCodeStrategy {

    private final LocationQueryService locationQueryService;

    @Override
    public String getRuleCode() {
        return "03";
    }

    @Override
    public String getDescription() {
        return "指定库区随机分配（在目标库区内按上架顺序查找可用库位）";
    }

    @Override
    public List<String> getCandidateLocations(
            PutawayRuleLine line, PutawayRecommendContext context) {
        String targetZone = line.getTargetZone();
        if (targetZone == null || targetZone.isEmpty()) {
            log.warn("规则代码03缺少目标库区配置: ruleLineId={}", line.getId());
            return List.of();
        }

        // 查询目标库区内的可用库位（按上架顺序排序）
        List<LocationQueryService.PutawayLocation> locations =
                locationQueryService.queryAvailableLocations(
                        context.getWarehouseCode(),
                        targetZone,
                        null,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null);

        log.debug("规则代码03候选库位: zone={}, count={}", targetZone, locations.size());

        return locations.stream()
                .map(LocationQueryService.PutawayLocation::getLocationCode)
                .collect(Collectors.toList());
    }
}
