package com.xwms.core.bom.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.bom.entity.ComponentReceiptDetail;

/** 组件扫描收货明细 Mapper */
@Mapper
public interface ComponentReceiptDetailMapper extends BaseMapper<ComponentReceiptDetail> {

    /** 根据收货单号查询明细 */
    List<ComponentReceiptDetail> selectByReceiptNo(@Param("receiptNo") String receiptNo);

    /** 根据收货单号和子件编码查询 */
    ComponentReceiptDetail selectByReceiptNoAndChildSku(
            @Param("receiptNo") String receiptNo, @Param("childSkuCode") String childSkuCode);

    /** 批量插入 */
    int batchInsert(@Param("list") List<ComponentReceiptDetail> list);
}
