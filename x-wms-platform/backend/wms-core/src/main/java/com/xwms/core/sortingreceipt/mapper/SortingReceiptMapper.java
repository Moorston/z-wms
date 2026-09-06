package com.xwms.core.sortingreceipt.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.sortingreceipt.entity.SortingReceipt;

/** 整理收货 Mapper */
@Mapper
public interface SortingReceiptMapper extends BaseMapper<SortingReceipt> {

    /** 根据整理收货单号查询 */
    SortingReceipt selectByReceiptNo(@Param("receiptNo") String receiptNo);

    /** 根据ASN号查询 */
    List<SortingReceipt> selectByAsnNo(@Param("asnNo") String asnNo);
}
