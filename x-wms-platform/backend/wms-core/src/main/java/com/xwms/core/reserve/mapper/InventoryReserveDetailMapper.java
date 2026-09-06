package com.xwms.core.reserve.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.reserve.entity.InventoryReserveDetail;

@Mapper
public interface InventoryReserveDetailMapper extends BaseMapper<InventoryReserveDetail> {

    @Select(
            "SELECT * FROM wms_inventory_reserve_detail WHERE reserve_no = #{reserveNo} ORDER BY line_no")
    List<InventoryReserveDetail> selectByReserveNo(@Param("reserveNo") String reserveNo);

    /** 增加已释放数量 */
    @Update(
            "UPDATE wms_inventory_reserve_detail SET released_qty = released_qty + #{qty}, status = CASE WHEN released_qty + #{qty} >= reserved_qty THEN 'RELEASED' ELSE 'PARTIAL' END WHERE id = #{id}")
    int addReleasedQty(@Param("id") Long id, @Param("qty") BigDecimal qty);

    /** 增加已确认数量 */
    @Update(
            "UPDATE wms_inventory_reserve_detail SET confirmed_qty = confirmed_qty + #{qty}, status = CASE WHEN confirmed_qty + #{qty} >= reserved_qty THEN 'CONFIRMED' ELSE status END WHERE id = #{id}")
    int addConfirmedQty(@Param("id") Long id, @Param("qty") BigDecimal qty);
}
