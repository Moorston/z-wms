package com.xwms.base.rule.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.rule.entity.RuleExecLog;

@Mapper
public interface RuleExecLogMapper extends BaseMapper<RuleExecLog> {

    @Select(
            "SELECT * FROM wms_rule_exec_log WHERE rule_code = #{ruleCode} ORDER BY exec_time DESC LIMIT 100")
    List<RuleExecLog> selectByRuleCode(@Param("ruleCode") String ruleCode);

    @Select(
            "SELECT * FROM wms_rule_exec_log WHERE biz_type = #{bizType} AND biz_no = #{bizNo} ORDER BY exec_time")
    List<RuleExecLog> selectByBiz(@Param("bizType") String bizType, @Param("bizNo") String bizNo);
}
