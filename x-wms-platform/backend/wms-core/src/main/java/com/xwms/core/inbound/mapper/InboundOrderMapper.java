package com.xwms.core.inbound.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.inbound.entity.InboundOrder;

@Mapper
public interface InboundOrderMapper extends BaseMapper<InboundOrder> {

    @Select("SELECT * FROM wms_inbound_order WHERE inbound_no = #{inboundNo}")
    InboundOrder selectByInboundNo(@Param("inboundNo") String inboundNo);

    @Select("SELECT * FROM wms_inbound_order WHERE asn_no = #{asnNo}")
    List<InboundOrder> selectByAsnNo(@Param("asnNo") String asnNo);

    @Select("SELECT * FROM wms_inbound_order WHERE status = #{status} ORDER BY created_time")
    List<InboundOrder> selectByStatus(@Param("status") String status);
}
