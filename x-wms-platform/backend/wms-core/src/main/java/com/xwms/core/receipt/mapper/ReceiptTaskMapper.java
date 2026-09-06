package com.xwms.core.receipt.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.receipt.entity.ReceiptTask;

/** 收货任务Mapper */
@Mapper
public interface ReceiptTaskMapper extends BaseMapper<ReceiptTask> {

    ReceiptTask selectByTaskNo(@Param("taskNo") String taskNo);

    ReceiptTask selectByAsnNo(@Param("asnNo") String asnNo);

    List<ReceiptTask> selectByInboundNo(@Param("inboundNo") String inboundNo);

    List<ReceiptTask> selectPendingTasks(@Param("warehouseCode") String warehouseCode);

    int updateReceivedQty(
            @Param("taskNo") String taskNo, @Param("receivedQty") java.math.BigDecimal receivedQty);

    int updateStatus(@Param("taskNo") String taskNo, @Param("status") String status);
}
