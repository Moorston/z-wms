package com.xwms.base.rule.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.rule.entity.RuleVersion;

@Mapper
public interface RuleVersionMapper extends BaseMapper<RuleVersion> {

    @Select("SELECT * FROM wms_rule_version WHERE rule_code = #{ruleCode} ORDER BY version DESC")
    List<RuleVersion> selectByRuleCode(@Param("ruleCode") String ruleCode);
}
