package com.xwms.core.performance.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.performance.entity.PerformanceDaily;

@Mapper
public interface PerformanceDailyMapper extends BaseMapper<PerformanceDaily> {

    @Select(
            "SELECT * FROM wms_performance_daily WHERE employee_id = #{employeeId} AND work_date = #{date}")
    PerformanceDaily selectByEmployeeAndDate(
            @Param("employeeId") Long employeeId, @Param("date") LocalDate date);

    @Select(
            "SELECT * FROM wms_performance_daily WHERE work_date = #{date} AND warehouse_code = #{warehouse} ORDER BY performance_score DESC")
    List<PerformanceDaily> selectByDateAndWarehouse(
            @Param("date") LocalDate date, @Param("warehouse") String warehouse);
}
