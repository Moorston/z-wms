package com.xwms.core.freeze.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.freeze.entity.InventoryFreezeDetail;

@Mapper
public interface InventoryFreezeDetailMapper extends BaseMapper<InventoryFreezeDetail> {

    @Select(
            "SELECT * FROM wms_inventory_freeze_detail WHERE freeze_no = #{freezeNo} ORDER BY sku_code, location_code")
    List<InventoryFreezeDetail> selectByFreezeNo(@Param("freezeNo") String freezeNo);

    @Select(
            "SELECT * FROM wms_inventory_freeze_detail WHERE sku_code = #{skuCode} AND status IN ('FROZEN','PARTIAL') ORDER BY freeze_time DESC")
    List<InventoryFreezeDetail> selectFrozenBySku(@Param("skuCode") String skuCode);

    @Select(
            "SELECT * FROM wms_inventory_freeze_detail WHERE location_code = #{locationCode} AND status IN ('FROZEN','PARTIAL')")
    List<InventoryFreezeDetail> selectFrozenByLocation(@Param("locationCode") String locationCode);

    @Select(
            "SELECT * FROM wms_inventory_freeze_detail WHERE batch_no = #{batchNo} AND status IN ('FROZEN','PARTIAL')")
    List<InventoryFreezeDetail> selectFrozenByBatch(@Param("batchNo") String batchNo);

    @Update(
            "UPDATE wms_inventory_freeze_detail SET unfreeze_qty = #{unfreezeQty}, remain_qty = #{remainQty}, status = #{status}, unfreeze_time = NOW() WHERE id = #{id}")
    int updateUnfreeze(
            @Param("id") Long id,
            @Param("unfreezeQty") java.math.BigDecimal unfreezeQty,
            @Param("remainQty") java.math.BigDecimal remainQty,
            @Param("status") String status);
}
