package com.xwms.core.dashboard.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.dashboard.entity.DashboardComponent;

@Mapper
public interface DashboardComponentMapper extends BaseMapper<DashboardComponent> {

    @Select("SELECT * FROM wms_dashboard_component WHERE component_code = #{componentCode}")
    DashboardComponent selectByComponentCode(@Param("componentCode") String componentCode);

    @Select(
            "SELECT * FROM wms_dashboard_component WHERE component_type = #{componentType} AND status = 'ACTIVE'")
    List<DashboardComponent> selectByType(@Param("componentType") String componentType);

    @Select(
            "SELECT * FROM wms_dashboard_component WHERE data_source = #{dataSource} AND status = 'ACTIVE'")
    List<DashboardComponent> selectByDataSource(@Param("dataSource") String dataSource);

    @Select(
            "SELECT * FROM wms_dashboard_component WHERE status = 'ACTIVE' ORDER BY component_type, component_name")
    List<DashboardComponent> selectAllActive();
}
