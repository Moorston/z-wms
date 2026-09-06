package com.xwms.integration.gateway.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.integration.gateway.entity.ApiKey;

@Mapper
public interface ApiKeyMapper extends BaseMapper<ApiKey> {

    @Select("SELECT * FROM wms_api_key WHERE app_key = #{appKey}")
    ApiKey selectByAppKey(@Param("appKey") String appKey);

    @Select("SELECT * FROM wms_api_key WHERE status = 'ACTIVE' ORDER BY app_name")
    List<ApiKey> selectActiveKeys();
}
