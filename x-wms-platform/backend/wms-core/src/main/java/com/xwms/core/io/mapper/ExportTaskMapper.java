package com.xwms.core.io.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.io.entity.ExportTask;

@Mapper
public interface ExportTaskMapper extends BaseMapper<ExportTask> {

    @Select("SELECT * FROM wms_export_task WHERE task_id = #{taskId}")
    ExportTask selectByTaskId(@Param("taskId") String taskId);

    @Select("SELECT * FROM wms_export_task WHERE status = #{status} ORDER BY created_time")
    List<ExportTask> selectByStatus(@Param("status") String status);

    @Select(
            "SELECT * FROM wms_export_task WHERE operator = #{operator} ORDER BY created_time DESC LIMIT #{limit}")
    List<ExportTask> selectRecentByOperator(
            @Param("operator") String operator, @Param("limit") int limit);

    @Update(
            "UPDATE wms_export_task SET status = #{status}, start_time = NOW(), updated_time = NOW() WHERE task_id = #{taskId}")
    int startTask(@Param("taskId") String taskId, @Param("status") String status);

    @Update(
            "UPDATE wms_export_task SET status = #{status}, total_count = #{totalCount}, exported_count = #{exportedCount}, file_name = #{fileName}, file_path = #{filePath}, file_size = #{fileSize}, end_time = NOW(), duration_ms = #{durationMs}, error_message = #{errorMessage}, updated_time = NOW() WHERE task_id = #{taskId}")
    int completeTask(
            @Param("taskId") String taskId,
            @Param("status") String status,
            @Param("totalCount") Integer totalCount,
            @Param("exportedCount") Integer exportedCount,
            @Param("fileName") String fileName,
            @Param("filePath") String filePath,
            @Param("fileSize") Long fileSize,
            @Param("durationMs") Long durationMs,
            @Param("errorMessage") String errorMessage);

    @Update(
            "UPDATE wms_export_task SET download_count = download_count + 1, updated_time = NOW() WHERE task_id = #{taskId}")
    int incrementDownloadCount(@Param("taskId") String taskId);
}
