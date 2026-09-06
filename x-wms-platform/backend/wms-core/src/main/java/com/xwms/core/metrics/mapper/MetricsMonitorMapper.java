package com.xwms.core.metrics.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.metrics.entity.MetricsMonitor;

@Mapper
public interface MetricsMonitorMapper extends BaseMapper<MetricsMonitor> {
    @Select("SELECT * FROM wms_metrics_monitor WHERE monitor_id = #{monitorId}")
    MetricsMonitor selectByMonitorId(@Param("monitorId") String monitorId);

    @Select("SELECT * FROM wms_metrics_monitor WHERE metric_id = #{metricId} AND is_active = 'Y'")
    MetricsMonitor selectActiveByMetric(@Param("metricId") String metricId);

    @Select(
            "SELECT * FROM wms_metrics_monitor WHERE warehouse_code = #{warehouseCode} AND is_active = 'Y' ORDER BY metric_code")
    List<MetricsMonitor> selectActiveByWarehouse(@Param("warehouseCode") String warehouseCode);

    @Select(
            "SELECT * FROM wms_metrics_monitor WHERE status = #{status} AND is_active = 'Y' ORDER BY last_check_time")
    List<MetricsMonitor> selectActiveByStatus(@Param("status") String status);
}
