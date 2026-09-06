package com.xwms.core.allocation.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.allocation.entity.AllocationDetail;

@Mapper
public interface AllocationDetailMapper extends BaseMapper<AllocationDetail> {

    @Select(
            "SELECT * FROM wms_allocation_detail WHERE allocation_no = #{allocationNo} ORDER BY sku_code, location_code")
    List<AllocationDetail> selectByAllocationNo(@Param("allocationNo") String allocationNo);

    @Select(
            "SELECT * FROM wms_allocation_detail WHERE order_no = #{orderNo} ORDER BY order_line, sku_code")
    List<AllocationDetail> selectByOrderNo(@Param("orderNo") String orderNo);

    @Select(
            "SELECT * FROM wms_allocation_detail WHERE sku_code = #{skuCode} AND status IN ('ALLOCATED','PICKING')")
    List<AllocationDetail> selectActiveBySku(@Param("skuCode") String skuCode);

    @Select(
            "SELECT * FROM wms_allocation_detail WHERE location_code = #{locationCode} AND status IN ('ALLOCATED','PICKING')")
    List<AllocationDetail> selectActiveByLocation(@Param("locationCode") String locationCode);

    @Update(
            "UPDATE wms_allocation_detail SET status = #{status}, picked_qty = #{pickedQty}, remain_qty = #{remainQty} WHERE id = #{id}")
    int updateStatus(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("pickedQty") java.math.BigDecimal pickedQty,
            @Param("remainQty") java.math.BigDecimal remainQty);

    @Update(
            "UPDATE wms_allocation_detail SET status = 'RELEASED', release_time = NOW() WHERE allocation_no = #{allocationNo}")
    int releaseByAllocationNo(@Param("allocationNo") String allocationNo);
}
