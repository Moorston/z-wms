package com.xwms.base.carrier.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.carrier.entity.Carrier;

@Mapper
public interface CarrierMapper extends BaseMapper<Carrier> {

    @Select("SELECT * FROM wms_carrier WHERE carrier_code = #{carrierCode}")
    Carrier selectByCarrierCode(@Param("carrierCode") String carrierCode);

    @Select("SELECT * FROM wms_carrier WHERE status = 'ACTIVE' ORDER BY priority DESC")
    List<Carrier> selectActiveCarriers();

    @Select(
            "SELECT * FROM wms_carrier WHERE carrier_type = #{carrierType} AND status = 'ACTIVE' ORDER BY priority DESC")
    List<Carrier> selectByType(@Param("carrierType") String carrierType);
}
