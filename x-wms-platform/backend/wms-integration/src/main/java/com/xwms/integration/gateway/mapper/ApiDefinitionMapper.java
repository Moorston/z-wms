package com.xwms.integration.gateway.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.integration.gateway.entity.ApiDefinition;

@Mapper
public interface ApiDefinitionMapper extends BaseMapper<ApiDefinition> {

    @Select(
            "SELECT * FROM wms_api_definition WHERE api_category = #{category} AND enabled = 1 ORDER BY api_code")
    List<ApiDefinition> selectByCategory(@Param("category") String category);

    @Select("SELECT * FROM wms_api_definition WHERE api_code = #{code}")
    ApiDefinition selectByCode(@Param("code") String code);

    @Select("SELECT * FROM wms_api_definition WHERE api_path = #{path} AND api_method = #{method}")
    ApiDefinition selectByPathAndMethod(@Param("path") String path, @Param("method") String method);
}
