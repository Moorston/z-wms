package com.xwms.core.safety.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.safety.entity.SafetyRule;

@Mapper
public interface SafetyRuleMapper extends BaseMapper<SafetyRule> {

    @Select("SELECT * FROM wms_safety_rule WHERE rule_code = #{ruleCode}")
    SafetyRule selectByRuleCode(@Param("ruleCode") String ruleCode);

    /** 匹配SKU的安全库存规则 */
    @Select(
            "SELECT * FROM wms_safety_rule WHERE status = 'ACTIVE' AND (warehouse_code = #{warehouseCode} OR warehouse_code IS NULL) AND (owner_code = #{ownerCode} OR owner_code IS NULL) AND (sku_code = #{skuCode} OR sku_code IS NULL) AND (category_code = #{categoryCode} OR category_code IS NULL) AND (abc_class = #{abcClass} OR abc_class IS NULL) ORDER BY priority DESC LIMIT 1")
    SafetyRule matchRule(
            @Param("warehouseCode") String warehouseCode,
            @Param("ownerCode") String ownerCode,
            @Param("skuCode") String skuCode,
            @Param("categoryCode") String categoryCode,
            @Param("abcClass") String abcClass);
}
