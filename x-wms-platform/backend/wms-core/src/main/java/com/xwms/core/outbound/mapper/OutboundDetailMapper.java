package com.xwms.core.outbound.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.outbound.entity.OutboundDetail;

@Mapper
public interface OutboundDetailMapper extends BaseMapper<OutboundDetail> {

    @Select("SELECT * FROM wms_outbound_detail WHERE outbound_no = #{outboundNo} ORDER BY line_no")
    List<OutboundDetail> selectByOutboundNo(@Param("outboundNo") String outboundNo);

    @Select("SELECT * FROM wms_outbound_detail WHERE detail_no = #{detailNo}")
    OutboundDetail selectByDetailNo(@Param("detailNo") String detailNo);
}
