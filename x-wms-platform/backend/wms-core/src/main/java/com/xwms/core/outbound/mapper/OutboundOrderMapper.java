package com.xwms.core.outbound.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.outbound.entity.OutboundOrder;

@Mapper
public interface OutboundOrderMapper extends BaseMapper<OutboundOrder> {

    @Select("SELECT * FROM wms_outbound_order WHERE outbound_no = #{outboundNo}")
    OutboundOrder selectByOutboundNo(@Param("outboundNo") String outboundNo);

    @Select("SELECT * FROM wms_outbound_order WHERE wave_no = #{waveNo}")
    List<OutboundOrder> selectByWaveNo(@Param("waveNo") String waveNo);

    @Select("SELECT * FROM wms_outbound_order WHERE status = #{status} ORDER BY created_time")
    List<OutboundOrder> selectByStatus(@Param("status") String status);
}
