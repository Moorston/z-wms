package com.xwms.base.serial.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.serial.entity.SerialRule;

@Mapper
public interface SerialRuleMapper extends BaseMapper<SerialRule> {

    @Select("SELECT * FROM wms_serial_rule WHERE rule_code = #{ruleCode}")
    SerialRule selectByRuleCode(@Param("ruleCode") String ruleCode);

    @Select(
            "SELECT * FROM wms_serial_rule WHERE (sku_code = #{skuCode} OR sku_code IS NULL) AND (category_code = #{categoryCode} OR category_code IS NULL) AND status = 'ACTIVE' LIMIT 1")
    SerialRule selectBySkuOrCategory(
            @Param("skuCode") String skuCode, @Param("categoryCode") String categoryCode);

    /** 原子递增当前序号 */
    @Update(
            "UPDATE wms_serial_rule SET current_seq = current_seq + 1, updated_time = NOW() WHERE rule_code = #{ruleCode} AND status = 'ACTIVE'")
    int incrementCurrentSeq(@Param("ruleCode") String ruleCode);
}
