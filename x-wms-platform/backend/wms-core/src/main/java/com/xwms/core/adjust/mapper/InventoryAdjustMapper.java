package com.xwms.core.adjust.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.adjust.entity.InventoryAdjust;

@Mapper
public interface InventoryAdjustMapper extends BaseMapper<InventoryAdjust> {

    @Select("SELECT * FROM wms_inventory_adjust WHERE adjust_no = #{adjustNo}")
    InventoryAdjust selectByAdjustNo(@Param("adjustNo") String adjustNo);

    @Update(
            "UPDATE wms_inventory_adjust SET status = #{status}, updated_time = NOW() WHERE adjust_no = #{adjustNo}")
    int updateStatus(@Param("adjustNo") String adjustNo, @Param("status") String status);
}
