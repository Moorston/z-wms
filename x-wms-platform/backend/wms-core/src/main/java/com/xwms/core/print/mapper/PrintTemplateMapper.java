package com.xwms.core.print.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.print.entity.PrintTemplate;

@Mapper
public interface PrintTemplateMapper extends BaseMapper<PrintTemplate> {

    @Select(
            "SELECT * FROM wms_print_template WHERE template_type = #{type} AND enabled = 1 ORDER BY template_code")
    List<PrintTemplate> selectByType(@Param("type") String type);

    @Select("SELECT * FROM wms_print_template WHERE template_code = #{code} AND enabled = 1")
    PrintTemplate selectByCode(@Param("code") String code);
}
