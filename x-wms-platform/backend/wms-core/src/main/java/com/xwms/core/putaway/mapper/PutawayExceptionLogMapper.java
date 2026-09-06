package com.xwms.core.putaway.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.putaway.entity.PutawayExceptionLog;

/** 上架例外日志Mapper */
@Mapper
public interface PutawayExceptionLogMapper extends BaseMapper<PutawayExceptionLog> {

    @Select(
            "SELECT * FROM wms_putaway_exception_log WHERE task_no = #{taskNo} ORDER BY operate_time DESC")
    List<PutawayExceptionLog> selectByTaskNo(@Param("taskNo") String taskNo);

    @Select(
            "SELECT * FROM wms_putaway_exception_log WHERE handle_status = 'PENDING' ORDER BY operate_time ASC")
    List<PutawayExceptionLog> selectPending();

    @Select(
            "SELECT * FROM wms_putaway_exception_log WHERE exception_type = #{exceptionType} AND handle_status = 'PENDING' ORDER BY operate_time ASC")
    List<PutawayExceptionLog> selectPendingByType(@Param("exceptionType") String exceptionType);
}
