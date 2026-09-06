package com.xwms.core.sortingreceipt.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.sortingreceipt.entity.SortingReceiptDetail;

/** 整理收货明细 Mapper */
@Mapper
public interface SortingReceiptDetailMapper extends BaseMapper<SortingReceiptDetail> {

    /** 根据整理收货单号查询明细 */
    List<SortingReceiptDetail> selectByReceiptNo(@Param("receiptNo") String receiptNo);

    /** 根据整理收货单号和SKU查询 */
    SortingReceiptDetail selectByReceiptNoAndSku(
            @Param("receiptNo") String receiptNo, @Param("skuCode") String skuCode);

    /** 批量插入 */
    int batchInsert(@Param("list") List<SortingReceiptDetail> list);
}
