package com.xwms.base.batch.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.batch.entity.BatchTraceLog;

@Mapper
public interface BatchTraceLogMapper extends BaseMapper<BatchTraceLog> {

    @Select("SELECT * FROM wms_batch_trace_log WHERE batch_no = #{batchNo} ORDER BY operation_time")
    List<BatchTraceLog> selectByBatch(@Param("batchNo") String batchNo);

    @Select(
            "SELECT * FROM wms_batch_trace_log WHERE batch_no = #{batchNo} AND sku_code = #{skuCode} ORDER BY operation_time")
    List<BatchTraceLog> selectByBatchAndSku(
            @Param("batchNo") String batchNo, @Param("skuCode") String skuCode);
}
