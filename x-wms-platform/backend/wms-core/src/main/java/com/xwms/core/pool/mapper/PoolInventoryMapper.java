package com.xwms.core.pool.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.pool.entity.PoolInventory;

@Mapper
public interface PoolInventoryMapper extends BaseMapper<PoolInventory> {

    @Select("SELECT * FROM wms_pool_inventory WHERE pool_code = #{poolCode} AND status = 'ACTIVE'")
    List<PoolInventory> selectByPoolCode(@Param("poolCode") String poolCode);

    @Select(
            "SELECT * FROM wms_pool_inventory WHERE pool_code = #{poolCode} AND sku_code = #{skuCode} AND status = 'ACTIVE'")
    List<PoolInventory> selectByPoolAndSku(
            @Param("poolCode") String poolCode, @Param("skuCode") String skuCode);

    @Select(
            "SELECT * FROM wms_pool_inventory WHERE warehouse_code = #{warehouseCode} AND sku_code = #{skuCode} AND status = 'ACTIVE' AND available_qty > 0 ORDER BY pool_code")
    List<PoolInventory> selectAvailableByWarehouseAndSku(
            @Param("warehouseCode") String warehouseCode, @Param("skuCode") String skuCode);

    @Update(
            "UPDATE wms_pool_inventory SET available_qty = available_qty - #{qty}, allocated_qty = allocated_qty + #{qty}, updated_time = NOW() WHERE id = #{id} AND available_qty >= #{qty}")
    int allocateInventory(@Param("id") Long id, @Param("qty") java.math.BigDecimal qty);

    @Update(
            "UPDATE wms_pool_inventory SET available_qty = available_qty + #{qty}, allocated_qty = allocated_qty - #{qty}, updated_time = NOW() WHERE id = #{id}")
    int releaseInventory(@Param("id") Long id, @Param("qty") java.math.BigDecimal qty);
}
