package com.xwms.core.combo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.combo.entity.ComboRule;

@Mapper
public interface ComboRuleMapper extends BaseMapper<ComboRule> {

    @Select("SELECT * FROM wms_combo_rule WHERE combo_code = #{comboCode}")
    ComboRule selectByComboCode(@Param("comboCode") String comboCode);
}
