package com.xwms.base.location.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.location.entity.Area;

@Mapper
public interface AreaMapper extends BaseMapper<Area> {

    @Select(
            "SELECT * FROM wms_area WHERE warehouse_code = #{warehouseCode} AND status = 'ACTIVE' ORDER BY sort_no")
    List<Area> selectByWarehouse(@Param("warehouseCode") String warehouseCode);

    @Select("SELECT * FROM wms_area WHERE area_code = #{code}")
    Area selectByCode(@Param("code") String code);
}
