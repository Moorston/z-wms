package com.xwms.core.query.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.query.entity.InventoryMonthly;

@Mapper
public interface InventoryMonthlyMapper extends BaseMapper<InventoryMonthly> {

    @Select(
            "SELECT * FROM wms_inventory_monthly WHERE report_month = #{reportMonth} AND warehouse_code = #{warehouseCode}")
    List<InventoryMonthly> selectByMonthAndWarehouse(
            @Param("reportMonth") String reportMonth, @Param("warehouseCode") String warehouseCode);

    @Select(
            "SELECT * FROM wms_inventory_monthly WHERE report_month BETWEEN #{startMonth} AND #{endMonth} AND sku_code = #{skuCode} ORDER BY report_month")
    List<InventoryMonthly> selectBySkuAndMonthRange(
            @Param("skuCode") String skuCode,
            @Param("startMonth") String startMonth,
            @Param("endMonth") String endMonth);

    @Select(
            "SELECT * FROM wms_inventory_monthly WHERE report_month = #{reportMonth} AND sku_code = #{skuCode} AND warehouse_code = #{warehouseCode}")
    InventoryMonthly selectByMonthAndSku(
            @Param("reportMonth") String reportMonth,
            @Param("skuCode") String skuCode,
            @Param("warehouseCode") String warehouseCode);
}
