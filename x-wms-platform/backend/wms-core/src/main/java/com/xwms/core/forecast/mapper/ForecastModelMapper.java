package com.xwms.core.forecast.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.forecast.entity.ForecastModel;

@Mapper
public interface ForecastModelMapper extends BaseMapper<ForecastModel> {

    @Select(
            "SELECT * FROM wms_forecast_model WHERE model_code = #{modelCode} AND version = #{version}")
    ForecastModel selectByCodeAndVersion(
            @Param("modelCode") String modelCode, @Param("version") String version);

    @Select(
            "SELECT * FROM wms_forecast_model WHERE model_type = #{modelType} AND status = 'ACTIVE' ORDER BY accuracy DESC")
    List<ForecastModel> selectActiveByType(@Param("modelType") String modelType);

    @Select(
            "SELECT * FROM wms_forecast_model WHERE is_default = 'Y' AND status = 'ACTIVE' AND model_type = #{modelType} LIMIT 1")
    ForecastModel selectDefaultByType(@Param("modelType") String modelType);

    @Select("SELECT * FROM wms_forecast_model WHERE status = #{status} ORDER BY created_time DESC")
    List<ForecastModel> selectByStatus(@Param("status") String status);

    @Update(
            "UPDATE wms_forecast_model SET is_default = 'N', updated_time = NOW() WHERE model_type = #{modelType} AND is_default = 'Y'")
    int clearDefaultByType(@Param("modelType") String modelType);

    @Update(
            "UPDATE wms_forecast_model SET is_default = 'Y', updated_time = NOW() WHERE model_code = #{modelCode} AND version = #{version}")
    int setDefault(@Param("modelCode") String modelCode, @Param("version") String version);
}
