package com.xwms.core.forecast.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.forecast.entity.DemandForecast;

@Mapper
public interface DemandForecastMapper extends BaseMapper<DemandForecast> {

    @Select("SELECT * FROM wms_demand_forecast WHERE forecast_id = #{forecastId}")
    DemandForecast selectByForecastId(@Param("forecastId") String forecastId);

    @Select(
            "SELECT * FROM wms_demand_forecast WHERE sku_code = #{skuCode} AND forecast_period = #{forecastPeriod} ORDER BY forecast_time DESC LIMIT #{limit}")
    List<DemandForecast> selectBySkuAndPeriod(
            @Param("skuCode") String skuCode,
            @Param("forecastPeriod") String forecastPeriod,
            @Param("limit") int limit);

    @Select(
            "SELECT * FROM wms_demand_forecast WHERE warehouse_code = #{warehouseCode} AND model_code = #{modelCode} AND forecast_start = #{forecastStart} AND forecast_end = #{forecastEnd}")
    List<DemandForecast> selectByWarehouseAndModelAndPeriod(
            @Param("warehouseCode") String warehouseCode,
            @Param("modelCode") String modelCode,
            @Param("forecastStart") LocalDate forecastStart,
            @Param("forecastEnd") LocalDate forecastEnd);

    @Select(
            "SELECT * FROM wms_demand_forecast WHERE warehouse_code = #{warehouseCode} AND forecast_period = #{forecastPeriod} ORDER BY forecast_time DESC LIMIT #{limit}")
    List<DemandForecast> selectRecentByWarehouseAndPeriod(
            @Param("warehouseCode") String warehouseCode,
            @Param("forecastPeriod") String forecastPeriod,
            @Param("limit") int limit);
}
