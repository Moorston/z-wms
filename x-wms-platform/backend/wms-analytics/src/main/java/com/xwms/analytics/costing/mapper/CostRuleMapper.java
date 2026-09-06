package com.xwms.analytics.costing.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.analytics.costing.entity.CostRule;

@Mapper
public interface CostRuleMapper extends BaseMapper<CostRule> {

    @Select("SELECT * FROM wms_cost_rule WHERE rule_code = #{ruleCode}")
    CostRule selectByRuleCode(@Param("ruleCode") String ruleCode);

    /** 鍖归厤SKU鐨勬垚鏈鍒? */
    @Select(
            "SELECT * FROM wms_cost_rule WHERE status = 'ACTIVE' AND (sku_code = #{skuCode} OR sku_code IS NULL) AND (category_code = #{categoryCode} OR category_code IS NULL) AND (owner_code = #{ownerCode} OR owner_code IS NULL) ORDER BY priority DESC LIMIT 1")
    CostRule matchRule(
            @Param("skuCode") String skuCode,
            @Param("categoryCode") String categoryCode,
            @Param("ownerCode") String ownerCode);
}
