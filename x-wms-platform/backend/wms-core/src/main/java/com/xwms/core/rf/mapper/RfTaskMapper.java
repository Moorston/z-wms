package com.xwms.core.rf.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.rf.entity.RfTask;

@Mapper
public interface RfTaskMapper extends BaseMapper<RfTask> {

    @Select(
            "SELECT * FROM wms_rf_task WHERE status = 'PENDING' AND task_type = #{type} AND warehouse_code = #{warehouse} ORDER BY priority DESC, created_time ASC")
    List<RfTask> selectPendingByType(
            @Param("type") String type, @Param("warehouse") String warehouse);

    @Select(
            "SELECT * FROM wms_rf_task WHERE assigned_to = #{userId} AND status IN ('ASSIGNED','IN_PROGRESS','PAUSED') ORDER BY created_time DESC")
    List<RfTask> selectMyTasks(@Param("userId") String userId);
}
