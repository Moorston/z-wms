package com.xwms.core.forecast.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.forecast.entity.InventoryOptimization;

@Mapper
public interface InventoryOptimizationMapper extends BaseMapper<InventoryOptimization> {

    @Select("SELECT * FROM wms_inventory_optimization WHERE optimization_id = #{optimizationId}")
    InventoryOptimization selectByOptimizationId(@Param("optimizationId") String optimizationId);

    @Select(
            "SELECT * FROM wms_inventory_optimization WHERE warehouse_code = #{warehouseCode} AND optimization_type = #{optimizationType} ORDER BY optimization_time DESC LIMIT #{limit}")
    List<InventoryOptimization> selectByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode,
            @Param("optimizationType") String optimizationType,
            @Param("limit") int limit);

    @Select(
            "SELECT * FROM wms_inventory_optimization WHERE sku_code = #{skuCode} AND optimization_type = #{optimizationType} ORDER BY optimization_time DESC LIMIT #{limit}")
    List<InventoryOptimization> selectBySkuAndType(
            @Param("skuCode") String skuCode,
            @Param("optimizationType") String optimizationType,
            @Param("limit") int limit);

    @Select(
            "SELECT * FROM wms_inventory_optimization WHERE status = #{status} ORDER BY optimization_time")
    List<InventoryOptimization> selectByStatus(@Param("status") String status);

    @Update(
            "UPDATE wms_inventory_optimization SET status = 'IMPLEMENTED', implementer = #{implementer}, implement_time = NOW(), updated_time = NOW() WHERE optimization_id = #{optimizationId}")
    int implementOptimization(
            @Param("optimizationId") String optimizationId,
            @Param("implementer") String implementer);
}
