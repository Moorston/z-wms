package com.xwms.base.batch.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.batch.entity.BatchTraceRule;

@Mapper
public interface BatchTraceRuleMapper extends BaseMapper<BatchTraceRule> {

    @Select(
            "SELECT * FROM wms_batch_trace_rule WHERE rule_type = #{ruleType} AND enabled = 1 ORDER BY rule_code")
    List<BatchTraceRule> selectByType(@Param("ruleType") String ruleType);
}
