package com.xwms.core.abc.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.abc.entity.AbcHistory;

@Mapper
public interface AbcHistoryMapper extends BaseMapper<AbcHistory> {

    @Select("SELECT * FROM wms_abc_history WHERE sku_code = #{skuCode} ORDER BY change_time DESC")
    List<AbcHistory> selectBySku(@Param("skuCode") String skuCode);

    @Select(
            "SELECT * FROM wms_abc_history WHERE warehouse_code = #{warehouseCode} ORDER BY change_time DESC")
    List<AbcHistory> selectByWarehouse(@Param("warehouseCode") String warehouseCode);
}
