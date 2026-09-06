package com.xwms.core.allocation.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.allocation.entity.AllocationLog;

@Mapper
public interface AllocationLogMapper extends BaseMapper<AllocationLog> {

    @Select(
            "SELECT * FROM wms_allocation_log WHERE allocation_no = #{allocationNo} ORDER BY operate_time")
    List<AllocationLog> selectByAllocationNo(@Param("allocationNo") String allocationNo);

    @Select("SELECT * FROM wms_allocation_log WHERE order_no = #{orderNo} ORDER BY operate_time")
    List<AllocationLog> selectByOrderNo(@Param("orderNo") String orderNo);

    @Select(
            "SELECT * FROM wms_allocation_log WHERE sku_code = #{skuCode} ORDER BY operate_time DESC LIMIT #{limit}")
    List<AllocationLog> selectRecentBySku(
            @Param("skuCode") String skuCode, @Param("limit") int limit);
}
