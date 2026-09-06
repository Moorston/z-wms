package com.xwms.core.label.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.label.entity.LabelTask;

@Mapper
public interface LabelTaskMapper extends BaseMapper<LabelTask> {

    @Select("SELECT * FROM wms_label_task WHERE task_id = #{taskId}")
    LabelTask selectByTaskId(@Param("taskId") String taskId);

    @Select("SELECT * FROM wms_label_task WHERE status = #{status} ORDER BY created_time")
    List<LabelTask> selectByStatus(@Param("status") String status);

    @Select(
            "SELECT * FROM wms_label_task WHERE template_code = #{templateCode} ORDER BY created_time DESC LIMIT #{limit}")
    List<LabelTask> selectRecentByTemplate(
            @Param("templateCode") String templateCode, @Param("limit") int limit);

    @Update(
            "UPDATE wms_label_task SET status = #{status}, start_time = NOW(), updated_time = NOW() WHERE task_id = #{taskId}")
    int startTask(@Param("taskId") String taskId, @Param("status") String status);

    @Update(
            "UPDATE wms_label_task SET status = #{status}, printed_count = #{printedCount}, failed_count = #{failedCount}, end_time = NOW(), duration_ms = #{durationMs}, error_message = #{errorMessage}, updated_time = NOW() WHERE task_id = #{taskId}")
    int completeTask(
            @Param("taskId") String taskId,
            @Param("status") String status,
            @Param("printedCount") Integer printedCount,
            @Param("failedCount") Integer failedCount,
            @Param("durationMs") Long durationMs,
            @Param("errorMessage") String errorMessage);
}
