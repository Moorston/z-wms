package com.xwms.integration.external.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.integration.external.entity.ApiCallLog;

@Mapper
public interface ApiCallLogMapper extends BaseMapper<ApiCallLog> {

    @Select(
            "SELECT * FROM wms_api_call_log WHERE system_code = #{systemCode} ORDER BY created_time DESC")
    List<ApiCallLog> selectBySystem(@Param("systemCode") String systemCode);
}
