package com.xwms.core.alert.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.alert.entity.AlertRecord;

@Mapper
public interface AlertRecordMapper extends BaseMapper<AlertRecord> {

    @Select("SELECT * FROM wms_alert_record WHERE alert_id = #{alertId}")
    AlertRecord selectByAlertId(@Param("alertId") String alertId);

    @Select("SELECT * FROM wms_alert_record WHERE status = #{status} ORDER BY trigger_time DESC")
    List<AlertRecord> selectByStatus(@Param("status") String status);

    @Select(
            "SELECT * FROM wms_alert_record WHERE alert_type = #{alertType} AND status IN ('PENDING','PROCESSING') ORDER BY trigger_time DESC")
    List<AlertRecord> selectActiveByType(@Param("alertType") String alertType);

    @Select(
            "SELECT * FROM wms_alert_record WHERE sku_code = #{skuCode} ORDER BY trigger_time DESC LIMIT #{limit}")
    List<AlertRecord> selectRecentBySku(
            @Param("skuCode") String skuCode, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM wms_alert_record WHERE status IN ('PENDING','PROCESSING')")
    int countActive();

    @Select(
            "SELECT COUNT(*) FROM wms_alert_record WHERE alert_level = #{level} AND status IN ('PENDING','PROCESSING')")
    int countActiveByLevel(@Param("level") String level);

    @Update(
            "UPDATE wms_alert_record SET status = #{status}, resolve_time = NOW(), resolved_by = #{operator}, resolve_note = #{note} WHERE alert_id = #{alertId}")
    int updateStatus(
            @Param("alertId") String alertId,
            @Param("status") String status,
            @Param("operator") String operator,
            @Param("note") String note);

    @Select(
            "SELECT * FROM wms_alert_record WHERE alert_type = #{alertType} AND business_no = #{businessNo} AND status IN ('PENDING','PROCESSING') LIMIT 1")
    AlertRecord selectPendingByTypeAndBiz(
            @Param("alertType") String alertType, @Param("businessNo") String businessNo);

    @Select(
            "SELECT * FROM wms_alert_record WHERE status IN ('PENDING','PROCESSING') ORDER BY trigger_time DESC")
    List<AlertRecord> selectPendingAlerts();
}
