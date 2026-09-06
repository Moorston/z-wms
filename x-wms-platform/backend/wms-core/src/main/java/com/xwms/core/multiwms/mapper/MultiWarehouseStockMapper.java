package com.xwms.core.multiwms.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.multiwms.entity.MultiWarehouseStock;

@Mapper
public interface MultiWarehouseStockMapper extends BaseMapper<MultiWarehouseStock> {

    @Select(
            "SELECT * FROM wms_multi_warehouse_stock WHERE sku = #{sku} ORDER BY available_qty DESC")
    List<MultiWarehouseStock> selectBySku(@Param("sku") String sku);

    @Select(
            "SELECT * FROM wms_multi_warehouse_stock WHERE warehouse_code = #{warehouse} ORDER BY sku")
    List<MultiWarehouseStock> selectByWarehouse(@Param("warehouse") String warehouse);
}
