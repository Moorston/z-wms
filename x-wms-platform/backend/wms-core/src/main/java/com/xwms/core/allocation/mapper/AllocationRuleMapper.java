package com.xwms.core.allocation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.allocation.entity.AllocationRule;

@Mapper
public interface AllocationRuleMapper extends BaseMapper<AllocationRule> {

    @Select("SELECT * FROM wms_allocation_rule WHERE rule_code = #{ruleCode}")
    AllocationRule selectByRuleCode(@Param("ruleCode") String ruleCode);

    @Select(
            "SELECT * FROM wms_allocation_rule WHERE status = 'ACTIVE' AND (warehouse_code = #{warehouseCode} OR warehouse_code IS NULL) AND (owner_code = #{ownerCode} OR owner_code IS NULL) AND (category_code = #{categoryCode} OR category_code IS NULL) AND (sku_code = #{skuCode} OR sku_code IS NULL) AND (order_type = #{orderType} OR order_type IS NULL) ORDER BY priority DESC LIMIT 1")
    AllocationRule matchRule(
            @Param("warehouseCode") String warehouseCode,
            @Param("ownerCode") String ownerCode,
            @Param("categoryCode") String categoryCode,
            @Param("skuCode") String skuCode,
            @Param("orderType") String orderType);
}
