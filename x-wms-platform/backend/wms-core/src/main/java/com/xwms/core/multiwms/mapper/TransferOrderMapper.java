package com.xwms.core.multiwms.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.multiwms.entity.TransferOrder;

@Mapper
public interface TransferOrderMapper extends BaseMapper<TransferOrder> {

    @Select(
            "SELECT * FROM wms_transfer_order WHERE from_warehouse = #{warehouse} OR to_warehouse = #{warehouse} ORDER BY created_time DESC")
    List<TransferOrder> selectByWarehouse(@Param("warehouse") String warehouse);
}
