package com.xwms.core.shipment.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.shipment.entity.Shipment;

@Mapper
public interface ShipmentMapper extends BaseMapper<Shipment> {

    @Select("SELECT * FROM wms_shipment WHERE shipment_no = #{shipmentNo}")
    Shipment selectByShipmentNo(@Param("shipmentNo") String shipmentNo);

    @Select("SELECT * FROM wms_shipment WHERE outbound_no = #{outboundNo}")
    Shipment selectByOutboundNo(@Param("outboundNo") String outboundNo);

    @Select(
            "SELECT * FROM wms_shipment WHERE carrier = #{carrier} AND status = #{status} ORDER BY created_time")
    List<Shipment> selectByCarrierAndStatus(
            @Param("carrier") String carrier, @Param("status") String status);

    @Select("SELECT * FROM wms_shipment WHERE status = #{status} ORDER BY created_time")
    List<Shipment> selectByStatus(@Param("status") String status);
}
