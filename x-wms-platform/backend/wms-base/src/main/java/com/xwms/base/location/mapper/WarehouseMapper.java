package com.xwms.base.location.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.location.entity.Warehouse;

@Mapper
public interface WarehouseMapper extends BaseMapper<Warehouse> {

    @Select("SELECT * FROM wms_warehouse WHERE status = 'ACTIVE' ORDER BY warehouse_code")
    List<Warehouse> selectActiveWarehouses();

    @Select("SELECT * FROM wms_warehouse WHERE warehouse_code = #{code}")
    Warehouse selectByCode(@Param("code") String code);
}
