package com.xwms.core.replenish.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.replenish.entity.ReplenishTask;

@Mapper
public interface ReplenishTaskMapper extends BaseMapper<ReplenishTask> {

    /** 查询待处理补货任务(按优先级排序) */
    @Select(
            "SELECT * FROM wms_replenish_task WHERE status = 'PENDING' AND warehouse_code_col = #{warehouse} AND deleted = 0 ORDER BY priority ASC, created_time ASC")
    List<ReplenishTask> selectPendingTasks(@Param("warehouse") String warehouse);

    /** 查询某SKU是否有进行中的补货任务 */
    @Select(
            "SELECT COUNT(*) FROM wms_replenish_task WHERE sku = #{sku} AND status IN ('PENDING','ASSIGNED','PICKING','PICKED','PUTAWAYING') AND deleted = 0")
    int countActiveBySku(@Param("sku") String sku);
}
