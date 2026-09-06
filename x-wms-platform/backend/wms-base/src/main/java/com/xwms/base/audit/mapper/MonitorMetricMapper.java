package com.xwms.base.audit.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.audit.entity.MonitorMetric;

@Mapper
public interface MonitorMetricMapper extends BaseMapper<MonitorMetric> {

    @Select(
            "SELECT * FROM sys_monitor_metric WHERE metric_name = #{name} AND collected_time >= #{start} ORDER BY collected_time")
    List<MonitorMetric> selectByNameAndTimeRange(
            @Param("name") String name, @Param("start") LocalDateTime start);

    @Select(
            "SELECT * FROM sys_monitor_metric WHERE status != 'NORMAL' ORDER BY collected_time DESC")
    List<MonitorMetric> selectAbnormal();
}
