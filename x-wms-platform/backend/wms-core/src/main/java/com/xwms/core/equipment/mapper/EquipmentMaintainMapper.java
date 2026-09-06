package com.xwms.core.equipment.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.equipment.entity.EquipmentMaintain;

@Mapper
public interface EquipmentMaintainMapper extends BaseMapper<EquipmentMaintain> {

    @Select(
            "SELECT * FROM wms_equipment_maintain WHERE equipment_id = #{equipmentId} ORDER BY created_time DESC")
    List<EquipmentMaintain> selectByEquipmentId(@Param("equipmentId") Long equipmentId);
}
