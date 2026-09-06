package com.xwms.analytics.kpi.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.analytics.kpi.entity.KpiTarget;

@Mapper
public interface KpiTargetMapper extends BaseMapper<KpiTarget> {

    @Select(
            "SELECT * FROM wms_kpi_target WHERE kpi_code = #{kpiCode} AND target_period = #{period} AND period_value = #{periodValue} AND status = 'ACTIVE'")
    List<KpiTarget> selectActiveTargets(
            @Param("kpiCode") String kpiCode,
            @Param("period") String period,
            @Param("periodValue") String periodValue);
}
