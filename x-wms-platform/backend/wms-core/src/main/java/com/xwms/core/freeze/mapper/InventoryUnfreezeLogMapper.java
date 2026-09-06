package com.xwms.core.freeze.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.freeze.entity.InventoryUnfreezeLog;

@Mapper
public interface InventoryUnfreezeLogMapper extends BaseMapper<InventoryUnfreezeLog> {

    @Select("SELECT * FROM wms_inventory_unfreeze_log WHERE unfreeze_no = #{unfreezeNo}")
    InventoryUnfreezeLog selectByUnfreezeNo(@Param("unfreezeNo") String unfreezeNo);

    @Select(
            "SELECT * FROM wms_inventory_unfreeze_log WHERE freeze_no = #{freezeNo} ORDER BY unfreeze_time DESC")
    List<InventoryUnfreezeLog> selectByFreezeNo(@Param("freezeNo") String freezeNo);
}
