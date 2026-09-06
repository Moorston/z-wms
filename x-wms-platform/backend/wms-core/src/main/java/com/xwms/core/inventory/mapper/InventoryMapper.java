package com.xwms.core.inventory.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.inventory.entity.Inventory;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    @Select(
            "SELECT * FROM wms_inventory WHERE warehouse_code = #{warehouseCode} AND location_code = #{locationCode} AND sku_code = #{skuCode} AND batch_no = #{batchNo} AND owner_code_col = #{ownerCode}")
    Inventory selectByUniqueKey(
            @Param("warehouseCode") String warehouseCode,
            @Param("locationCode") String locationCode,
            @Param("skuCode") String skuCode,
            @Param("batchNo") String batchNo,
            @Param("ownerCode") String ownerCode);

    @Select(
            "SELECT * FROM wms_inventory WHERE sku_code = #{skuCode} AND owner_code_col = #{ownerCode} ORDER BY warehouse_code, location_code")
    List<Inventory> selectBySkuAndOwner(
            @Param("skuCode") String skuCode, @Param("ownerCode") String ownerCode);

    /** FIFO查可用库位（available_qty > 0，按入库时间升序）——用于多明细出库分配 */
    @Select(
            "SELECT * FROM wms_inventory WHERE sku_code = #{skuCode} AND owner_code_col = #{ownerCode} AND available_qty > 0 ORDER BY last_in_time ASC")
    List<Inventory> selectAvailableFifo(
            @Param("skuCode") String skuCode, @Param("ownerCode") String ownerCode);

    @Select(
            "SELECT * FROM wms_inventory WHERE warehouse_code = #{warehouseCode} AND owner_code_col = #{ownerCode} ORDER BY location_code, sku_code")
    List<Inventory> selectByWarehouseAndOwner(
            @Param("warehouseCode") String warehouseCode, @Param("ownerCode") String ownerCode);

    /** Oracle直接扣减库存（原子SQL+乐观锁） */
    @Update(
            "UPDATE wms_inventory SET quantity = quantity - #{qty}, available_qty = available_qty - #{qty}, version = version + 1, last_out_time = NOW() WHERE id = #{id} AND version = #{version} AND quantity >= #{qty}")
    int deductInventory(
            @Param("id") Long id, @Param("qty") BigDecimal qty, @Param("version") Integer version);

    /** 核销预占扣减（总量减少，预占核销，可用不动——预占时已减过可用） */
    @Update(
            "UPDATE wms_inventory SET quantity = quantity - #{qty}, allocated_qty = allocated_qty - #{qty}, version = version + 1, last_out_time = NOW() WHERE id = #{id} AND version = #{version} AND allocated_qty >= #{qty}")
    int deductAllocatedInventory(
            @Param("id") Long id, @Param("qty") BigDecimal qty, @Param("version") Integer version);

    /** Oracle直接增加库存（原子SQL+乐观锁） */
    @Update(
            "UPDATE wms_inventory SET quantity = quantity + #{qty}, available_qty = available_qty + #{qty}, version = version + 1, last_in_time = NOW() WHERE id = #{id} AND version = #{version}")
    int addInventory(
            @Param("id") Long id, @Param("qty") BigDecimal qty, @Param("version") Integer version);

    /** 预占库存（可用减少，预占增加） */
    @Update(
            "UPDATE wms_inventory SET available_qty = available_qty - #{qty}, allocated_qty = allocated_qty + #{qty}, version = version + 1 WHERE id = #{id} AND version = #{version} AND available_qty >= #{qty}")
    int allocateInventory(
            @Param("id") Long id, @Param("qty") BigDecimal qty, @Param("version") Integer version);

    /** 释放预占（可用增加，预占减少） */
    @Update(
            "UPDATE wms_inventory SET available_qty = available_qty + #{qty}, allocated_qty = allocated_qty - #{qty}, version = version + 1 WHERE id = #{id} AND version = #{version} AND allocated_qty >= #{qty}")
    int releaseAllocation(
            @Param("id") Long id, @Param("qty") BigDecimal qty, @Param("version") Integer version);

    /** 冻结库存（可用减少，冻结增加） */
    @Update(
            "UPDATE wms_inventory SET available_qty = available_qty - #{qty}, frozen_qty = frozen_qty + #{qty}, version = version + 1 WHERE id = #{id} AND version = #{version} AND available_qty >= #{qty}")
    int freezeInventory(
            @Param("id") Long id, @Param("qty") BigDecimal qty, @Param("version") Integer version);

    /** 解冻库存（可用增加，冻结减少） */
    @Update(
            "UPDATE wms_inventory SET available_qty = available_qty + #{qty}, frozen_qty = frozen_qty - #{qty}, version = version + 1 WHERE id = #{id} AND version = #{version} AND frozen_qty >= #{qty}")
    int unfreezeInventory(
            @Param("id") Long id, @Param("qty") BigDecimal qty, @Param("version") Integer version);
}
