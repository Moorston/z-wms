package com.xwms.core.freeze.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.freeze.entity.InventoryFreeze;

@Mapper
public interface InventoryFreezeMapper extends BaseMapper<InventoryFreeze> {

    @Select("SELECT * FROM wms_inventory_freeze WHERE freeze_no = #{freezeNo}")
    InventoryFreeze selectByFreezeNo(@Param("freezeNo") String freezeNo);

    @Update(
            "UPDATE wms_inventory_freeze SET status = #{status}, unfreeze_time = NOW(), updated_time = NOW() WHERE freeze_no = #{freezeNo}")
    int updateStatus(@Param("freezeNo") String freezeNo, @Param("status") String status);
}
