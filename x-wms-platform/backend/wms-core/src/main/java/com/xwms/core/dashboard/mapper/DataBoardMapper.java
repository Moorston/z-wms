package com.xwms.core.dashboard.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.dashboard.entity.DataBoard;

@Mapper
public interface DataBoardMapper extends BaseMapper<DataBoard> {
    @Select("SELECT * FROM wms_data_board WHERE board_id = #{boardId}")
    DataBoard selectByBoardId(@Param("boardId") String boardId);

    @Select("SELECT * FROM wms_data_board WHERE board_code = #{boardCode}")
    DataBoard selectByBoardCode(@Param("boardCode") String boardCode);

    @Select(
            "SELECT * FROM wms_data_board WHERE board_type = #{boardType} AND is_active = 'Y' ORDER BY sort_order")
    List<DataBoard> selectActiveByType(@Param("boardType") String boardType);

    @Select(
            "SELECT * FROM wms_data_board WHERE warehouse_code = #{warehouseCode} AND is_active = 'Y' ORDER BY sort_order")
    List<DataBoard> selectActiveByWarehouse(@Param("warehouseCode") String warehouseCode);
}
