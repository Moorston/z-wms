package com.xwms.core.adjust.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.adjust.entity.InventoryAdjustLog;

@Mapper
public interface InventoryAdjustLogMapper extends BaseMapper<InventoryAdjustLog> {

    @Select(
            "SELECT * FROM wms_inventory_adjust_log WHERE sku_code = #{skuCode} AND batch_no = #{batchNo} AND location_code = #{locationCode} ORDER BY action_time DESC")
    List<InventoryAdjustLog> selectBySkuLocation(
            @Param("skuCode") String skuCode,
            @Param("batchNo") String batchNo,
            @Param("locationCode") String locationCode);

    @Select(
            "SELECT * FROM wms_inventory_adjust_log WHERE adjust_no = #{adjustNo} ORDER BY action_time")
    List<InventoryAdjustLog> selectByAdjustNo(@Param("adjustNo") String adjustNo);
}
