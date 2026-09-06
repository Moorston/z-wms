package com.xwms.core.expiry.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.expiry.entity.ExpiryRule;

@Mapper
public interface ExpiryRuleMapper extends BaseMapper<ExpiryRule> {

    @Select("SELECT * FROM wms_expiry_rule WHERE rule_code = #{ruleCode}")
    ExpiryRule selectByRuleCode(@Param("ruleCode") String ruleCode);

    @Select(
            "SELECT * FROM wms_expiry_rule WHERE status = 'ACTIVE' AND (warehouse_code = #{warehouseCode} OR warehouse_code IS NULL) AND (owner_code = #{ownerCode} OR owner_code IS NULL) AND (category_code = #{categoryCode} OR category_code IS NULL) AND (sku_code = #{skuCode} OR sku_code IS NULL) ORDER BY priority DESC LIMIT 1")
    ExpiryRule matchRule(
            @Param("warehouseCode") String warehouseCode,
            @Param("ownerCode") String ownerCode,
            @Param("categoryCode") String categoryCode,
            @Param("skuCode") String skuCode);
}
