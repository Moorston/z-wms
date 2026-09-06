package com.xwms.core.alert.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.alert.entity.AlertRule;

@Mapper
public interface AlertRuleMapper extends BaseMapper<AlertRule> {

    @Select("SELECT * FROM wms_alert_rule WHERE rule_code = #{ruleCode}")
    AlertRule selectByRuleCode(@Param("ruleCode") String ruleCode);

    @Select("SELECT * FROM wms_alert_rule WHERE alert_type = #{alertType} AND status = 'ACTIVE'")
    List<AlertRule> selectByAlertType(@Param("alertType") String alertType);

    @Select(
            "SELECT * FROM wms_alert_rule WHERE warehouse_code = #{warehouseCode} AND status = 'ACTIVE'")
    List<AlertRule> selectByWarehouse(@Param("warehouseCode") String warehouseCode);

    @Select(
            "SELECT * FROM wms_alert_rule WHERE check_frequency = #{frequency} AND status = 'ACTIVE'")
    List<AlertRule> selectByFrequency(@Param("frequency") String frequency);

    @Select("SELECT * FROM wms_alert_rule WHERE sku_code = #{skuCode} AND status = 'ACTIVE'")
    List<AlertRule> selectBySku(@Param("skuCode") String skuCode);

    @Select("SELECT * FROM wms_alert_rule WHERE alert_type = #{category} AND status = 'ACTIVE'")
    List<AlertRule> selectEnabledByCategory(@Param("category") String category);

    @Select("SELECT * FROM wms_alert_rule WHERE status = 'ACTIVE'")
    List<AlertRule> selectAllEnabled();
}
