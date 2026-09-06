package com.xwms.core.simulation.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.simulation.entity.InventorySimulation;

@Mapper
public interface InventorySimulationMapper extends BaseMapper<InventorySimulation> {
    @Select("SELECT * FROM wms_inventory_simulation WHERE simulation_id = #{simulationId}")
    InventorySimulation selectBySimulationId(@Param("simulationId") String simulationId);

    @Select(
            "SELECT * FROM wms_inventory_simulation WHERE warehouse_code = #{warehouseCode} AND simulation_type = #{simulationType} ORDER BY created_time DESC")
    List<InventorySimulation> selectByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode,
            @Param("simulationType") String simulationType);

    @Select(
            "SELECT * FROM wms_inventory_simulation WHERE status = #{status} ORDER BY created_time DESC")
    List<InventorySimulation> selectByStatus(@Param("status") String status);
}
