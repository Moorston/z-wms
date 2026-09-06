package com.xwms.core.decision.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.decision.entity.InventoryOptimization;

@Mapper
public interface InventoryOptimizationMapper extends BaseMapper<InventoryOptimization> {
    @Select("SELECT * FROM wms_inventory_optimization WHERE optimization_id = #{optimizationId}")
    InventoryOptimization selectByOptimizationId(@Param("optimizationId") String optimizationId);

    @Select(
            "SELECT * FROM wms_inventory_optimization WHERE warehouse_code = #{warehouseCode} AND optimization_type = #{optimizationType} ORDER BY optimization_time DESC")
    List<InventoryOptimization> selectByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode,
            @Param("optimizationType") String optimizationType);

    @Select(
            "SELECT * FROM wms_inventory_optimization WHERE implementation_status = #{implementationStatus} ORDER BY optimization_time DESC")
    List<InventoryOptimization> selectByImplementationStatus(
            @Param("implementationStatus") String implementationStatus);

    @Select(
            "SELECT * FROM wms_inventory_optimization WHERE warehouse_code = #{warehouseCode} AND status = #{status} ORDER BY priority DESC, optimization_time DESC")
    List<InventoryOptimization> selectByWarehouseAndStatus(
            @Param("warehouseCode") String warehouseCode, @Param("status") String status);
}
