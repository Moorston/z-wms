package com.xwms.core.metrics.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.metrics.entity.MetricsCategory;

@Mapper
public interface MetricsCategoryMapper extends BaseMapper<MetricsCategory> {
    @Select("SELECT * FROM wms_metrics_category WHERE category_id = #{categoryId}")
    MetricsCategory selectByCategoryId(@Param("categoryId") String categoryId);

    @Select("SELECT * FROM wms_metrics_category WHERE category_code = #{categoryCode}")
    MetricsCategory selectByCategoryCode(@Param("categoryCode") String categoryCode);

    @Select(
            "SELECT * FROM wms_metrics_category WHERE system_id = #{systemId} AND is_active = 'Y' ORDER BY category_level, sort_order")
    List<MetricsCategory> selectActiveBySystem(@Param("systemId") String systemId);

    @Select(
            "SELECT * FROM wms_metrics_category WHERE parent_category_id = #{parentCategoryId} AND is_active = 'Y' ORDER BY sort_order")
    List<MetricsCategory> selectActiveByParent(@Param("parentCategoryId") String parentCategoryId);
}
