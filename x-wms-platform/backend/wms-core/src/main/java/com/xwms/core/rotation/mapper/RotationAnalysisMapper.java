package com.xwms.core.rotation.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.rotation.entity.RotationAnalysis;

@Mapper
public interface RotationAnalysisMapper extends BaseMapper<RotationAnalysis> {

    @Select(
            "SELECT * FROM wms_rotation_analysis WHERE analysis_date = #{analysisDate} AND period_type = #{periodType} AND sku_code = #{skuCode}")
    RotationAnalysis selectByDateAndSku(
            @Param("analysisDate") LocalDateTime analysisDate,
            @Param("periodType") String periodType,
            @Param("skuCode") String skuCode);

    @Select(
            "SELECT * FROM wms_rotation_analysis WHERE analysis_date BETWEEN #{startDate} AND #{endDate} AND period_type = #{periodType} ORDER BY analysis_date")
    List<RotationAnalysis> selectByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("periodType") String periodType);
}
