package com.xwms.core.performance.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.performance.entity.PerformanceAssess;

@Mapper
public interface PerformanceAssessMapper extends BaseMapper<PerformanceAssess> {

    @Select(
            "SELECT * FROM wms_performance_assess WHERE employee_id = #{employeeId} ORDER BY assess_period DESC")
    List<PerformanceAssess> selectByEmployeeId(@Param("employeeId") Long employeeId);

    @Select(
            "SELECT * FROM wms_performance_assess WHERE assess_period = #{period} AND warehouse_code = #{warehouse} ORDER BY performance_score DESC")
    List<PerformanceAssess> selectByPeriodAndWarehouse(
            @Param("period") String period, @Param("warehouse") String warehouse);
}
