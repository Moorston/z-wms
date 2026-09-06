package com.xwms.core.qc.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.qc.entity.QcTask;

/** 质检任务Mapper */
@Mapper
public interface QcTaskMapper extends BaseMapper<QcTask> {

    QcTask selectByTaskNo(@Param("taskNo") String taskNo);

    List<QcTask> selectByAsnNo(@Param("asnNo") String asnNo);

    List<QcTask> selectByInboundNo(@Param("inboundNo") String inboundNo);

    List<QcTask> selectByReceiptTaskNo(@Param("receiptTaskNo") String receiptTaskNo);

    List<QcTask> selectByPutawayTaskNo(@Param("putawayTaskNo") String putawayTaskNo);

    List<QcTask> selectPendingTasks(@Param("warehouseCode") String warehouseCode);

    int updateStatus(@Param("taskNo") String taskNo, @Param("status") String status);

    int updateResult(
            @Param("taskNo") String taskNo,
            @Param("qcResult") String qcResult,
            @Param("status") String status);
}
