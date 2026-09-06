package com.xwms.core.simulation.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.simulation.entity.StressTest;

@Mapper
public interface StressTestMapper extends BaseMapper<StressTest> {
    @Select("SELECT * FROM wms_stress_test WHERE test_id = #{testId}")
    StressTest selectByTestId(@Param("testId") String testId);

    @Select(
            "SELECT * FROM wms_stress_test WHERE warehouse_code = #{warehouseCode} AND test_type = #{testType} ORDER BY created_time DESC")
    List<StressTest> selectByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode, @Param("testType") String testType);

    @Select("SELECT * FROM wms_stress_test WHERE status = #{status} ORDER BY created_time DESC")
    List<StressTest> selectByStatus(@Param("status") String status);
}
