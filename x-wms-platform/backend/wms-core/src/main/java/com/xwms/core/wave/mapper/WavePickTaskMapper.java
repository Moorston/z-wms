package com.xwms.core.wave.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.wave.entity.WavePickTask;

@Mapper
public interface WavePickTaskMapper extends BaseMapper<WavePickTask> {

    @Select("SELECT * FROM wms_wave_pick_task WHERE wave_no = #{waveNo} ORDER BY path_order")
    List<WavePickTask> selectByWaveNo(@Param("waveNo") String waveNo);

    @Select("SELECT * FROM wms_wave_pick_task WHERE task_no = #{taskNo}")
    WavePickTask selectByTaskNo(@Param("taskNo") String taskNo);

    @Select(
            "SELECT * FROM wms_wave_pick_task WHERE wave_no = #{waveNo} AND status = 'PENDING' ORDER BY path_order")
    List<WavePickTask> selectPendingByWaveNo(@Param("waveNo") String waveNo);
}
