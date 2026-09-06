package com.xwms.core.bom.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.bom.entity.ComponentReceipt;

/** 组件扫描收货 Mapper */
@Mapper
public interface ComponentReceiptMapper extends BaseMapper<ComponentReceipt> {

    /** 根据收货单号查询 */
    ComponentReceipt selectByReceiptNo(@Param("receiptNo") String receiptNo);

    /** 根据ASN号查询 */
    List<ComponentReceipt> selectByAsnNo(@Param("asnNo") String asnNo);
}
