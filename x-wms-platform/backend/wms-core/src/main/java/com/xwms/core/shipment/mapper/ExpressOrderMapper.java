package com.xwms.core.shipment.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.shipment.entity.ExpressOrder;

@Mapper
public interface ExpressOrderMapper extends BaseMapper<ExpressOrder> {

    @Select("SELECT * FROM wms_express_order WHERE express_no = #{expressNo}")
    ExpressOrder selectByExpressNo(@Param("expressNo") String expressNo);

    @Select("SELECT * FROM wms_express_order WHERE tracking_no = #{trackingNo}")
    ExpressOrder selectByTrackingNo(@Param("trackingNo") String trackingNo);

    @Select("SELECT * FROM wms_express_order WHERE shipment_no = #{shipmentNo}")
    List<ExpressOrder> selectByShipmentNo(@Param("shipmentNo") String shipmentNo);

    @Select("SELECT * FROM wms_express_order WHERE outbound_no = #{outboundNo}")
    List<ExpressOrder> selectByOutboundNo(@Param("outboundNo") String outboundNo);

    @Select("SELECT * FROM wms_express_order WHERE status = #{status} ORDER BY created_time")
    List<ExpressOrder> selectByStatus(@Param("status") String status);
}
