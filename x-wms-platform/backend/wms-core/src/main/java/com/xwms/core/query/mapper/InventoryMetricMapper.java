package com.xwms.core.query.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.query.entity.InventoryMetric;

@Mapper
public interface InventoryMetricMapper extends BaseMapper<InventoryMetric> {

    @Select(
            "SELECT * FROM wms_inventory_metric WHERE metric_date = #{metricDate} AND metric_type = #{metricType} AND warehouse_code = #{warehouseCode} ORDER BY rank_no")
    List<InventoryMetric> selectByDateAndType(
            @Param("metricDate") LocalDate metricDate,
            @Param("metricType") String metricType,
            @Param("warehouseCode") String warehouseCode);

    @Select(
            "SELECT * FROM wms_inventory_metric WHERE metric_date BETWEEN #{startDate} AND #{endDate} AND metric_type = #{metricType} AND sku_code = #{skuCode} ORDER BY metric_date")
    List<InventoryMetric> selectBySkuAndDateRange(
            @Param("skuCode") String skuCode,
            @Param("metricType") String metricType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Select(
            "SELECT * FROM wms_inventory_metric WHERE metric_date = #{metricDate} AND metric_type = #{metricType} AND level_code = #{levelCode}")
    List<InventoryMetric> selectByLevel(
            @Param("metricDate") LocalDate metricDate,
            @Param("metricType") String metricType,
            @Param("levelCode") String levelCode);
}
