package com.xwms.core.io.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.io.entity.ImportRecord;

@Mapper
public interface ImportRecordMapper extends BaseMapper<ImportRecord> {

    @Select("SELECT * FROM wms_import_record WHERE task_id = #{taskId} ORDER BY row_no")
    List<ImportRecord> selectByTaskId(@Param("taskId") String taskId);

    @Select(
            "SELECT * FROM wms_import_record WHERE task_id = #{taskId} AND import_status = #{status} ORDER BY row_no")
    List<ImportRecord> selectByTaskIdAndStatus(
            @Param("taskId") String taskId, @Param("status") String status);

    @Select(
            "SELECT COUNT(*) FROM wms_import_record WHERE task_id = #{taskId} AND import_status = #{status}")
    int countByTaskIdAndStatus(@Param("taskId") String taskId, @Param("status") String status);

    @Update(
            "UPDATE wms_import_record SET import_status = #{status}, error_code = #{errorCode}, error_message = #{errorMessage}, retry_count = retry_count + 1, process_time = NOW() WHERE record_id = #{recordId}")
    int updateStatus(
            @Param("recordId") String recordId,
            @Param("status") String status,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage);
}
