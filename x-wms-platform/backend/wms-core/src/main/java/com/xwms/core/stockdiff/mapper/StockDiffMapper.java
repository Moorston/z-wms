package com.xwms.core.stockdiff.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.stockdiff.entity.StockDiff;

@Mapper
public interface StockDiffMapper extends BaseMapper<StockDiff> {

    @Select("SELECT * FROM wms_stock_diff WHERE diff_id = #{diffId}")
    StockDiff selectByDiffId(@Param("diffId") String diffId);

    @Select(
            "SELECT * FROM wms_stock_diff WHERE stocktake_no = #{stocktakeNo} ORDER BY stocktake_line")
    List<StockDiff> selectByStocktakeNo(@Param("stocktakeNo") String stocktakeNo);

    @Select("SELECT * FROM wms_stock_diff WHERE status = #{status} ORDER BY created_time DESC")
    List<StockDiff> selectByStatus(@Param("status") String status);

    @Select(
            "SELECT * FROM wms_stock_diff WHERE sku_code = #{skuCode} ORDER BY created_time DESC LIMIT #{limit}")
    List<StockDiff> selectRecentBySku(@Param("skuCode") String skuCode, @Param("limit") int limit);

    @Select(
            "SELECT * FROM wms_stock_diff WHERE location_code = #{locationCode} AND status IN ('PENDING','PROCESSING','APPROVING')")
    List<StockDiff> selectActiveByLocation(@Param("locationCode") String locationCode);

    @Select(
            "SELECT COUNT(*) FROM wms_stock_diff WHERE status IN ('PENDING','PROCESSING','APPROVING')")
    int countActive();

    @Select(
            "SELECT COUNT(*) FROM wms_stock_diff WHERE diff_type = #{diffType} AND status IN ('PENDING','PROCESSING','APPROVING')")
    int countActiveByType(@Param("diffType") String diffType);

    @Update(
            "UPDATE wms_stock_diff SET status = #{status}, handler = #{handler}, handle_time = NOW(), updated_time = NOW() WHERE diff_id = #{diffId}")
    int updateStatus(
            @Param("diffId") String diffId,
            @Param("status") String status,
            @Param("handler") String handler);

    @Update(
            "UPDATE wms_stock_diff SET status = 'RESOLVED', adjust_flag = 'Y', adjust_no = #{adjustNo}, handler = #{handler}, handle_time = NOW(), updated_time = NOW() WHERE diff_id = #{diffId}")
    int resolveWithAdjust(
            @Param("diffId") String diffId,
            @Param("adjustNo") String adjustNo,
            @Param("handler") String handler);
}
