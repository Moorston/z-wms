package com.xwms.core.crossdock.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.crossdock.entity.Crossdock;

@Mapper
public interface CrossdockMapper extends BaseMapper<Crossdock> {

    @Select("SELECT * FROM wms_crossdock WHERE crossdock_no = #{crossdockNo}")
    Crossdock selectByCrossdockNo(@Param("crossdockNo") String crossdockNo);

    @Select("SELECT * FROM wms_crossdock WHERE inbound_no = #{inboundNo}")
    Crossdock selectByInboundNo(@Param("inboundNo") String inboundNo);

    @Select("SELECT * FROM wms_crossdock WHERE outbound_no = #{outboundNo}")
    Crossdock selectByOutboundNo(@Param("outboundNo") String outboundNo);

    @Select("SELECT * FROM wms_crossdock WHERE status = #{status} ORDER BY scheduled_time")
    List<Crossdock> selectByStatus(@Param("status") String status);
}
