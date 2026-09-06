package com.xwms.core.expiry.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.expiry.entity.ExpiryAlert;

@Mapper
public interface ExpiryAlertMapper extends BaseMapper<ExpiryAlert> {

    @Select("SELECT * FROM wms_expiry_alert WHERE alert_no = #{alertNo}")
    ExpiryAlert selectByAlertNo(@Param("alertNo") String alertNo);

    @Update(
            "UPDATE wms_expiry_alert SET status = #{status}, handle_action = #{handleAction}, handled_by = #{handledBy}, handled_time = NOW() WHERE alert_no = #{alertNo}")
    int updateStatus(
            @Param("alertNo") String alertNo,
            @Param("status") String status,
            @Param("handleAction") String handleAction,
            @Param("handledBy") String handledBy);
}
