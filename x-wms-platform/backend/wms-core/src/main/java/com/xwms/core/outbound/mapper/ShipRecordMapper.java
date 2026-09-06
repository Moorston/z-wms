package com.xwms.core.outbound.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.outbound.entity.ShipRecord;

@Mapper
public interface ShipRecordMapper extends BaseMapper<ShipRecord> {

    @Select("SELECT * FROM wms_ship_record WHERE outbound_no = #{outboundNo} ORDER BY ship_time")
    List<ShipRecord> selectByOutboundNo(@Param("outboundNo") String outboundNo);

    @Select("SELECT * FROM wms_ship_record WHERE tracking_no = #{trackingNo}")
    ShipRecord selectByTrackingNo(@Param("trackingNo") String trackingNo);
}
