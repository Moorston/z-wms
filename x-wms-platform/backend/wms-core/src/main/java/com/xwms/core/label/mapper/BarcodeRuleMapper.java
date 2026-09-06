package com.xwms.core.label.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.label.entity.BarcodeRule;

@Mapper
public interface BarcodeRuleMapper extends BaseMapper<BarcodeRule> {

    @Select("SELECT * FROM wms_barcode_rule WHERE rule_code = #{ruleCode}")
    BarcodeRule selectByRuleCode(@Param("ruleCode") String ruleCode);

    @Select(
            "SELECT * FROM wms_barcode_rule WHERE barcode_type = #{barcodeType} AND status = 'ACTIVE'")
    List<BarcodeRule> selectByBarcodeType(@Param("barcodeType") String barcodeType);

    @Select(
            "SELECT * FROM wms_barcode_rule WHERE warehouse_code = #{warehouseCode} AND barcode_type = #{barcodeType} AND status = 'ACTIVE'")
    BarcodeRule selectByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode, @Param("barcodeType") String barcodeType);

    @Update(
            "UPDATE wms_barcode_rule SET sequence_current = sequence_current + 1, updated_time = NOW() WHERE rule_code = #{ruleCode}")
    int incrementSequence(@Param("ruleCode") String ruleCode);
}
