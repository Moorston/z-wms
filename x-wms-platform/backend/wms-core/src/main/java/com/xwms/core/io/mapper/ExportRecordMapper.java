package com.xwms.core.io.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.io.entity.ExportRecord;

@Mapper
public interface ExportRecordMapper extends BaseMapper<ExportRecord> {

    @Select("SELECT * FROM wms_export_record WHERE task_id = #{taskId} ORDER BY row_no")
    List<ExportRecord> selectByTaskId(@Param("taskId") String taskId);

    @Select(
            "SELECT * FROM wms_export_record WHERE task_id = #{taskId} AND export_status = #{status} ORDER BY row_no")
    List<ExportRecord> selectByTaskIdAndStatus(
            @Param("taskId") String taskId, @Param("status") String status);

    @Select(
            "SELECT COUNT(*) FROM wms_export_record WHERE task_id = #{taskId} AND export_status = #{status}")
    int countByTaskIdAndStatus(@Param("taskId") String taskId, @Param("status") String status);

    @Update(
            "UPDATE wms_export_record SET export_status = #{status}, error_message = #{errorMessage} WHERE record_id = #{recordId}")
    int updateStatus(
            @Param("recordId") String recordId,
            @Param("status") String status,
            @Param("errorMessage") String errorMessage);
}
