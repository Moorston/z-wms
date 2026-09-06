package com.xwms.integration.gateway.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.integration.gateway.entity.ApiRateLimit;

@Mapper
public interface ApiRateLimitMapper extends BaseMapper<ApiRateLimit> {

    @Select(
            "SELECT * FROM wms_api_rate_limit WHERE target_type = #{targetType} AND target_value = #{targetValue} AND enabled = 1")
    ApiRateLimit selectByTarget(
            @Param("targetType") String targetType, @Param("targetValue") String targetValue);

    @Select("SELECT * FROM wms_api_rate_limit WHERE enabled = 1 ORDER BY target_type, target_value")
    List<ApiRateLimit> selectAllEnabled();
}
