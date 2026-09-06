package com.xwms.integration.gateway.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.integration.gateway.entity.ApiCallLog;

@Mapper
public interface ApiCallLogMapper extends BaseMapper<ApiCallLog> {

    @Select(
            "SELECT * FROM wms_api_call_log WHERE api_code = #{apiCode} ORDER BY call_time DESC LIMIT 100")
    List<ApiCallLog> selectByApiCode(@Param("apiCode") String apiCode);

    @Select(
            "SELECT * FROM wms_api_call_log WHERE app_key = #{appKey} ORDER BY call_time DESC LIMIT 100")
    List<ApiCallLog> selectByAppKey(@Param("appKey") String appKey);
}
