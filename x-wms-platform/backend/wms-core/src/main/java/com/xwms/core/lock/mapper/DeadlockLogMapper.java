package com.xwms.core.lock.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.lock.entity.DeadlockLog;

@Mapper
public interface DeadlockLogMapper extends BaseMapper<DeadlockLog> {

    @Select("SELECT * FROM wms_deadlock_log WHERE log_id = #{logId}")
    DeadlockLog selectByLogId(@Param("logId") String logId);

    @Select("SELECT * FROM wms_deadlock_log ORDER BY detect_time DESC LIMIT #{limit}")
    List<DeadlockLog> selectRecent(@Param("limit") int limit);
}
