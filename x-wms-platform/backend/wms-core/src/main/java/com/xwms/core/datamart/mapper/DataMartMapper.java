package com.xwms.core.datamart.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.datamart.entity.DataMart;

@Mapper
public interface DataMartMapper extends BaseMapper<DataMart> {
    @Select("SELECT * FROM wms_data_mart WHERE mart_id = #{martId}")
    DataMart selectByMartId(@Param("martId") String martId);

    @Select("SELECT * FROM wms_data_mart WHERE mart_code = #{martCode}")
    DataMart selectByMartCode(@Param("martCode") String martCode);

    @Select(
            "SELECT * FROM wms_data_mart WHERE mart_type = #{martType} AND is_active = 'Y' ORDER BY sort_order")
    List<DataMart> selectActiveByType(@Param("martType") String martType);

    @Select(
            "SELECT * FROM wms_data_mart WHERE warehouse_code = #{warehouseCode} AND is_active = 'Y' ORDER BY sort_order")
    List<DataMart> selectActiveByWarehouse(@Param("warehouseCode") String warehouseCode);
}
