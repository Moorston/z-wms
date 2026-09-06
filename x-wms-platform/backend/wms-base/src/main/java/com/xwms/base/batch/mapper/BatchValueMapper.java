package com.xwms.base.batch.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.batch.entity.BatchValue;

@Mapper
public interface BatchValueMapper extends BaseMapper<BatchValue> {

    @Select(
            "SELECT * FROM wms_batch_value WHERE batch_no = #{batchNo} AND sku_code = #{skuCode} ORDER BY attr_code")
    List<BatchValue> selectByBatchAndSku(
            @Param("batchNo") String batchNo, @Param("skuCode") String skuCode);

    @Select(
            "SELECT * FROM wms_batch_value WHERE batch_no = #{batchNo} ORDER BY sku_code, attr_code")
    List<BatchValue> selectByBatch(@Param("batchNo") String batchNo);
}
