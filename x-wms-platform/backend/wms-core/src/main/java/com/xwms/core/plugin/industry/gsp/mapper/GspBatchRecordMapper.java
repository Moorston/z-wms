package com.xwms.core.plugin.industry.gsp.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.plugin.industry.gsp.entity.GspBatchRecord;

/** GSP批次记录Mapper */
@Mapper
public interface GspBatchRecordMapper extends BaseMapper<GspBatchRecord> {

    /** 根据记录编号查询 */
    GspBatchRecord selectByRecordNo(@Param("recordNo") String recordNo);

    /** 根据批次号查询 */
    List<GspBatchRecord> selectByBatchNo(@Param("batchNo") String batchNo);

    /** 根据商品编码查询 */
    List<GspBatchRecord> selectBySkuCode(@Param("skuCode") String skuCode);

    /** 查询近效期批次 */
    List<GspBatchRecord> selectNearExpiryBatches(@Param("days") Integer days);

    /** 查询已过期批次 */
    List<GspBatchRecord> selectExpiredBatches();

    /** 根据追溯码查询 */
    GspBatchRecord selectByTraceCode(@Param("traceCode") String traceCode);
}
