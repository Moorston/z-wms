package com.xwms.core.pack.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.pack.entity.Pack;

@Mapper
public interface PackMapper extends BaseMapper<Pack> {

    @Select("SELECT * FROM wms_pack WHERE pack_no = #{packNo}")
    Pack selectByPackNo(@Param("packNo") String packNo);

    @Select("SELECT * FROM wms_pack WHERE outbound_no = #{outboundNo}")
    Pack selectByOutboundNo(@Param("outboundNo") String outboundNo);

    @Select("SELECT * FROM wms_pack WHERE wave_no = #{waveNo}")
    List<Pack> selectByWaveNo(@Param("waveNo") String waveNo);

    @Select("SELECT * FROM wms_pack WHERE status = #{status} ORDER BY created_time")
    List<Pack> selectByStatus(@Param("status") String status);
}
