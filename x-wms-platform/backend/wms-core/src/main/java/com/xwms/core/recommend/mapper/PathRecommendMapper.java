package com.xwms.core.recommend.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.recommend.entity.PathRecommend;

@Mapper
public interface PathRecommendMapper extends BaseMapper<PathRecommend> {

    @Select("SELECT * FROM wms_path_recommend WHERE recommend_id = #{recommendId}")
    PathRecommend selectByRecommendId(@Param("recommendId") String recommendId);

    @Select(
            "SELECT * FROM wms_path_recommend WHERE wave_id = #{waveId} ORDER BY recommend_time DESC")
    List<PathRecommend> selectByWaveId(@Param("waveId") String waveId);

    @Select(
            "SELECT * FROM wms_path_recommend WHERE picker_id = #{pickerId} AND status = 'PENDING' ORDER BY priority DESC, recommend_time DESC")
    List<PathRecommend> selectPendingByPicker(@Param("pickerId") String pickerId);

    @Select(
            "SELECT * FROM wms_path_recommend WHERE warehouse_code = #{warehouseCode} AND status = #{status} ORDER BY priority DESC, recommend_time DESC")
    List<PathRecommend> selectByWarehouseAndStatus(
            @Param("warehouseCode") String warehouseCode, @Param("status") String status);
}
