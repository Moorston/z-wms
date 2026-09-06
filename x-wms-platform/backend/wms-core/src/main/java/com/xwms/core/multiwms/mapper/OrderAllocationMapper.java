package com.xwms.core.multiwms.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.multiwms.entity.OrderAllocation;

@Mapper
public interface OrderAllocationMapper extends BaseMapper<OrderAllocation> {

    @Select("SELECT * FROM wms_order_allocation WHERE order_no = #{orderNo}")
    List<OrderAllocation> selectByOrderNo(@Param("orderNo") String orderNo);

    @Select(
            "SELECT * FROM wms_order_allocation WHERE warehouse_code = #{warehouse} AND status = 'ALLOCATED' ORDER BY priority DESC, created_time ASC")
    List<OrderAllocation> selectPendingByWarehouse(@Param("warehouse") String warehouse);
}
