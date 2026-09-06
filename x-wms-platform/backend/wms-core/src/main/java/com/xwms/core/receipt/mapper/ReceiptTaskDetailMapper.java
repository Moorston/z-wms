package com.xwms.core.receipt.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.receipt.entity.ReceiptTaskDetail;

/** 收货任务明细Mapper */
@Mapper
public interface ReceiptTaskDetailMapper extends BaseMapper<ReceiptTaskDetail> {

    List<ReceiptTaskDetail> selectByTaskNo(@Param("taskNo") String taskNo);

    ReceiptTaskDetail selectByDetailNo(@Param("detailNo") String detailNo);

    ReceiptTaskDetail selectByTaskNoAndSku(
            @Param("taskNo") String taskNo, @Param("skuCode") String skuCode);

    int updateReceivedQty(
            @Param("detailNo") String detailNo,
            @Param("receivedQty") java.math.BigDecimal receivedQty);

    int updateStatus(@Param("detailNo") String detailNo, @Param("status") String status);
}
