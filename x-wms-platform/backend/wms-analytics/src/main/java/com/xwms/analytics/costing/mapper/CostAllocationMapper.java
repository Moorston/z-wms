package com.xwms.analytics.costing.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.analytics.costing.entity.CostAllocation;

@Mapper
public interface CostAllocationMapper extends BaseMapper<CostAllocation> {

    @Select("SELECT * FROM wms_cost_allocation WHERE allocation_id = #{allocationId}")
    CostAllocation selectByAllocationId(@Param("allocationId") String allocationId);

    @Select("SELECT * FROM wms_cost_allocation WHERE status = #{status} ORDER BY created_time")
    List<CostAllocation> selectByStatus(@Param("status") String status);

    @Select(
            "SELECT * FROM wms_cost_allocation WHERE warehouse_code = #{warehouseCode} AND allocation_type = #{allocationType} ORDER BY created_time DESC LIMIT #{limit}")
    List<CostAllocation> selectByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode,
            @Param("allocationType") String allocationType,
            @Param("limit") int limit);

    @Update(
            "UPDATE wms_cost_allocation SET status = #{status}, start_time = NOW(), updated_time = NOW() WHERE allocation_id = #{allocationId}")
    int startAllocation(@Param("allocationId") String allocationId, @Param("status") String status);

    @Update(
            "UPDATE wms_cost_allocation SET status = #{status}, allocated_count = #{allocatedCount}, allocated_amount = #{allocatedAmount}, end_time = NOW(), duration_ms = #{durationMs}, error_message = #{errorMessage}, updated_time = NOW() WHERE allocation_id = #{allocationId}")
    int completeAllocation(
            @Param("allocationId") String allocationId,
            @Param("status") String status,
            @Param("allocatedCount") Integer allocatedCount,
            @Param("allocatedAmount") java.math.BigDecimal allocatedAmount,
            @Param("durationMs") Long durationMs,
            @Param("errorMessage") String errorMessage);
}
