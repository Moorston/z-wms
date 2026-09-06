package com.xwms.core.reserve.mapper;

import java.math.BigDecimal;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.reserve.entity.InventoryReserve;

@Mapper
public interface InventoryReserveMapper extends BaseMapper<InventoryReserve> {

    @Select("SELECT * FROM wms_inventory_reserve WHERE reserve_no = #{reserveNo}")
    InventoryReserve selectByReserveNo(@Param("reserveNo") String reserveNo);

    @Select("SELECT * FROM wms_inventory_reserve WHERE ref_type = #{refType} AND ref_no = #{refNo}")
    InventoryReserve selectByRef(@Param("refType") String refType, @Param("refNo") String refNo);

    @Update(
            "UPDATE wms_inventory_reserve SET status = #{status}, updated_time = NOW() WHERE reserve_no = #{reserveNo}")
    int updateStatus(@Param("reserveNo") String reserveNo, @Param("status") String status);

    /** 增加已释放数量 */
    @Update(
            "UPDATE wms_inventory_reserve SET released_qty = released_qty + #{qty}, status = CASE WHEN released_qty + #{qty} >= reserved_qty THEN 'RELEASED' ELSE 'PARTIAL' END, updated_time = NOW() WHERE reserve_no = #{reserveNo}")
    int addReleasedQty(@Param("reserveNo") String reserveNo, @Param("qty") BigDecimal qty);

    /** 增加已确认数量 */
    @Update(
            "UPDATE wms_inventory_reserve SET confirmed_qty = confirmed_qty + #{qty}, status = CASE WHEN confirmed_qty + #{qty} >= reserved_qty THEN 'CONFIRMED' ELSE status END, confirmed_time = NOW(), updated_time = NOW() WHERE reserve_no = #{reserveNo}")
    int addConfirmedQty(@Param("reserveNo") String reserveNo, @Param("qty") BigDecimal qty);
}
