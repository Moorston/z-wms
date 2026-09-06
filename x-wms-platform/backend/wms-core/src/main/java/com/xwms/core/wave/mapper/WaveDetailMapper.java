package com.xwms.core.wave.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.wave.entity.WaveDetail;

@Mapper
public interface WaveDetailMapper extends BaseMapper<WaveDetail> {

    @Select(
            "SELECT * FROM wms_wave_detail WHERE wave_no = #{waveNo} ORDER BY priority DESC, created_time")
    List<WaveDetail> selectByWaveNo(@Param("waveNo") String waveNo);

    @Select("SELECT * FROM wms_wave_detail WHERE outbound_no = #{outboundNo}")
    WaveDetail selectByOutboundNo(@Param("outboundNo") String outboundNo);
}
