package com.xwms.core.receipt.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.receipt.entity.BlindReceipt;

/** 盲收记录Mapper */
@Mapper
public interface BlindReceiptMapper extends BaseMapper<BlindReceipt> {

    BlindReceipt selectByBlindNo(@Param("blindNo") String blindNo);

    List<BlindReceipt> selectByStatus(@Param("status") String status);

    List<BlindReceipt> selectBySkuCode(@Param("skuCode") String skuCode);

    List<BlindReceipt> selectUnmatched(@Param("warehouseCode") String warehouseCode);

    int updateMatchInfo(
            @Param("blindNo") String blindNo,
            @Param("matchedPoNo") String matchedPoNo,
            @Param("matchedAsnNo") String matchedAsnNo,
            @Param("matchedInboundNo") String matchedInboundNo,
            @Param("status") String status);

    int updateStatus(@Param("blindNo") String blindNo, @Param("status") String status);
}
