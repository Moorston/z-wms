package com.xwms.core.vas.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.vas.entity.VasOrderItem;

@Mapper
public interface VasOrderItemMapper extends BaseMapper<VasOrderItem> {

    @Select("SELECT * FROM wms_vas_order_item WHERE order_id = #{orderId} ORDER BY id")
    List<VasOrderItem> selectByOrderId(@Param("orderId") Long orderId);
}
