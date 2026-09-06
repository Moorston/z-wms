package com.xwms.core.lock.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.lock.entity.LockWaitQueue;

@Mapper
public interface LockWaitQueueMapper extends BaseMapper<LockWaitQueue> {

    @Select("SELECT * FROM wms_lock_wait_queue WHERE wait_id = #{waitId}")
    LockWaitQueue selectByWaitId(@Param("waitId") String waitId);

    @Select(
            "SELECT * FROM wms_lock_wait_queue WHERE lock_key = #{lockKey} AND status = 'WAITING' ORDER BY priority DESC, wait_start_time")
    List<LockWaitQueue> selectWaitingByLockKey(@Param("lockKey") String lockKey);

    @Select(
            "SELECT * FROM wms_lock_wait_queue WHERE requester = #{requester} AND status = 'WAITING'")
    List<LockWaitQueue> selectWaitingByRequester(@Param("requester") String requester);

    @Update(
            "UPDATE wms_lock_wait_queue SET status = #{status}, acquire_time = NOW() WHERE wait_id = #{waitId}")
    int updateStatus(@Param("waitId") String waitId, @Param("status") String status);

    @Update(
            "UPDATE wms_lock_wait_queue SET status = 'TIMEOUT' WHERE status = 'WAITING' AND wait_timeout < NOW()")
    int timeoutExpiredWaits();
}
