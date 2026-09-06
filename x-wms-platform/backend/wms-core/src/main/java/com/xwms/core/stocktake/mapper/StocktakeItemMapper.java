package com.xwms.core.stocktake.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.stocktake.entity.StocktakeItem;

@Mapper
public interface StocktakeItemMapper extends BaseMapper<StocktakeItem> {

    @Select(
            "SELECT * FROM wms_stocktake_item WHERE task_id = #{taskId} ORDER BY location_code, sku")
    List<StocktakeItem> selectByTaskId(@Param("taskId") Long taskId);

    @Select(
            "SELECT COUNT(*) FROM wms_stocktake_item WHERE task_id = #{taskId} AND count_status IN ('PENDING','RECOUNT')")
    int countPendingByTaskId(@Param("taskId") Long taskId);

    @Select("SELECT COUNT(*) FROM wms_stocktake_item WHERE task_id = #{taskId} AND diff_qty != 0")
    int countDiffByTaskId(@Param("taskId") Long taskId);
}
