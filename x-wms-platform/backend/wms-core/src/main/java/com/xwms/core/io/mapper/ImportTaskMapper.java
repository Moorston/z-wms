package com.xwms.core.io.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.io.entity.ImportTask;

@Mapper
public interface ImportTaskMapper extends BaseMapper<ImportTask> {

    @Select("SELECT * FROM wms_import_task WHERE task_id = #{taskId}")
    ImportTask selectByTaskId(@Param("taskId") String taskId);

    @Select("SELECT * FROM wms_import_task WHERE status = #{status} ORDER BY created_time")
    List<ImportTask> selectByStatus(@Param("status") String status);

    @Select(
            "SELECT * FROM wms_import_task WHERE operator = #{operator} ORDER BY created_time DESC LIMIT #{limit}")
    List<ImportTask> selectRecentByOperator(
            @Param("operator") String operator, @Param("limit") int limit);

    @Update(
            "UPDATE wms_import_task SET status = #{status}, start_time = NOW(), updated_time = NOW() WHERE task_id = #{taskId}")
    int startTask(@Param("taskId") String taskId, @Param("status") String status);

    @Update(
            "UPDATE wms_import_task SET status = #{status}, total_count = #{totalCount}, success_count = #{successCount}, fail_count = #{failCount}, skip_count = #{skipCount}, end_time = NOW(), duration_ms = #{durationMs}, error_message = #{errorMessage}, updated_time = NOW() WHERE task_id = #{taskId}")
    int completeTask(
            @Param("taskId") String taskId,
            @Param("status") String status,
            @Param("totalCount") Integer totalCount,
            @Param("successCount") Integer successCount,
            @Param("failCount") Integer failCount,
            @Param("skipCount") Integer skipCount,
            @Param("durationMs") Long durationMs,
            @Param("errorMessage") String errorMessage);
}
