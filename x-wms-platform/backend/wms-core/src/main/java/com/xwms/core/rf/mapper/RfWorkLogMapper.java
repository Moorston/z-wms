package com.xwms.core.rf.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.rf.entity.RfWorkLog;

@Mapper
public interface RfWorkLogMapper extends BaseMapper<RfWorkLog> {

    @Select("SELECT * FROM wms_rf_work_log WHERE task_id = #{taskId} ORDER BY created_time ASC")
    List<RfWorkLog> selectByTaskId(@Param("taskId") Long taskId);

    @Select("SELECT * FROM wms_rf_work_log WHERE user_id = #{userId} ORDER BY created_time DESC")
    List<RfWorkLog> selectByUserId(@Param("userId") String userId);
}
