package com.xwms.core.query.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.query.entity.InventoryDaily;

@Mapper
public interface InventoryDailyMapper extends BaseMapper<InventoryDaily> {

    @Select(
            "SELECT * FROM wms_inventory_daily WHERE report_date = #{reportDate} AND warehouse_code = #{warehouseCode}")
    List<InventoryDaily> selectByDateAndWarehouse(
            @Param("reportDate") LocalDate reportDate,
            @Param("warehouseCode") String warehouseCode);

    @Select(
            "SELECT * FROM wms_inventory_daily WHERE report_date BETWEEN #{startDate} AND #{endDate} AND sku_code = #{skuCode} ORDER BY report_date")
    List<InventoryDaily> selectBySkuAndDateRange(
            @Param("skuCode") String skuCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Select(
            "SELECT * FROM wms_inventory_daily WHERE report_date = #{reportDate} AND sku_code = #{skuCode} AND warehouse_code = #{warehouseCode}")
    InventoryDaily selectByDateAndSku(
            @Param("reportDate") LocalDate reportDate,
            @Param("skuCode") String skuCode,
            @Param("warehouseCode") String warehouseCode);
}
