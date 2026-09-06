package com.xwms.core.shipment.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.shipment.entity.ShipmentDetail;

@Mapper
public interface ShipmentDetailMapper extends BaseMapper<ShipmentDetail> {

    @Select("SELECT * FROM wms_shipment_detail WHERE shipment_no = #{shipmentNo} ORDER BY line_no")
    List<ShipmentDetail> selectByShipmentNo(@Param("shipmentNo") String shipmentNo);
}
