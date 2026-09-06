package com.xwms.core.dashboard.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.dashboard.entity.DashboardTemplate;

@Mapper
public interface DashboardTemplateMapper extends BaseMapper<DashboardTemplate> {

    @Select("SELECT * FROM wms_dashboard_template WHERE template_code = #{templateCode}")
    DashboardTemplate selectByTemplateCode(@Param("templateCode") String templateCode);

    @Select(
            "SELECT * FROM wms_dashboard_template WHERE template_type = #{templateType} AND status = 'ACTIVE'")
    List<DashboardTemplate> selectByType(@Param("templateType") String templateType);

    @Select(
            "SELECT * FROM wms_dashboard_template WHERE industry = #{industry} AND status = 'ACTIVE'")
    List<DashboardTemplate> selectByIndustry(@Param("industry") String industry);

    @Select("SELECT * FROM wms_dashboard_template WHERE is_builtin = 'Y' AND status = 'ACTIVE'")
    List<DashboardTemplate> selectBuiltin();

    @Select(
            "SELECT * FROM wms_dashboard_template WHERE template_type = #{templateType} AND industry = #{industry} AND status = 'ACTIVE'")
    List<DashboardTemplate> selectByTypeAndIndustry(
            @Param("templateType") String templateType, @Param("industry") String industry);
}
