package com.xwms.core.transaction.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.transaction.entity.InventoryReconcile;

@Mapper
public interface InventoryReconcileMapper extends BaseMapper<InventoryReconcile> {

    @Select("SELECT * FROM wms_inventory_reconcile WHERE reconcile_no = #{reconcileNo}")
    InventoryReconcile selectByReconcileNo(@Param("reconcileNo") String reconcileNo);

    @Update(
            "UPDATE wms_inventory_reconcile SET status = #{status}, operator = #{operator}, operate_time = NOW(), remark = #{remark}, updated_time = NOW() WHERE reconcile_no = #{reconcileNo}")
    int updateStatus(
            @Param("reconcileNo") String reconcileNo,
            @Param("status") String status,
            @Param("operator") String operator,
            @Param("remark") String remark);
}
