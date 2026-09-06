package com.xwms.core.datamart.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.datamart.entity.DimensionDefine;

@Mapper
public interface DimensionDefineMapper extends BaseMapper<DimensionDefine> {
    @Select("SELECT * FROM wms_dimension_define WHERE dimension_id = #{dimensionId}")
    DimensionDefine selectByDimensionId(@Param("dimensionId") String dimensionId);

    @Select("SELECT * FROM wms_dimension_define WHERE dimension_code = #{dimensionCode}")
    DimensionDefine selectByDimensionCode(@Param("dimensionCode") String dimensionCode);

    @Select(
            "SELECT * FROM wms_dimension_define WHERE mart_id = #{martId} AND is_active = 'Y' ORDER BY dimension_type, sort_order")
    List<DimensionDefine> selectActiveByMart(@Param("martId") String martId);

    @Select(
            "SELECT * FROM wms_dimension_define WHERE dimension_type = #{dimensionType} AND is_active = 'Y' ORDER BY sort_order")
    List<DimensionDefine> selectActiveByType(@Param("dimensionType") String dimensionType);

    @Select(
            "SELECT * FROM wms_dimension_define WHERE is_time_dimension = 'Y' AND is_active = 'Y' ORDER BY sort_order")
    List<DimensionDefine> selectTimeDimensions();
}
