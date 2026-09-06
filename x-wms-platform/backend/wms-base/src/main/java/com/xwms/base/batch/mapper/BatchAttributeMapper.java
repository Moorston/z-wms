package com.xwms.base.batch.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.batch.entity.BatchAttribute;

@Mapper
public interface BatchAttributeMapper extends BaseMapper<BatchAttribute> {

    @Select(
            "SELECT * FROM wms_batch_attribute WHERE attr_category = #{category} AND enabled = 1 ORDER BY sort_order")
    List<BatchAttribute> selectByCategory(@Param("category") String category);

    @Select(
            "SELECT * FROM wms_batch_attribute WHERE enabled = 1 ORDER BY attr_category, sort_order")
    List<BatchAttribute> selectAllEnabled();
}
