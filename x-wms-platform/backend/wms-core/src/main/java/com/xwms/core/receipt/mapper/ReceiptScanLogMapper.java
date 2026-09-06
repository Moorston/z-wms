package com.xwms.core.receipt.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.receipt.entity.ReceiptScanLog;

/** 扫描收货日志Mapper */
@Mapper
public interface ReceiptScanLogMapper extends BaseMapper<ReceiptScanLog> {

    List<ReceiptScanLog> selectByTaskNo(@Param("taskNo") String taskNo);

    List<ReceiptScanLog> selectByRecordNo(@Param("recordNo") String recordNo);

    List<ReceiptScanLog> selectByScanContent(@Param("scanContent") String scanContent);

    int countDuplicateSerial(@Param("serialNo") String serialNo, @Param("taskNo") String taskNo);
}
