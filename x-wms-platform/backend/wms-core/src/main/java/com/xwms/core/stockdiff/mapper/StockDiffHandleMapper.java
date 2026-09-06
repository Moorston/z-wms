package com.xwms.core.stockdiff.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.stockdiff.entity.StockDiffHandle;

@Mapper
public interface StockDiffHandleMapper extends BaseMapper<StockDiffHandle> {

    @Select("SELECT * FROM wms_stock_diff_handle WHERE diff_id = #{diffId} ORDER BY handle_time")
    List<StockDiffHandle> selectByDiffId(@Param("diffId") String diffId);

    @Select("SELECT * FROM wms_stock_diff_handle WHERE handle_id = #{handleId}")
    StockDiffHandle selectByHandleId(@Param("handleId") String handleId);
}
