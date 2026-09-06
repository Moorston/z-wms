package com.xwms.analytics.kpi.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.analytics.kpi.entity.KpiReport;

@Mapper
public interface KpiReportMapper extends BaseMapper<KpiReport> {

    @Select("SELECT * FROM wms_kpi_report WHERE enabled = 1 ORDER BY report_type, report_code")
    List<KpiReport> selectAllEnabled();
}
