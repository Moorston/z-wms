package com.xwms.core.transaction.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.transaction.entity.InventoryTxnSummary;

@Mapper
public interface InventoryTxnSummaryMapper extends BaseMapper<InventoryTxnSummary> {

    @Select(
            "SELECT * FROM wms_inventory_txn_summary WHERE summary_date = #{summaryDate} AND warehouse_code = #{warehouseCode} ORDER BY sku_code")
    List<InventoryTxnSummary> selectByDateAndWarehouse(
            @Param("summaryDate") LocalDate summaryDate,
            @Param("warehouseCode") String warehouseCode);

    @Select(
            "SELECT * FROM wms_inventory_txn_summary WHERE sku_code = #{skuCode} AND warehouse_code = #{warehouseCode} AND summary_date BETWEEN #{startDate} AND #{endDate} ORDER BY summary_date")
    List<InventoryTxnSummary> selectBySkuAndDateRange(
            @Param("skuCode") String skuCode,
            @Param("warehouseCode") String warehouseCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
