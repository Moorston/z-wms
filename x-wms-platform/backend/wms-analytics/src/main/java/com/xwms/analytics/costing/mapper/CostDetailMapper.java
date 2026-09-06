package com.xwms.analytics.costing.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.analytics.costing.entity.CostDetail;

@Mapper
public interface CostDetailMapper extends BaseMapper<CostDetail> {

    @Select("SELECT * FROM wms_cost_detail WHERE detail_id = #{detailId}")
    CostDetail selectByDetailId(@Param("detailId") String detailId);

    @Select(
            "SELECT * FROM wms_cost_detail WHERE calculate_id = #{calculateId} ORDER BY operation_time")
    List<CostDetail> selectByCalculateId(@Param("calculateId") String calculateId);

    @Select("SELECT * FROM wms_cost_detail WHERE adjust_id = #{adjustId} ORDER BY operation_time")
    List<CostDetail> selectByAdjustId(@Param("adjustId") String adjustId);

    @Select(
            "SELECT * FROM wms_cost_detail WHERE allocation_id = #{allocationId} ORDER BY operation_time")
    List<CostDetail> selectByAllocationId(@Param("allocationId") String allocationId);

    @Select(
            "SELECT * FROM wms_cost_detail WHERE sku_code = #{skuCode} AND period_date >= #{startDate} AND period_date <= #{endDate} ORDER BY operation_time")
    List<CostDetail> selectBySkuAndPeriod(
            @Param("skuCode") String skuCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Select(
            "SELECT * FROM wms_cost_detail WHERE biz_type = #{bizType} AND biz_no = #{bizNo} ORDER BY operation_time")
    List<CostDetail> selectByBiz(@Param("bizType") String bizType, @Param("bizNo") String bizNo);

    @Select(
            "SELECT SUM(total_cost) FROM wms_cost_detail WHERE warehouse_code = #{warehouseCode} AND period_date = #{periodDate} AND biz_type = #{bizType}")
    java.math.BigDecimal sumCostByWarehouseAndPeriod(
            @Param("warehouseCode") String warehouseCode,
            @Param("periodDate") LocalDate periodDate,
            @Param("bizType") String bizType);
}
