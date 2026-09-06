package com.xwms.analytics.costing.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.analytics.costing.entity.CostAdjust;

@Mapper
public interface CostAdjustMapper extends BaseMapper<CostAdjust> {

    @Select("SELECT * FROM wms_cost_adjust WHERE adjust_id = #{adjustId}")
    CostAdjust selectByAdjustId(@Param("adjustId") String adjustId);

    @Select(
            "SELECT * FROM wms_cost_adjust WHERE sku_code = #{skuCode} ORDER BY adjust_time DESC LIMIT #{limit}")
    List<CostAdjust> selectBySku(@Param("skuCode") String skuCode, @Param("limit") int limit);

    @Select("SELECT * FROM wms_cost_adjust WHERE status = #{status} ORDER BY adjust_time")
    List<CostAdjust> selectByStatus(@Param("status") String status);

    @Select(
            "SELECT * FROM wms_cost_adjust WHERE warehouse_code = #{warehouseCode} AND adjust_type = #{adjustType} ORDER BY adjust_time DESC LIMIT #{limit}")
    List<CostAdjust> selectByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode,
            @Param("adjustType") String adjustType,
            @Param("limit") int limit);

    @Update(
            "UPDATE wms_cost_adjust SET status = #{status}, approver = #{approver}, approve_time = NOW(), approve_opinion = #{approveOpinion}, updated_time = NOW() WHERE adjust_id = #{adjustId}")
    int approveAdjust(
            @Param("adjustId") String adjustId,
            @Param("status") String status,
            @Param("approver") String approver,
            @Param("approveOpinion") String approveOpinion);
}
