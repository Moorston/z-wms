package com.xwms.core.stockdiff.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.stockdiff.entity.StockDiffAnalysis;

@Mapper
public interface StockDiffAnalysisMapper extends BaseMapper<StockDiffAnalysis> {

    @Select("SELECT * FROM wms_stock_diff_analysis WHERE analysis_id = #{analysisId}")
    StockDiffAnalysis selectByAnalysisId(@Param("analysisId") String analysisId);

    @Select(
            "SELECT * FROM wms_stock_diff_analysis WHERE analysis_date = #{analysisDate} AND analysis_type = #{analysisType}")
    List<StockDiffAnalysis> selectByDateAndType(
            @Param("analysisDate") LocalDate analysisDate,
            @Param("analysisType") String analysisType);

    @Select(
            "SELECT * FROM wms_stock_diff_analysis WHERE warehouse_code = #{warehouseCode} AND analysis_date BETWEEN #{startDate} AND #{endDate} ORDER BY analysis_date")
    List<StockDiffAnalysis> selectByWarehouseAndDateRange(
            @Param("warehouseCode") String warehouseCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
