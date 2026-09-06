package com.xwms.core.decision.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.decision.entity.InventoryStrategy;

@Mapper
public interface InventoryStrategyMapper extends BaseMapper<InventoryStrategy> {
    @Select("SELECT * FROM wms_inventory_strategy WHERE strategy_id = #{strategyId}")
    InventoryStrategy selectByStrategyId(@Param("strategyId") String strategyId);

    @Select("SELECT * FROM wms_inventory_strategy WHERE strategy_code = #{strategyCode}")
    InventoryStrategy selectByStrategyCode(@Param("strategyCode") String strategyCode);

    @Select(
            "SELECT * FROM wms_inventory_strategy WHERE strategy_type = #{strategyType} AND is_active = 'Y' ORDER BY priority ASC")
    List<InventoryStrategy> selectActiveByType(@Param("strategyType") String strategyType);

    @Select(
            "SELECT * FROM wms_inventory_strategy WHERE warehouse_code = #{warehouseCode} AND strategy_type = #{strategyType} AND is_active = 'Y' ORDER BY priority ASC")
    List<InventoryStrategy> selectActiveByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode,
            @Param("strategyType") String strategyType);
}
