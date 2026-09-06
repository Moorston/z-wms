package com.xwms.core.qc.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.qc.entity.QcItem;

@Mapper
public interface QcItemMapper extends BaseMapper<QcItem> {

    @Select("SELECT * FROM wms_qc_item WHERE qc_id = #{qcId} ORDER BY sort_order")
    List<QcItem> selectByQcId(@Param("qcId") Long qcId);
}
