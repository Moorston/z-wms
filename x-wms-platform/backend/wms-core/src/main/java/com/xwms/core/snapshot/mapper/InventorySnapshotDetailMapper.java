package com.xwms.core.snapshot.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.snapshot.entity.InventorySnapshotDetail;

@Mapper
public interface InventorySnapshotDetailMapper extends BaseMapper<InventorySnapshotDetail> {

    @Select(
            "SELECT * FROM wms_inventory_snapshot_detail WHERE snapshot_no = #{snapshotNo} ORDER BY sku_code, location_code")
    List<InventorySnapshotDetail> selectBySnapshotNo(@Param("snapshotNo") String snapshotNo);

    @Select(
            "SELECT * FROM wms_inventory_snapshot_detail WHERE snapshot_no = #{snapshotNo} AND sku_code = #{skuCode}")
    List<InventorySnapshotDetail> selectBySnapshotNoAndSku(
            @Param("snapshotNo") String snapshotNo, @Param("skuCode") String skuCode);

    @Select("SELECT COUNT(*) FROM wms_inventory_snapshot_detail WHERE snapshot_no = #{snapshotNo}")
    int countBySnapshotNo(@Param("snapshotNo") String snapshotNo);
}
