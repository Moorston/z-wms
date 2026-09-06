package com.xwms.core.reserve.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.reserve.entity.InventoryReserveLog;

@Mapper
public interface InventoryReserveLogMapper extends BaseMapper<InventoryReserveLog> {

    @Select(
            "SELECT * FROM wms_inventory_reserve_log WHERE reserve_no = #{reserveNo} ORDER BY action_time")
    List<InventoryReserveLog> selectByReserveNo(@Param("reserveNo") String reserveNo);

    @Select(
            "SELECT * FROM wms_inventory_reserve_log WHERE sku_code = #{skuCode} AND batch_no = #{batchNo} AND location_code = #{locationCode} ORDER BY action_time DESC")
    List<InventoryReserveLog> selectBySkuLocation(
            @Param("skuCode") String skuCode,
            @Param("batchNo") String batchNo,
            @Param("locationCode") String locationCode);
}
