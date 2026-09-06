package com.xwms.core.plugin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.plugin.entity.Plugin;

@Mapper
public interface PluginMapper extends BaseMapper<Plugin> {

    @Select(
            "SELECT * FROM wms_plugin WHERE industry = #{industry} AND status = 'ENABLED' ORDER BY priority")
    List<Plugin> selectEnabledByIndustry(@Param("industry") String industry);

    @Select("SELECT * FROM wms_plugin WHERE status = 'ENABLED' ORDER BY priority")
    List<Plugin> selectAllEnabled();

    @Select("SELECT * FROM wms_plugin WHERE plugin_code = #{code}")
    Plugin selectByCode(@Param("code") String code);
}
