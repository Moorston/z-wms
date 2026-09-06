package com.xwms.base.location.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.location.entity.LocationGroup;

@Mapper
public interface LocationGroupMapper extends BaseMapper<LocationGroup> {

    @Select(
            "SELECT * FROM wms_location_group WHERE warehouse_code = #{warehouseCode} AND status = 'ACTIVE' ORDER BY sort_no")
    List<LocationGroup> selectByWarehouse(@Param("warehouseCode") String warehouseCode);

    @Select("SELECT * FROM wms_location_group WHERE group_code = #{code}")
    LocationGroup selectByCode(@Param("code") String code);
}
