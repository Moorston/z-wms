package com.xwms.core.putaway.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.putaway.entity.PutawayRecommendLog;

/** 上架推荐日志Mapper */
@Mapper
public interface PutawayRecommendLogMapper extends BaseMapper<PutawayRecommendLog> {

    @Select(
            "SELECT * FROM wms_putaway_recommend_log WHERE task_no = #{taskNo} ORDER BY recommend_time DESC")
    List<PutawayRecommendLog> selectByTaskNo(@Param("taskNo") String taskNo);

    @Select(
            "SELECT * FROM wms_putaway_recommend_log WHERE sku_code = #{skuCode} AND warehouse_code = #{warehouseCode} ORDER BY recommend_time DESC LIMIT #{limit}")
    List<PutawayRecommendLog> selectRecentBySku(
            @Param("skuCode") String skuCode,
            @Param("warehouseCode") String warehouseCode,
            @Param("limit") int limit);
}
