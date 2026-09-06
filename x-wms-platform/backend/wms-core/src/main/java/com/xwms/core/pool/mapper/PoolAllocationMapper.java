package com.xwms.core.pool.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.pool.entity.PoolAllocation;

@Mapper
public interface PoolAllocationMapper extends BaseMapper<PoolAllocation> {

    @Select("SELECT * FROM wms_pool_allocation WHERE allocation_id = #{allocationId}")
    PoolAllocation selectByAllocationId(@Param("allocationId") String allocationId);

    @Select(
            "SELECT * FROM wms_pool_allocation WHERE order_no = #{orderNo} ORDER BY order_line, sku_code")
    List<PoolAllocation> selectByOrderNo(@Param("orderNo") String orderNo);

    @Select(
            "SELECT * FROM wms_pool_allocation WHERE pool_code = #{poolCode} AND status IN ('ALLOCATED','PICKING')")
    List<PoolAllocation> selectActiveByPool(@Param("poolCode") String poolCode);

    @Select(
            "SELECT * FROM wms_pool_allocation WHERE sku_code = #{skuCode} AND status IN ('ALLOCATED','PICKING')")
    List<PoolAllocation> selectActiveBySku(@Param("skuCode") String skuCode);

    @Update(
            "UPDATE wms_pool_allocation SET status = #{status}, picked_qty = #{pickedQty}, remain_qty = #{remainQty} WHERE id = #{id}")
    int updateStatus(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("pickedQty") java.math.BigDecimal pickedQty,
            @Param("remainQty") java.math.BigDecimal remainQty);

    @Update(
            "UPDATE wms_pool_allocation SET status = 'RELEASED', release_time = NOW() WHERE allocation_id = #{allocationId}")
    int releaseByAllocationId(@Param("allocationId") String allocationId);
}
