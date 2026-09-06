package com.xwms.core.plugin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.plugin.entity.PluginLog;

@Mapper
public interface PluginLogMapper extends BaseMapper<PluginLog> {

    @Select(
            "SELECT * FROM wms_plugin_log WHERE plugin_code = #{pluginCode} ORDER BY created_time DESC")
    List<PluginLog> selectByPlugin(@Param("pluginCode") String pluginCode);
}
