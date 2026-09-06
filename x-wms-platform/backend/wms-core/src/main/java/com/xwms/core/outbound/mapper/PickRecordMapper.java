package com.xwms.core.outbound.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.outbound.entity.PickRecord;

@Mapper
public interface PickRecordMapper extends BaseMapper<PickRecord> {

    @Select("SELECT * FROM wms_pick_record WHERE outbound_no = #{outboundNo} ORDER BY pick_time")
    List<PickRecord> selectByOutboundNo(@Param("outboundNo") String outboundNo);

    @Select("SELECT * FROM wms_pick_record WHERE wave_no = #{waveNo} ORDER BY pick_time")
    List<PickRecord> selectByWaveNo(@Param("waveNo") String waveNo);
}
