package com.xwms.core.simulation.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.simulation.entity.ScenarioSimulation;

@Mapper
public interface ScenarioSimulationMapper extends BaseMapper<ScenarioSimulation> {
    @Select("SELECT * FROM wms_scenario_simulation WHERE scenario_id = #{scenarioId}")
    ScenarioSimulation selectByScenarioId(@Param("scenarioId") String scenarioId);

    @Select(
            "SELECT * FROM wms_scenario_simulation WHERE warehouse_code = #{warehouseCode} AND scenario_type = #{scenarioType} ORDER BY created_time DESC")
    List<ScenarioSimulation> selectByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode,
            @Param("scenarioType") String scenarioType);

    @Select(
            "SELECT * FROM wms_scenario_simulation WHERE status = #{status} ORDER BY created_time DESC")
    List<ScenarioSimulation> selectByStatus(@Param("status") String status);
}
