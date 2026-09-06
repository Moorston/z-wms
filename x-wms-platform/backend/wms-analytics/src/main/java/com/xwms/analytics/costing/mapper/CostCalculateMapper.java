package com.xwms.analytics.costing.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.analytics.costing.entity.CostCalculate;

@Mapper
public interface CostCalculateMapper extends BaseMapper<CostCalculate> {

    @Select("SELECT * FROM wms_cost_calculate WHERE calculate_id = #{calculateId}")
    CostCalculate selectByCalculateId(@Param("calculateId") String calculateId);

    @Select(
            "SELECT * FROM wms_cost_calculate WHERE warehouse_code = #{warehouseCode} AND period_start = #{periodStart} AND period_end = #{periodEnd}")
    CostCalculate selectByWarehouseAndPeriod(
            @Param("warehouseCode") String warehouseCode,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd);

    @Select("SELECT * FROM wms_cost_calculate WHERE status = #{status} ORDER BY created_time")
    List<CostCalculate> selectByStatus(@Param("status") String status);

    @Select(
            "SELECT * FROM wms_cost_calculate WHERE warehouse_code = #{warehouseCode} AND calculate_type = #{calculateType} ORDER BY period_start DESC LIMIT #{limit}")
    List<CostCalculate> selectRecentByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode,
            @Param("calculateType") String calculateType,
            @Param("limit") int limit);

    @Update(
            "UPDATE wms_cost_calculate SET status = #{status}, start_time = NOW(), updated_time = NOW() WHERE calculate_id = #{calculateId}")
    int startCalculate(@Param("calculateId") String calculateId, @Param("status") String status);

    @Update(
            "UPDATE wms_cost_calculate SET status = #{status}, calculated_count = #{calculatedCount}, failed_count = #{failedCount}, total_quantity = #{totalQuantity}, total_amount = #{totalAmount}, average_cost = #{averageCost}, end_time = NOW(), duration_ms = #{durationMs}, error_message = #{errorMessage}, updated_time = NOW() WHERE calculate_id = #{calculateId}")
    int completeCalculate(
            @Param("calculateId") String calculateId,
            @Param("status") String status,
            @Param("calculatedCount") Integer calculatedCount,
            @Param("failedCount") Integer failedCount,
            @Param("totalQuantity") java.math.BigDecimal totalQuantity,
            @Param("totalAmount") java.math.BigDecimal totalAmount,
            @Param("averageCost") java.math.BigDecimal averageCost,
            @Param("durationMs") Long durationMs,
            @Param("errorMessage") String errorMessage);
}
