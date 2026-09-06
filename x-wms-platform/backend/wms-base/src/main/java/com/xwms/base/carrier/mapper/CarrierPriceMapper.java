package com.xwms.base.carrier.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.carrier.entity.CarrierPrice;

@Mapper
public interface CarrierPriceMapper extends BaseMapper<CarrierPrice> {

    @Select(
            "SELECT * FROM wms_carrier_price WHERE carrier_code = #{carrierCode} AND service_code = #{serviceCode} AND status = 'ACTIVE' ORDER BY start_weight")
    List<CarrierPrice> selectByCarrierAndService(
            @Param("carrierCode") String carrierCode, @Param("serviceCode") String serviceCode);

    @Select(
            "SELECT * FROM wms_carrier_price WHERE carrier_code = #{carrierCode} AND service_code = #{serviceCode} AND region_code = #{regionCode} AND start_weight <= #{weight} AND (end_weight IS NULL OR end_weight >= #{weight}) AND status = 'ACTIVE' AND (effective_date IS NULL OR effective_date <= NOW()) AND (expire_date IS NULL OR expire_date >= NOW()) LIMIT 1")
    CarrierPrice selectPrice(
            @Param("carrierCode") String carrierCode,
            @Param("serviceCode") String serviceCode,
            @Param("regionCode") String regionCode,
            @Param("weight") BigDecimal weight);
}
