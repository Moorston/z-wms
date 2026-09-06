package com.xwms.core.datamart.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.datamart.entity.MetricDefine;

@Mapper
public interface MetricDefineMapper extends BaseMapper<MetricDefine> {
    @Select("SELECT * FROM wms_metric_define WHERE metric_id = #{metricId}")
    MetricDefine selectByMetricId(@Param("metricId") String metricId);

    @Select("SELECT * FROM wms_metric_define WHERE metric_code = #{metricCode}")
    MetricDefine selectByMetricCode(@Param("metricCode") String metricCode);

    @Select(
            "SELECT * FROM wms_metric_define WHERE mart_id = #{martId} AND is_active = 'Y' ORDER BY metric_category, sort_order")
    List<MetricDefine> selectActiveByMart(@Param("martId") String martId);

    @Select(
            "SELECT * FROM wms_metric_define WHERE metric_type = #{metricType} AND is_active = 'Y' ORDER BY sort_order")
    List<MetricDefine> selectActiveByType(@Param("metricType") String metricType);

    @Select(
            "SELECT * FROM wms_metric_define WHERE metric_category = #{metricCategory} AND is_active = 'Y' ORDER BY sort_order")
    List<MetricDefine> selectActiveByCategory(@Param("metricCategory") String metricCategory);
}
