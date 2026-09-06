package com.xwms.core.move.mapper;

import java.math.BigDecimal;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.move.entity.MoveOrder;

@Mapper
public interface MoveOrderMapper extends BaseMapper<MoveOrder> {

    @Select("SELECT * FROM wms_move_order WHERE move_no = #{moveNo}")
    MoveOrder selectByMoveNo(@Param("moveNo") String moveNo);

    @Update(
            "UPDATE wms_move_order SET status = #{status}, updated_time = NOW() WHERE move_no = #{moveNo}")
    int updateStatus(@Param("moveNo") String moveNo, @Param("status") String status);

    /** 更新已移数量 */
    @Update(
            "UPDATE wms_move_order SET moved_qty = moved_qty + #{qty}, updated_time = NOW() WHERE move_no = #{moveNo}")
    int addMovedQty(@Param("moveNo") String moveNo, @Param("qty") BigDecimal qty);
}
