package com.xwms.core.performance.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.performance.entity.WorkRecord;

@Mapper
public interface WorkRecordMapper extends BaseMapper<WorkRecord> {

    @Select(
            "SELECT * FROM wms_work_record WHERE employee_id = #{employeeId} AND start_time >= #{start} AND start_time < #{end} ORDER BY start_time")
    List<WorkRecord> selectByEmployeeAndDateRange(
            @Param("employeeId") Long employeeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
