package com.xwms.core.recommend.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.recommend.entity.ReplenishRecommend;

@Mapper
public interface ReplenishRecommendMapper extends BaseMapper<ReplenishRecommend> {

    @Select("SELECT * FROM wms_replenish_recommend WHERE recommend_id = #{recommendId}")
    ReplenishRecommend selectByRecommendId(@Param("recommendId") String recommendId);

    @Select(
            "SELECT * FROM wms_replenish_recommend WHERE warehouse_code = #{warehouseCode} AND sku_code = #{skuCode} AND status = 'PENDING' ORDER BY priority DESC, recommend_time DESC")
    List<ReplenishRecommend> selectPendingByWarehouseAndSku(
            @Param("warehouseCode") String warehouseCode, @Param("skuCode") String skuCode);

    @Select(
            "SELECT * FROM wms_replenish_recommend WHERE warehouse_code = #{warehouseCode} AND status = #{status} ORDER BY priority DESC, recommend_time DESC")
    List<ReplenishRecommend> selectByWarehouseAndStatus(
            @Param("warehouseCode") String warehouseCode, @Param("status") String status);

    @Select(
            "SELECT * FROM wms_replenish_recommend WHERE warehouse_code = #{warehouseCode} AND priority = #{priority} AND status = 'PENDING' ORDER BY recommend_time DESC")
    List<ReplenishRecommend> selectByWarehouseAndPriority(
            @Param("warehouseCode") String warehouseCode, @Param("priority") String priority);
}
