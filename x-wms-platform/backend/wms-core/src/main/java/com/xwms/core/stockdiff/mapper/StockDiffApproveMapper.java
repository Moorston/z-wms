package com.xwms.core.stockdiff.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.stockdiff.entity.StockDiffApprove;

@Mapper
public interface StockDiffApproveMapper extends BaseMapper<StockDiffApprove> {

    @Select("SELECT * FROM wms_stock_diff_approve WHERE diff_id = #{diffId} ORDER BY approve_time")
    List<StockDiffApprove> selectByDiffId(@Param("diffId") String diffId);

    @Select("SELECT * FROM wms_stock_diff_approve WHERE approve_id = #{approveId}")
    StockDiffApprove selectByApproveId(@Param("approveId") String approveId);
}
