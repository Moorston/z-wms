package com.xwms.core.metrics.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.metrics.entity.MetricsCalculation;

@Mapper
public interface MetricsCalculationMapper extends BaseMapper<MetricsCalculation> {
    @Select("SELECT * FROM wms_metrics_calculation WHERE calc_id = #{calcId}")
    MetricsCalculation selectByCalcId(@Param("calcId") String calcId);

    @Select(
            "SELECT * FROM wms_metrics_calculation WHERE metric_id = #{metricId} ORDER BY period_start DESC LIMIT #{limit}")
    List<MetricsCalculation> selectRecentByMetric(
            @Param("metricId") String metricId, @Param("limit") int limit);

    @Select(
            "SELECT * FROM wms_metrics_calculation WHERE warehouse_code = #{warehouseCode} AND period_start >= #{periodStart} AND period_end <= #{periodEnd} ORDER BY period_start")
    List<MetricsCalculation> selectByWarehouseAndPeriod(
            @Param("warehouseCode") String warehouseCode,
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd);

    @Select(
            "SELECT * FROM wms_metrics_calculation WHERE status = #{status} ORDER BY created_time DESC")
    List<MetricsCalculation> selectByStatus(@Param("status") String status);
}
