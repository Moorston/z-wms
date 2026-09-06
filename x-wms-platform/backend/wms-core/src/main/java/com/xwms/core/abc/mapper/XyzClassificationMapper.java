package com.xwms.core.abc.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.abc.entity.XyzClassification;

@Mapper
public interface XyzClassificationMapper extends BaseMapper<XyzClassification> {

    @Select(
            "SELECT * FROM wms_xyz_classification WHERE warehouse_code = #{warehouseCode} AND status = 'CURRENT' ORDER BY cv")
    List<XyzClassification> selectCurrentByWarehouse(@Param("warehouseCode") String warehouseCode);

    @Select(
            "SELECT * FROM wms_xyz_classification WHERE sku_code = #{skuCode} AND status = 'CURRENT'")
    XyzClassification selectCurrentBySku(@Param("skuCode") String skuCode);

    @Update(
            "UPDATE wms_xyz_classification SET status = 'HISTORY' WHERE warehouse_code = #{warehouseCode} AND status = 'CURRENT'")
    int markAsHistory(@Param("warehouseCode") String warehouseCode);
}
