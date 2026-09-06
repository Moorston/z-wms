package com.xwms.core.inbound.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.inbound.entity.ReceiveRecord;

@Mapper
public interface ReceiveRecordMapper extends BaseMapper<ReceiveRecord> {

    @Select(
            "SELECT * FROM wms_receive_record WHERE inbound_no = #{inboundNo} ORDER BY receive_time")
    List<ReceiveRecord> selectByInboundNo(@Param("inboundNo") String inboundNo);

    @Select("SELECT * FROM wms_receive_record WHERE detail_no = #{detailNo} ORDER BY receive_time")
    List<ReceiveRecord> selectByDetailNo(@Param("detailNo") String detailNo);
}
