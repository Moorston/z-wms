package com.xwms.core.inbound.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.inbound.entity.InboundDetail;

@Mapper
public interface InboundDetailMapper extends BaseMapper<InboundDetail> {

    @Select("SELECT * FROM wms_inbound_detail WHERE inbound_no = #{inboundNo} ORDER BY line_no")
    List<InboundDetail> selectByInboundNo(@Param("inboundNo") String inboundNo);

    @Select("SELECT * FROM wms_inbound_detail WHERE detail_no = #{detailNo}")
    InboundDetail selectByDetailNo(@Param("detailNo") String detailNo);
}
