package com.xwms.core.transaction.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.transaction.entity.InventoryTransaction;

@Mapper
public interface InventoryTransactionMapper extends BaseMapper<InventoryTransaction> {

    @Select("SELECT * FROM wms_inventory_transaction WHERE txn_no = #{txnNo}")
    InventoryTransaction selectByTxnNo(@Param("txnNo") String txnNo);

    @Select(
            "SELECT * FROM wms_inventory_transaction WHERE business_no = #{businessNo} ORDER BY operate_time")
    List<InventoryTransaction> selectByBusinessNo(@Param("businessNo") String businessNo);

    @Select(
            "SELECT * FROM wms_inventory_transaction WHERE sku_code = #{skuCode} AND warehouse_code = #{warehouseCode} AND operate_time BETWEEN #{startTime} AND #{endTime} ORDER BY operate_time")
    List<InventoryTransaction> selectBySkuAndTime(
            @Param("skuCode") String skuCode,
            @Param("warehouseCode") String warehouseCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Select(
            "SELECT * FROM wms_inventory_transaction WHERE batch_no = #{batchNo} ORDER BY operate_time")
    List<InventoryTransaction> selectByBatchNo(@Param("batchNo") String batchNo);

    @Select(
            "SELECT * FROM wms_inventory_transaction WHERE ref_txn_no = #{refTxnNo} ORDER BY operate_time")
    List<InventoryTransaction> selectByRefTxnNo(@Param("refTxnNo") String refTxnNo);
}
