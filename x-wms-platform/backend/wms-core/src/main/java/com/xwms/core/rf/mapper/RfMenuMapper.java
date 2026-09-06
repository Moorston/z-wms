package com.xwms.core.rf.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.rf.entity.RfMenu;

@Mapper
public interface RfMenuMapper extends BaseMapper<RfMenu> {

    @Select("SELECT * FROM wms_rf_menu WHERE enabled = 1 ORDER BY parent_code, sort_order")
    List<RfMenu> selectAllEnabled();

    @Select(
            "SELECT * FROM wms_rf_menu WHERE parent_code = #{parentCode} AND enabled = 1 ORDER BY sort_order")
    List<RfMenu> selectByParent(@Param("parentCode") String parentCode);
}
