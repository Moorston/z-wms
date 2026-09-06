package com.xwms.base.rule.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.rule.entity.RuleParam;

@Mapper
public interface RuleParamMapper extends BaseMapper<RuleParam> {

    @Select("SELECT * FROM wms_rule_param WHERE rule_code = #{ruleCode} ORDER BY sort_order")
    List<RuleParam> selectByRuleCode(@Param("ruleCode") String ruleCode);
}
