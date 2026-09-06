package com.xwms.core.recommend.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.recommend.entity.WaveRecommend;

@Mapper
public interface WaveRecommendMapper extends BaseMapper<WaveRecommend> {

    @Select("SELECT * FROM wms_wave_recommend WHERE recommend_id = #{recommendId}")
    WaveRecommend selectByRecommendId(@Param("recommendId") String recommendId);

    @Select(
            "SELECT * FROM wms_wave_recommend WHERE warehouse_code = #{warehouseCode} AND wave_type = #{waveType} AND status = 'PENDING' ORDER BY priority DESC, recommend_time DESC")
    List<WaveRecommend> selectPendingByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode, @Param("waveType") String waveType);

    @Select(
            "SELECT * FROM wms_wave_recommend WHERE warehouse_code = #{warehouseCode} AND status = #{status} ORDER BY priority DESC, recommend_time DESC")
    List<WaveRecommend> selectByWarehouseAndStatus(
            @Param("warehouseCode") String warehouseCode, @Param("status") String status);

    @Select(
            "SELECT * FROM wms_wave_recommend WHERE warehouse_code = #{warehouseCode} AND priority = #{priority} AND status = 'PENDING' ORDER BY recommend_time DESC")
    List<WaveRecommend> selectByWarehouseAndPriority(
            @Param("warehouseCode") String warehouseCode, @Param("priority") String priority);
}
