package com.xwms.core.abc.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.abc.entity.AbcRule;

@Mapper
public interface AbcRuleMapper extends BaseMapper<AbcRule> {

    @Select("SELECT * FROM wms_abc_rule WHERE rule_code = #{ruleCode}")
    AbcRule selectByRuleCode(@Param("ruleCode") String ruleCode);

    /** 匹配仓库的ABC分类规则 */
    @Select(
            "SELECT * FROM wms_abc_rule WHERE status = 'ACTIVE' AND (warehouse_code = #{warehouseCode} OR warehouse_code IS NULL) AND (owner_code = #{ownerCode} OR owner_code IS NULL) AND (category_code = #{categoryCode} OR category_code IS NULL) ORDER BY rule_code LIMIT 1")
    AbcRule matchRule(
            @Param("warehouseCode") String warehouseCode,
            @Param("ownerCode") String ownerCode,
            @Param("categoryCode") String categoryCode);
}
