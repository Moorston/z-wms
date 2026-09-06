package com.xwms.core.plugin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.plugin.entity.PluginConfig;

@Mapper
public interface PluginConfigMapper extends BaseMapper<PluginConfig> {

    @Select("SELECT * FROM wms_plugin_config WHERE plugin_code = #{pluginCode} ORDER BY config_key")
    List<PluginConfig> selectByPlugin(@Param("pluginCode") String pluginCode);
}
