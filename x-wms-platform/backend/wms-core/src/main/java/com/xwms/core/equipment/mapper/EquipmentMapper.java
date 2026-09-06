package com.xwms.core.equipment.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.equipment.entity.Equipment;

@Mapper
public interface EquipmentMapper extends BaseMapper<Equipment> {

    @Select(
            "SELECT * FROM wms_equipment WHERE warehouse_code = #{warehouse} AND status = 'IDLE' AND deleted = 0 ORDER BY equipment_code")
    List<Equipment> selectAvailableByWarehouse(@Param("warehouse") String warehouse);

    @Select(
            "SELECT * FROM wms_equipment WHERE equipment_type = #{type} AND status = 'IDLE' AND warehouse_code = #{warehouse} AND deleted = 0 ORDER BY total_run_hours ASC")
    List<Equipment> selectAvailableByType(
            @Param("type") String type, @Param("warehouse") String warehouse);
}
