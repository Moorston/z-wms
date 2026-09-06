package com.xwms.core.recommend.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.recommend.entity.LocationRecommend;

@Mapper
public interface LocationRecommendMapper extends BaseMapper<LocationRecommend> {

    @Select("SELECT * FROM wms_location_recommend WHERE recommend_id = #{recommendId}")
    LocationRecommend selectByRecommendId(@Param("recommendId") String recommendId);

    @Select(
            "SELECT * FROM wms_location_recommend WHERE warehouse_code = #{warehouseCode} AND sku_code = #{skuCode} AND status = 'PENDING' ORDER BY recommend_time DESC")
    List<LocationRecommend> selectPendingByWarehouseAndSku(
            @Param("warehouseCode") String warehouseCode, @Param("skuCode") String skuCode);

    @Select(
            "SELECT * FROM wms_location_recommend WHERE biz_type = #{bizType} AND biz_no = #{bizNo}")
    List<LocationRecommend> selectByBiz(
            @Param("bizType") String bizType, @Param("bizNo") String bizNo);

    @Select(
            "SELECT * FROM wms_location_recommend WHERE warehouse_code = #{warehouseCode} AND status = #{status} ORDER BY recommend_time DESC")
    List<LocationRecommend> selectByWarehouseAndStatus(
            @Param("warehouseCode") String warehouseCode, @Param("status") String status);
}
