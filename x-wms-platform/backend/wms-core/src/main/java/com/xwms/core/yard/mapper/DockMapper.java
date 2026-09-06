package com.xwms.core.yard.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.yard.entity.Dock;

@Mapper
public interface DockMapper extends BaseMapper<Dock> {

    @Select(
            "SELECT * FROM wms_dock WHERE warehouse_code = #{warehouse} AND status = 'IDLE' AND dock_type IN (#{type}, 'BOTH') AND deleted = 0 ORDER BY sort_order")
    List<Dock> selectAvailableDocks(
            @Param("warehouse") String warehouse, @Param("type") String type);

    @Select(
            "SELECT * FROM wms_dock WHERE warehouse_code = #{warehouse} AND deleted = 0 ORDER BY sort_order")
    List<Dock> selectByWarehouse(@Param("warehouse") String warehouse);
}
