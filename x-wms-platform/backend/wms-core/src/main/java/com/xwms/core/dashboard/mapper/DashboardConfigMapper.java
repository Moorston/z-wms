package com.xwms.core.dashboard.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.dashboard.entity.DashboardConfig;

@Mapper
public interface DashboardConfigMapper extends BaseMapper<DashboardConfig> {
    @Select("SELECT * FROM wms_dashboard_config WHERE config_id = #{configId}")
    DashboardConfig selectByConfigId(@Param("configId") String configId);

    @Select("SELECT * FROM wms_dashboard_config WHERE config_code = #{configCode}")
    DashboardConfig selectByConfigCode(@Param("configCode") String configCode);

    @Select(
            "SELECT * FROM wms_dashboard_config WHERE dashboard_type = #{dashboardType} AND is_active = 'Y' ORDER BY sort_order")
    List<DashboardConfig> selectActiveByType(@Param("dashboardType") String dashboardType);

    @Select(
            "SELECT * FROM wms_dashboard_config WHERE warehouse_code = #{warehouseCode} AND is_default = 'Y' AND is_active = 'Y'")
    DashboardConfig selectDefaultByWarehouse(@Param("warehouseCode") String warehouseCode);
}
