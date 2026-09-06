package com.xwms.core.move.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.move.entity.MoveTask;

@Mapper
public interface MoveTaskMapper extends BaseMapper<MoveTask> {

    @Select("SELECT * FROM wms_move_task WHERE task_no = #{taskNo}")
    MoveTask selectByTaskNo(@Param("taskNo") String taskNo);

    @Select("SELECT * FROM wms_move_task WHERE move_no = #{moveNo} ORDER BY task_no")
    List<MoveTask> selectByMoveNo(@Param("moveNo") String moveNo);

    @Select(
            "SELECT * FROM wms_move_task WHERE assignee = #{assignee} AND status IN ('PENDING','ASSIGNED','EXECUTING') ORDER BY priority DESC, created_time")
    List<MoveTask> selectByAssignee(@Param("assignee") String assignee);

    @Update(
            "UPDATE wms_move_task SET status = #{status}, updated_time = NOW() WHERE task_no = #{taskNo}")
    int updateStatus(@Param("taskNo") String taskNo, @Param("status") String status);

    @Update(
            "UPDATE wms_move_task SET moved_qty = moved_qty + #{qty}, status = CASE WHEN moved_qty + #{qty} >= plan_qty THEN 'COMPLETED' ELSE status END WHERE task_no = #{taskNo}")
    int addMovedQty(@Param("taskNo") String taskNo, @Param("qty") BigDecimal qty);
}
