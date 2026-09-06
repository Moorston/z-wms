package com.xwms.core.combo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.combo.entity.ComboItem;

@Mapper
public interface ComboItemMapper extends BaseMapper<ComboItem> {

    @Select("SELECT * FROM wms_combo_item WHERE combo_code = #{comboCode} ORDER BY sort_no")
    List<ComboItem> selectByComboCode(@Param("comboCode") String comboCode);

    @Select("SELECT * FROM wms_combo_item WHERE item_sku_code = #{skuCode}")
    List<ComboItem> selectByItemSku(@Param("skuCode") String skuCode);
}
