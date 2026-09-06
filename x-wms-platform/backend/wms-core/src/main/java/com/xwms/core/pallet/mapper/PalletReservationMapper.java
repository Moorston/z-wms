package com.xwms.core.pallet.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.pallet.entity.PalletReservation;

/** 码盘预约Mapper */
@Mapper
public interface PalletReservationMapper extends BaseMapper<PalletReservation> {

    PalletReservation selectByReservationNo(@Param("reservationNo") String reservationNo);

    PalletReservation selectByAsnNo(@Param("asnNo") String asnNo);

    List<PalletReservation> selectByStatus(@Param("status") String status);

    List<PalletReservation> selectByWarehouse(@Param("warehouseCode") String warehouseCode);

    int updateStatus(@Param("reservationNo") String reservationNo, @Param("status") String status);

    int updateActualInfo(
            @Param("reservationNo") String reservationNo,
            @Param("actualPalletCount") Integer actualPalletCount,
            @Param("actualTotalQty") java.math.BigDecimal actualTotalQty);
}
