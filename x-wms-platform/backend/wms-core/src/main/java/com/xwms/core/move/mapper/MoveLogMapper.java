package com.xwms.core.move.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.move.entity.MoveLog;

@Mapper
public interface MoveLogMapper extends BaseMapper<MoveLog> {

    @Select("SELECT * FROM wms_move_log WHERE move_no = #{moveNo} ORDER BY action_time")
    List<MoveLog> selectByMoveNo(@Param("moveNo") String moveNo);

    @Select(
            "SELECT * FROM wms_move_log WHERE sku_code = #{skuCode} AND batch_no = #{batchNo} ORDER BY action_time DESC")
    List<MoveLog> selectBySku(@Param("skuCode") String skuCode, @Param("batchNo") String batchNo);
}
