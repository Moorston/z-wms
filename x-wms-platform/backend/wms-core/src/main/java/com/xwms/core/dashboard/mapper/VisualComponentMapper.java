package com.xwms.core.dashboard.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.dashboard.entity.VisualComponent;

@Mapper
public interface VisualComponentMapper extends BaseMapper<VisualComponent> {
    @Select("SELECT * FROM wms_visual_component WHERE component_id = #{componentId}")
    VisualComponent selectByComponentId(@Param("componentId") String componentId);

    @Select("SELECT * FROM wms_visual_component WHERE component_code = #{componentCode}")
    VisualComponent selectByComponentCode(@Param("componentCode") String componentCode);

    @Select(
            "SELECT * FROM wms_visual_component WHERE component_type = #{componentType} AND is_active = 'Y' ORDER BY sort_order")
    List<VisualComponent> selectActiveByType(@Param("componentType") String componentType);

    @Select(
            "SELECT * FROM wms_visual_component WHERE is_builtin = 'Y' AND is_active = 'Y' ORDER BY component_type, sort_order")
    List<VisualComponent> selectBuiltinActive();
}
