package com.xwms.analytics.kpi.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.analytics.kpi.entity.KpiDefine;

@Mapper
public interface KpiDefineMapper extends BaseMapper<KpiDefine> {

    @Select(
            "SELECT * FROM wms_kpi_define WHERE kpi_category = #{category} AND enabled = 1 ORDER BY sort_order")
    List<KpiDefine> selectByCategory(@Param("category") String category);

    @Select("SELECT * FROM wms_kpi_define WHERE enabled = 1 ORDER BY kpi_category, sort_order")
    List<KpiDefine> selectAllEnabled();
}
