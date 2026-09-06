package com.xwms.core.putawayrule.strategy.impl;

import java.util.List;

import org.springframework.stereotype.Component;

import com.xwms.core.putawayrule.dto.PutawayRecommendContext;
import com.xwms.core.putawayrule.entity.PutawayRuleLine;
import com.xwms.core.putawayrule.strategy.RuleCodeStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 规则代码07：固定库位（产品档案指定上架库位） 直接返回产品档案中配置的固定上架库位 适用于品种少、批量大、高频拣选SKU */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleCode07Strategy implements RuleCodeStrategy {

    @Override
    public String getRuleCode() {
        return "07";
    }

    @Override
    public String getDescription() {
        return "固定库位（产品档案指定上架库位，直接上架到指定位置）";
    }

    @Override
    public List<String> getCandidateLocations(
            PutawayRuleLine line, PutawayRecommendContext context) {
        String targetLocation = line.getTargetLocation();
        if (targetLocation == null || targetLocation.isEmpty()) {
            // 如果规则行未指定，则从产品档案获取（实际项目中通过Feign调用产品服务）
            log.warn("规则代码07缺少目标库位配置，需从产品档案获取: sku={}", context.getSkuCode());
            // TODO: 从产品档案获取固定上架库位
            return List.of();
        }

        log.debug("规则代码07固定库位: location={}", targetLocation);
        return List.of(targetLocation);
    }
}
