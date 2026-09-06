package com.xwms.core.putawayrule.strategy;

import java.util.List;

import com.xwms.core.putawayrule.dto.PutawayRecommendContext;
import com.xwms.core.putawayrule.entity.PutawayRuleLine;

/** 规则代码策略接口（SPI扩展点） 每种规则代码（01-31）对应一个实现，负责获取候选库位清单 新增规则代码只需实现此接口并注册到工厂 */
public interface RuleCodeStrategy {

    /** 规则代码（如"03"、"07"、"13"） */
    String getRuleCode();

    /** 规则代码描述 */
    String getDescription();

    /**
     * 根据规则行和上架上下文获取候选库位清单
     *
     * @param line 规则行配置
     * @param context 上架推荐上下文
     * @return 候选库位编码列表（按上架顺序排序）
     */
    List<String> getCandidateLocations(PutawayRuleLine line, PutawayRecommendContext context);
}
