package com.xwms.core.equipment.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.equipment.entity.EquipmentUsage;

@Mapper
public interface EquipmentUsageMapper extends BaseMapper<EquipmentUsage> {

    @Select(
            "SELECT * FROM wms_equipment_usage WHERE equipment_id = #{equipmentId} ORDER BY start_time DESC")
    List<EquipmentUsage> selectByEquipmentId(@Param("equipmentId") Long equipmentId);
}
