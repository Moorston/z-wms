package com.xwms.core.wave.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.wave.entity.WavePath;

@Mapper
public interface WavePathMapper extends BaseMapper<WavePath> {

    @Select("SELECT * FROM wms_wave_path WHERE wave_no = #{waveNo} ORDER BY path_order")
    List<WavePath> selectByWaveNo(@Param("waveNo") String waveNo);
}
