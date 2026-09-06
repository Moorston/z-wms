package com.xwms.core.dashboard.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.dashboard.entity.RealtimeMonitor;

@Mapper
public interface RealtimeMonitorMapper extends BaseMapper<RealtimeMonitor> {
    @Select("SELECT * FROM wms_realtime_monitor WHERE monitor_id = #{monitorId}")
    RealtimeMonitor selectByMonitorId(@Param("monitorId") String monitorId);

    @Select(
            "SELECT * FROM wms_realtime_monitor WHERE warehouse_code = #{warehouseCode} AND monitor_type = #{monitorType} AND is_active = 'Y'")
    List<RealtimeMonitor> selectActiveByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode, @Param("monitorType") String monitorType);

    @Select(
            "SELECT * FROM wms_realtime_monitor WHERE warehouse_code = #{warehouseCode} AND status = #{status} AND is_active = 'Y'")
    List<RealtimeMonitor> selectByWarehouseAndStatus(
            @Param("warehouseCode") String warehouseCode, @Param("status") String status);

    @Select(
            "SELECT * FROM wms_realtime_monitor WHERE warehouse_code = #{warehouseCode} AND is_active = 'Y' ORDER BY monitor_type")
    List<RealtimeMonitor> selectActiveByWarehouse(@Param("warehouseCode") String warehouseCode);
}
