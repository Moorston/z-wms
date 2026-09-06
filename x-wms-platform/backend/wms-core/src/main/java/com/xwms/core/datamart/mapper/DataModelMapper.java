package com.xwms.core.datamart.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.datamart.entity.DataModel;

@Mapper
public interface DataModelMapper extends BaseMapper<DataModel> {
    @Select("SELECT * FROM wms_data_model WHERE model_id = #{modelId}")
    DataModel selectByModelId(@Param("modelId") String modelId);

    @Select("SELECT * FROM wms_data_model WHERE model_code = #{modelCode}")
    DataModel selectByModelCode(@Param("modelCode") String modelCode);

    @Select(
            "SELECT * FROM wms_data_model WHERE mart_id = #{martId} AND is_active = 'Y' ORDER BY model_type, sort_order")
    List<DataModel> selectActiveByMart(@Param("martId") String martId);

    @Select(
            "SELECT * FROM wms_data_model WHERE model_type = #{modelType} AND is_active = 'Y' ORDER BY sort_order")
    List<DataModel> selectActiveByType(@Param("modelType") String modelType);

    @Select("SELECT * FROM wms_data_model WHERE table_name = #{tableName}")
    DataModel selectByTableName(@Param("tableName") String tableName);
}
