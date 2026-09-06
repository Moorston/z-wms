package com.xwms.core.label.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.label.entity.LabelTemplate;

@Mapper
public interface LabelTemplateMapper extends BaseMapper<LabelTemplate> {

    @Select("SELECT * FROM wms_label_template WHERE template_code = #{templateCode}")
    LabelTemplate selectByTemplateCode(@Param("templateCode") String templateCode);

    @Select(
            "SELECT * FROM wms_label_template WHERE template_type = #{templateType} AND status = 'ACTIVE'")
    List<LabelTemplate> selectByTemplateType(@Param("templateType") String templateType);

    @Select(
            "SELECT * FROM wms_label_template WHERE warehouse_code = #{warehouseCode} AND status = 'ACTIVE'")
    List<LabelTemplate> selectByWarehouse(@Param("warehouseCode") String warehouseCode);
}
