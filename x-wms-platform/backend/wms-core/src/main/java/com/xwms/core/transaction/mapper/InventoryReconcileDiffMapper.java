package com.xwms.core.transaction.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.transaction.entity.InventoryReconcileDiff;

@Mapper
public interface InventoryReconcileDiffMapper extends BaseMapper<InventoryReconcileDiff> {

    @Select("SELECT * FROM wms_inventory_reconcile_diff WHERE diff_id = #{diffId}")
    InventoryReconcileDiff selectByDiffId(@Param("diffId") String diffId);

    @Select(
            "SELECT * FROM wms_inventory_reconcile_diff WHERE reconcile_no = #{reconcileNo} ORDER BY diff_qty DESC")
    List<InventoryReconcileDiff> selectByReconcileNo(@Param("reconcileNo") String reconcileNo);

    @Update(
            "UPDATE wms_inventory_reconcile_diff SET status = #{status}, resolve_action = #{resolveAction}, resolved_by = #{resolvedBy}, resolved_time = NOW() WHERE diff_id = #{diffId}")
    int updateStatus(
            @Param("diffId") String diffId,
            @Param("status") String status,
            @Param("resolveAction") String resolveAction,
            @Param("resolvedBy") String resolvedBy);
}
