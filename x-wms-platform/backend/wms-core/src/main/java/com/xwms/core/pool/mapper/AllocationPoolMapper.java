package com.xwms.core.pool.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.pool.entity.AllocationPool;

@Mapper
public interface AllocationPoolMapper extends BaseMapper<AllocationPool> {

    @Select("SELECT * FROM wms_allocation_pool WHERE pool_code = #{poolCode}")
    AllocationPool selectByPoolCode(@Param("poolCode") String poolCode);

    @Select(
            "SELECT * FROM wms_allocation_pool WHERE warehouse_code = #{warehouseCode} AND status = 'ACTIVE' ORDER BY priority DESC")
    List<AllocationPool> selectByWarehouse(@Param("warehouseCode") String warehouseCode);

    @Select(
            "SELECT * FROM wms_allocation_pool WHERE warehouse_code = #{warehouseCode} AND pool_type = #{poolType} AND status = 'ACTIVE'")
    List<AllocationPool> selectByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode, @Param("poolType") String poolType);

    @Select(
            "SELECT * FROM wms_allocation_pool WHERE owner_code = #{ownerCode} AND status = 'ACTIVE'")
    List<AllocationPool> selectByOwner(@Param("ownerCode") String ownerCode);
}
