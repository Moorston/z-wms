package com.xwms.core.abc.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.abc.entity.AbcClassification;

@Mapper
public interface AbcClassificationMapper extends BaseMapper<AbcClassification> {

    @Select(
            "SELECT * FROM wms_abc_classification WHERE warehouse_code = #{warehouseCode} AND status = 'CURRENT' ORDER BY rank_no")
    List<AbcClassification> selectCurrentByWarehouse(@Param("warehouseCode") String warehouseCode);

    @Select(
            "SELECT * FROM wms_abc_classification WHERE sku_code = #{skuCode} AND status = 'CURRENT'")
    AbcClassification selectCurrentBySku(@Param("skuCode") String skuCode);

    @Select(
            "SELECT * FROM wms_abc_classification WHERE warehouse_code = #{warehouseCode} AND abc_class = #{abcClass} AND status = 'CURRENT' ORDER BY rank_no")
    List<AbcClassification> selectByClass(
            @Param("warehouseCode") String warehouseCode, @Param("abcClass") String abcClass);

    /** 将当前分类标记为历史 */
    @Update(
            "UPDATE wms_abc_classification SET status = 'HISTORY' WHERE warehouse_code = #{warehouseCode} AND status = 'CURRENT'")
    int markAsHistory(@Param("warehouseCode") String warehouseCode);
}
