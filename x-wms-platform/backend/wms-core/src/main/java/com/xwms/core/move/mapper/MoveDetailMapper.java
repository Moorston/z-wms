package com.xwms.core.move.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.move.entity.MoveDetail;

@Mapper
public interface MoveDetailMapper extends BaseMapper<MoveDetail> {

    @Select("SELECT * FROM wms_move_detail WHERE move_no = #{moveNo} ORDER BY line_no")
    List<MoveDetail> selectByMoveNo(@Param("moveNo") String moveNo);

    @Update(
            "UPDATE wms_move_detail SET moved_qty = moved_qty + #{qty}, status = CASE WHEN moved_qty + #{qty} >= plan_qty THEN 'COMPLETED' ELSE 'MOVING' END WHERE id = #{id}")
    int addMovedQty(@Param("id") Long id, @Param("qty") BigDecimal qty);
}
