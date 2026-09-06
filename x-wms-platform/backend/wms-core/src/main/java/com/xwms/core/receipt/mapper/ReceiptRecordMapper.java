package com.xwms.core.receipt.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.receipt.entity.ReceiptRecord;

/** 收货记录Mapper */
@Mapper
public interface ReceiptRecordMapper extends BaseMapper<ReceiptRecord> {

    ReceiptRecord selectByRecordNo(@Param("recordNo") String recordNo);

    List<ReceiptRecord> selectByTaskNo(@Param("taskNo") String taskNo);

    List<ReceiptRecord> selectByAsnNo(@Param("asnNo") String asnNo);

    List<ReceiptRecord> selectByInboundNo(@Param("inboundNo") String inboundNo);

    List<ReceiptRecord> selectBySkuCode(@Param("skuCode") String skuCode);
}
