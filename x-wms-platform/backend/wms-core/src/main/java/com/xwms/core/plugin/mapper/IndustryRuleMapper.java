package com.xwms.core.plugin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.plugin.entity.IndustryRule;

@Mapper
public interface IndustryRuleMapper extends BaseMapper<IndustryRule> {

    @Select(
            "SELECT * FROM wms_industry_rule WHERE industry = #{industry} AND trigger_event = #{event} AND enabled = 1 ORDER BY priority")
    List<IndustryRule> selectByIndustryAndEvent(
            @Param("industry") String industry, @Param("event") String event);
}
