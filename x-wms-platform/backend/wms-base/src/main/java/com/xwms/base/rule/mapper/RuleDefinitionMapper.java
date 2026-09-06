package com.xwms.base.rule.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.rule.entity.RuleDefinition;

@Mapper
public interface RuleDefinitionMapper extends BaseMapper<RuleDefinition> {

    @Select(
            "SELECT * FROM wms_rule_definition WHERE rule_type = #{ruleType} AND enabled = 1 ORDER BY priority")
    List<RuleDefinition> selectByType(@Param("ruleType") String ruleType);

    @Select("SELECT * FROM wms_rule_definition WHERE rule_code = #{code} AND version = #{version}")
    RuleDefinition selectByCodeAndVersion(
            @Param("code") String code, @Param("version") Integer version);

    @Select(
            "SELECT * FROM wms_rule_definition WHERE rule_code = #{code} ORDER BY version DESC LIMIT 1")
    RuleDefinition selectLatestByCode(@Param("code") String code);
}
