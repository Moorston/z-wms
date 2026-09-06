package com.xwms.core.dataquality.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.dataquality.entity.DqReport;

@Mapper
public interface DqReportMapper extends BaseMapper<DqReport> {
    @Select("SELECT * FROM wms_dq_report WHERE report_id = #{reportId}")
    DqReport selectByReportId(@Param("reportId") String reportId);

    @Select(
            "SELECT * FROM wms_dq_report WHERE warehouse_code = #{warehouseCode} AND period_start >= #{startTime} AND period_end <= #{endTime} ORDER BY period_start")
    List<DqReport> selectByWarehouseAndPeriod(
            @Param("warehouseCode") String warehouseCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Select(
            "SELECT * FROM wms_dq_report WHERE report_type = #{reportType} AND status = #{status} ORDER BY generated_time DESC")
    List<DqReport> selectByTypeAndStatus(
            @Param("reportType") String reportType, @Param("status") String status);

    @Select(
            "SELECT * FROM wms_dq_report WHERE warehouse_code = #{warehouseCode} ORDER BY generated_time DESC LIMIT #{limit}")
    List<DqReport> selectRecentByWarehouse(
            @Param("warehouseCode") String warehouseCode, @Param("limit") int limit);
}
