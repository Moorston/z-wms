package com.xwms.core.plugin.industry.gsp.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.plugin.industry.gsp.entity.GspQualityCheck;

/** GSP质检记录Mapper */
@Mapper
public interface GspQualityCheckMapper extends BaseMapper<GspQualityCheck> {

    /** 根据质检单号查询 */
    GspQualityCheck selectByCheckNo(@Param("checkNo") String checkNo);

    /** 根据关联业务单号查询 */
    List<GspQualityCheck> selectByRefNo(@Param("refNo") String refNo);

    /** 根据批次号查询 */
    List<GspQualityCheck> selectByBatchNo(@Param("batchNo") String batchNo);

    /** 查询待质检记录 */
    List<GspQualityCheck> selectPendingChecks();

    /** 根据质检类型和状态查询 */
    List<GspQualityCheck> selectByTypeAndStatus(
            @Param("checkType") String checkType, @Param("status") String status);
}
