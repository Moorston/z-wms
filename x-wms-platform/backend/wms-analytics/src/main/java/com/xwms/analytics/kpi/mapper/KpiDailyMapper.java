package com.xwms.analytics.kpi.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.analytics.kpi.entity.KpiDaily;

@Mapper
public interface KpiDailyMapper extends BaseMapper<KpiDaily> {

    @Select(
            "SELECT * FROM wms_kpi_daily WHERE kpi_code = #{kpiCode} AND stat_date BETWEEN #{start} AND #{end} ORDER BY stat_date")
    List<KpiDaily> selectByKpiAndDateRange(
            @Param("kpiCode") String kpiCode,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Select(
            "SELECT * FROM wms_kpi_daily WHERE stat_date = #{date} AND warehouse_code = #{whCode} ORDER BY kpi_category, kpi_code")
    List<KpiDaily> selectByDateAndWarehouse(
            @Param("date") LocalDate date, @Param("whCode") String whCode);
}
