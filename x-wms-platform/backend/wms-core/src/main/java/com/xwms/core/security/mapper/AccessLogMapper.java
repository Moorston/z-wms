package com.xwms.core.security.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.security.entity.AccessLog;

@Mapper
public interface AccessLogMapper extends BaseMapper<AccessLog> {

    @Select("SELECT * FROM wms_access_log WHERE log_id = #{logId}")
    AccessLog selectByLogId(@Param("logId") String logId);

    @Select(
            "SELECT * FROM wms_access_log WHERE user_code = #{userCode} AND access_time >= #{startTime} ORDER BY access_time DESC LIMIT #{limit}")
    List<AccessLog> selectByUserAndTime(
            @Param("userCode") String userCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("limit") int limit);

    @Select(
            "SELECT * FROM wms_access_log WHERE access_type = #{accessType} AND access_time >= #{startTime} ORDER BY access_time DESC LIMIT #{limit}")
    List<AccessLog> selectByTypeAndTime(
            @Param("accessType") String accessType,
            @Param("startTime") LocalDateTime startTime,
            @Param("limit") int limit);

    @Select(
            "SELECT * FROM wms_access_log WHERE ip_address = #{ipAddress} AND access_time >= #{startTime} ORDER BY access_time DESC LIMIT #{limit}")
    List<AccessLog> selectByIpAndTime(
            @Param("ipAddress") String ipAddress,
            @Param("startTime") LocalDateTime startTime,
            @Param("limit") int limit);

    @Select(
            "SELECT COUNT(*) FROM wms_access_log WHERE user_code = #{userCode} AND access_type = 'LOGIN' AND is_success = 'Y' AND access_time >= #{startTime}")
    int countLoginSuccess(
            @Param("userCode") String userCode, @Param("startTime") LocalDateTime startTime);

    @Select(
            "SELECT COUNT(*) FROM wms_access_log WHERE user_code = #{userCode} AND access_type = 'LOGIN' AND is_success = 'N' AND access_time >= #{startTime}")
    int countLoginFailed(
            @Param("userCode") String userCode, @Param("startTime") LocalDateTime startTime);
}
