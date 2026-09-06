package com.xwms.core.pallet.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.pallet.entity.PalletDetail;

/** 托盘明细Mapper */
@Mapper
public interface PalletDetailMapper extends BaseMapper<PalletDetail> {

    List<PalletDetail> selectByLpnNo(@Param("lpnNo") String lpnNo);

    PalletDetail selectByDetailNo(@Param("detailNo") String detailNo);

    List<PalletDetail> selectByLpnAndSku(
            @Param("lpnNo") String lpnNo, @Param("skuCode") String skuCode);

    List<PalletDetail> selectByAsnNo(@Param("asnNo") String asnNo);

    int updateQty(
            @Param("detailNo") String detailNo,
            @Param("qty") java.math.BigDecimal qty,
            @Param("remainingQty") java.math.BigDecimal remainingQty);

    int updateStatus(@Param("detailNo") String detailNo, @Param("status") String status);
}
