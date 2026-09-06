package com.xwms.core.dashboard.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.dashboard.entity.DashboardSnapshot;

@Mapper
public interface DashboardSnapshotMapper extends BaseMapper<DashboardSnapshot> {

    @Select("SELECT * FROM wms_dashboard_snapshot WHERE snapshot_id = #{snapshotId}")
    DashboardSnapshot selectBySnapshotId(@Param("snapshotId") String snapshotId);

    @Select(
            "SELECT * FROM wms_dashboard_snapshot WHERE dashboard_code = #{dashboardCode} AND snapshot_time >= #{startTime} ORDER BY snapshot_time")
    List<DashboardSnapshot> selectByDashboardAndTime(
            @Param("dashboardCode") String dashboardCode,
            @Param("startTime") LocalDateTime startTime);

    @Select(
            "SELECT * FROM wms_dashboard_snapshot WHERE component_code = #{componentCode} AND snapshot_time >= #{startTime} ORDER BY snapshot_time")
    List<DashboardSnapshot> selectByComponentAndTime(
            @Param("componentCode") String componentCode,
            @Param("startTime") LocalDateTime startTime);

    @Select(
            "SELECT * FROM wms_dashboard_snapshot WHERE warehouse_code = #{warehouseCode} AND data_type = #{dataType} AND period_type = #{periodType} ORDER BY snapshot_time DESC LIMIT #{limit}")
    List<DashboardSnapshot> selectRecentByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode,
            @Param("dataType") String dataType,
            @Param("periodType") String periodType,
            @Param("limit") int limit);

    @Select(
            "SELECT * FROM wms_dashboard_snapshot WHERE warehouse_code = #{warehouseCode} AND data_type = #{dataType} AND snapshot_time = (SELECT MAX(snapshot_time) FROM wms_dashboard_snapshot WHERE warehouse_code = #{warehouseCode} AND data_type = #{dataType})")
    DashboardSnapshot selectLatestByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode, @Param("dataType") String dataType);
}
