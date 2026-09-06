package com.xwms.core.snapshot.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.snapshot.entity.InventorySnapshot;

@Mapper
public interface InventorySnapshotMapper extends BaseMapper<InventorySnapshot> {

    @Select("SELECT * FROM wms_inventory_snapshot WHERE snapshot_no = #{snapshotNo}")
    InventorySnapshot selectBySnapshotNo(@Param("snapshotNo") String snapshotNo);

    @Select(
            "SELECT * FROM wms_inventory_snapshot WHERE warehouse_code = #{warehouseCode} AND snapshot_date = #{snapshotDate} AND snapshot_type = #{snapshotType} LIMIT 1")
    InventorySnapshot selectByDateAndType(
            @Param("warehouseCode") String warehouseCode,
            @Param("snapshotDate") LocalDate snapshotDate,
            @Param("snapshotType") String snapshotType);

    @Select(
            "SELECT * FROM wms_inventory_snapshot WHERE warehouse_code = #{warehouseCode} AND snapshot_date BETWEEN #{startDate} AND #{endDate} ORDER BY snapshot_date DESC")
    List<InventorySnapshot> selectByDateRange(
            @Param("warehouseCode") String warehouseCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
