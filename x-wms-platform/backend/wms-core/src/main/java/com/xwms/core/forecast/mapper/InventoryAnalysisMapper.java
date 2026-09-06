package com.xwms.core.forecast.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.forecast.entity.InventoryAnalysis;

@Mapper
public interface InventoryAnalysisMapper extends BaseMapper<InventoryAnalysis> {

    @Select("SELECT * FROM wms_inventory_analysis WHERE analysis_id = #{analysisId}")
    InventoryAnalysis selectByAnalysisId(@Param("analysisId") String analysisId);

    @Select(
            "SELECT * FROM wms_inventory_analysis WHERE warehouse_code = #{warehouseCode} AND analysis_type = #{analysisType} AND period_start = #{periodStart} AND period_end = #{periodEnd}")
    InventoryAnalysis selectByWarehouseAndTypeAndPeriod(
            @Param("warehouseCode") String warehouseCode,
            @Param("analysisType") String analysisType,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd);

    @Select(
            "SELECT * FROM wms_inventory_analysis WHERE sku_code = #{skuCode} AND analysis_type = #{analysisType} ORDER BY analysis_time DESC LIMIT #{limit}")
    List<InventoryAnalysis> selectBySkuAndType(
            @Param("skuCode") String skuCode,
            @Param("analysisType") String analysisType,
            @Param("limit") int limit);

    @Select(
            "SELECT * FROM wms_inventory_analysis WHERE warehouse_code = #{warehouseCode} AND analysis_type = #{analysisType} ORDER BY analysis_time DESC LIMIT #{limit}")
    List<InventoryAnalysis> selectRecentByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode,
            @Param("analysisType") String analysisType,
            @Param("limit") int limit);
}
