package com.xwms.core.metrics.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.metrics.entity.MetricsSystem;

@Mapper
public interface MetricsSystemMapper extends BaseMapper<MetricsSystem> {
    @Select("SELECT * FROM wms_metrics_system WHERE system_id = #{systemId}")
    MetricsSystem selectBySystemId(@Param("systemId") String systemId);

    @Select("SELECT * FROM wms_metrics_system WHERE system_code = #{systemCode}")
    MetricsSystem selectBySystemCode(@Param("systemCode") String systemCode);

    @Select(
            "SELECT * FROM wms_metrics_system WHERE system_type = #{systemType} AND is_active = 'Y' ORDER BY sort_order")
    List<MetricsSystem> selectActiveByType(@Param("systemType") String systemType);

    @Select(
            "SELECT * FROM wms_metrics_system WHERE warehouse_code = #{warehouseCode} AND is_default = 'Y' AND is_active = 'Y'")
    MetricsSystem selectDefaultByWarehouse(@Param("warehouseCode") String warehouseCode);
}
