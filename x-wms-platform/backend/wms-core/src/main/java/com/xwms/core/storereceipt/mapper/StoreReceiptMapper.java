package com.xwms.core.storereceipt.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.storereceipt.entity.StoreReceipt;

@Mapper
public interface StoreReceiptMapper extends BaseMapper<StoreReceipt> {
    StoreReceipt selectByReceiptNo(@Param("receiptNo") String receiptNo);

    List<StoreReceipt> selectByStore(@Param("storeCode") String storeCode);

    List<StoreReceipt> selectByOutboundNo(@Param("outboundNo") String outboundNo);
}
