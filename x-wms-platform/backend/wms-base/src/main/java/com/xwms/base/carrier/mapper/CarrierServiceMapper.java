package com.xwms.base.carrier.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.carrier.entity.CarrierService;

@Mapper
public interface CarrierServiceMapper extends BaseMapper<CarrierService> {

    @Select(
            "SELECT * FROM wms_carrier_service WHERE carrier_code = #{carrierCode} AND status = 'ACTIVE' ORDER BY service_code")
    List<CarrierService> selectByCarrierCode(@Param("carrierCode") String carrierCode);

    @Select(
            "SELECT * FROM wms_carrier_service WHERE carrier_code = #{carrierCode} AND service_code = #{serviceCode}")
    CarrierService selectByCarrierAndService(
            @Param("carrierCode") String carrierCode, @Param("serviceCode") String serviceCode);
}
