package com.xwms.core.pallet.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.pallet.entity.Pallet;

/** 托盘Mapper */
@Mapper
public interface PalletMapper extends BaseMapper<Pallet> {

    Pallet selectByLpnNo(@Param("lpnNo") String lpnNo);

    List<Pallet> selectByStatus(@Param("status") String status);

    List<Pallet> selectByLocation(@Param("locationCode") String locationCode);

    List<Pallet> selectByAsnNo(@Param("asnNo") String asnNo);

    List<Pallet> selectByInboundNo(@Param("inboundNo") String inboundNo);

    List<Pallet> selectEmptyPallets(@Param("warehouseCode") String warehouseCode);

    int updateStatus(@Param("lpnNo") String lpnNo, @Param("status") String status);

    int updateLocation(
            @Param("lpnNo") String lpnNo,
            @Param("locationCode") String locationCode,
            @Param("areaCode") String areaCode);

    int updateQty(
            @Param("lpnNo") String lpnNo,
            @Param("totalQty") java.math.BigDecimal totalQty,
            @Param("skuCount") Integer skuCount);
}
