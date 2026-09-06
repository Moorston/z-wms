package com.xwms.core.sortingreceipt.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.sortingreceipt.entity.SortingReceiptBox;

/** 整理收货箱 Mapper */
@Mapper
public interface SortingReceiptBoxMapper extends BaseMapper<SortingReceiptBox> {

    /** 根据箱号查询 */
    SortingReceiptBox selectByBoxNo(@Param("boxNo") String boxNo);

    /** 根据整理收货单号查询 */
    List<SortingReceiptBox> selectByReceiptNo(@Param("receiptNo") String receiptNo);

    /** 批量插入 */
    int batchInsert(@Param("list") List<SortingReceiptBox> list);
}
