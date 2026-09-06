package com.xwms.core.dataquality.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.dataquality.entity.DqRule;

@Mapper
public interface DqRuleMapper extends BaseMapper<DqRule> {
    @Select("SELECT * FROM wms_dq_rule WHERE rule_id = #{ruleId}")
    DqRule selectByRuleId(@Param("ruleId") String ruleId);

    @Select("SELECT * FROM wms_dq_rule WHERE rule_code = #{ruleCode}")
    DqRule selectByRuleCode(@Param("ruleCode") String ruleCode);

    @Select(
            "SELECT * FROM wms_dq_rule WHERE rule_type = #{ruleType} AND is_active = 'Y' ORDER BY sort_order")
    List<DqRule> selectActiveByType(@Param("ruleType") String ruleType);

    @Select(
            "SELECT * FROM wms_dq_rule WHERE table_name = #{tableName} AND is_active = 'Y' ORDER BY sort_order")
    List<DqRule> selectActiveByTable(@Param("tableName") String tableName);

    @Select("SELECT * FROM wms_dq_rule WHERE is_active = 'Y' ORDER BY rule_type, sort_order")
    List<DqRule> selectAllActive();
}
