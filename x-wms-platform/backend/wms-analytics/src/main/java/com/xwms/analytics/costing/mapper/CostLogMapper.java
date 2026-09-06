package com.xwms.analytics.costing.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.analytics.costing.entity.CostLog;

@Mapper
public interface CostLogMapper extends BaseMapper<CostLog> {

    @Select("SELECT * FROM wms_cost_log WHERE sku_code = #{skuCode} ORDER BY action_time DESC")
    List<CostLog> selectBySku(@Param("skuCode") String skuCode);

    @Select(
            "SELECT * FROM wms_cost_log WHERE ref_type = #{refType} AND ref_no = #{refNo} ORDER BY action_time")
    List<CostLog> selectByRef(@Param("refType") String refType, @Param("refNo") String refNo);
}
