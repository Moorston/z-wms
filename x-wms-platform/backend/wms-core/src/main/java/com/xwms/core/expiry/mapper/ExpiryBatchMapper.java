package com.xwms.core.expiry.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.expiry.entity.ExpiryBatch;

@Mapper
public interface ExpiryBatchMapper extends BaseMapper<ExpiryBatch> {

    @Select(
            "SELECT * FROM wms_expiry_batch WHERE batch_no = #{batchNo} AND warehouse_code = #{warehouseCode} AND sku_code = #{skuCode}")
    ExpiryBatch selectByBatch(
            @Param("batchNo") String batchNo,
            @Param("warehouseCode") String warehouseCode,
            @Param("skuCode") String skuCode);

    @Select(
            "SELECT * FROM wms_expiry_batch WHERE sku_code = #{skuCode} AND warehouse_code = #{warehouseCode} AND expiry_status != 'EXPIRED' ORDER BY expiry_date")
    List<ExpiryBatch> selectFefoBySku(
            @Param("skuCode") String skuCode, @Param("warehouseCode") String warehouseCode);

    @Select(
            "SELECT * FROM wms_expiry_batch WHERE warehouse_code = #{warehouseCode} AND expiry_date <= #{expiryDate} AND expiry_status != 'EXPIRED'")
    List<ExpiryBatch> selectExpiringBatches(
            @Param("warehouseCode") String warehouseCode,
            @Param("expiryDate") LocalDate expiryDate);

    @Select(
            "SELECT * FROM wms_expiry_batch WHERE warehouse_code = #{warehouseCode} AND warning_level != 'NORMAL' AND expiry_status != 'EXPIRED' ORDER BY remain_days")
    List<ExpiryBatch> selectWarningBatches(@Param("warehouseCode") String warehouseCode);

    @Update(
            "UPDATE wms_expiry_batch SET remain_days = #{remainDays}, warning_level = #{warningLevel}, expiry_status = #{expiryStatus}, updated_time = NOW() WHERE id = #{id}")
    int updateExpiryStatus(
            @Param("id") Long id,
            @Param("remainDays") int remainDays,
            @Param("warningLevel") String warningLevel,
            @Param("expiryStatus") String expiryStatus);
}
