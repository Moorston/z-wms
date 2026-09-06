package com.xwms.core.rotation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.rotation.entity.RotationRule;

@Mapper
public interface RotationRuleMapper extends BaseMapper<RotationRule> {

    @Select("SELECT * FROM wms_rotation_rule WHERE rule_code = #{ruleCode}")
    RotationRule selectByRuleCode(@Param("ruleCode") String ruleCode);

    /** 按SKU/品类/货主匹配周转规则（优先级最高的） */
    @Select(
            "SELECT * FROM wms_rotation_rule WHERE status = 'ACTIVE' AND (sku_code = #{skuCode} OR sku_code IS NULL) AND (category_code = #{categoryCode} OR category_code IS NULL) AND (owner_code = #{ownerCode} OR owner_code IS NULL) ORDER BY priority DESC LIMIT 1")
    RotationRule matchRule(
            @Param("skuCode") String skuCode,
            @Param("categoryCode") String categoryCode,
            @Param("ownerCode") String ownerCode);
}
