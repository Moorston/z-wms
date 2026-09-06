package com.xwms.core.safety.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.safety.entity.SafetyStock;

@Mapper
public interface SafetyStockMapper extends BaseMapper<SafetyStock> {

    @Select(
            "SELECT * FROM wms_safety_stock WHERE warehouse_code = #{warehouseCode} AND status = 'CURRENT' ORDER BY sku_code")
    List<SafetyStock> selectCurrentByWarehouse(@Param("warehouseCode") String warehouseCode);

    @Select("SELECT * FROM wms_safety_stock WHERE sku_code = #{skuCode} AND status = 'CURRENT'")
    SafetyStock selectCurrentBySku(@Param("skuCode") String skuCode);

    @Select(
            "SELECT * FROM wms_safety_stock WHERE warehouse_code = #{warehouseCode} AND stock_status = #{stockStatus} AND status = 'CURRENT'")
    List<SafetyStock> selectByStockStatus(
            @Param("warehouseCode") String warehouseCode, @Param("stockStatus") String stockStatus);

    @Update(
            "UPDATE wms_safety_stock SET status = 'HISTORY' WHERE warehouse_code = #{warehouseCode} AND status = 'CURRENT'")
    int markAsHistory(@Param("warehouseCode") String warehouseCode);
}
