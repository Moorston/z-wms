package com.xwms.core.wave.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.wave.entity.Wave;

@Mapper
public interface WaveMapper extends BaseMapper<Wave> {

    @Select("SELECT * FROM wms_wave WHERE wave_no = #{waveNo}")
    Wave selectByWaveNo(@Param("waveNo") String waveNo);

    @Select("SELECT * FROM wms_wave WHERE status = #{status} ORDER BY priority DESC, created_time")
    List<Wave> selectByStatus(@Param("status") String status);

    @Select(
            "SELECT * FROM wms_wave WHERE warehouse_code = #{warehouseCode} AND status IN ('CREATED','ALLOCATED') ORDER BY priority DESC, created_time")
    List<Wave> selectPendingByWarehouse(@Param("warehouseCode") String warehouseCode);
}
