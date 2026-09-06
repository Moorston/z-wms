package com.xwms.integration.external.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.integration.external.entity.ExternalSystem;

@Mapper
public interface ExternalSystemMapper extends BaseMapper<ExternalSystem> {

    @Select("SELECT * FROM wms_external_system WHERE system_type = #{type} AND status = 'ACTIVE'")
    List<ExternalSystem> selectActiveByType(@Param("type") String type);

    @Select("SELECT * FROM wms_external_system WHERE system_code = #{code}")
    ExternalSystem selectByCode(@Param("code") String code);
}
