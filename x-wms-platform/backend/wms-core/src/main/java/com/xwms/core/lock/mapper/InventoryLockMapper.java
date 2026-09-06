package com.xwms.core.lock.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.lock.entity.InventoryLock;

@Mapper
public interface InventoryLockMapper extends BaseMapper<InventoryLock> {

    @Select("SELECT * FROM wms_inventory_lock WHERE lock_id = #{lockId}")
    InventoryLock selectByLockId(@Param("lockId") String lockId);

    @Select(
            "SELECT * FROM wms_inventory_lock WHERE lock_key = #{lockKey} AND status = 'HELD' ORDER BY acquire_time")
    List<InventoryLock> selectHeldByLockKey(@Param("lockKey") String lockKey);

    @Select(
            "SELECT * FROM wms_inventory_lock WHERE business_no = #{businessNo} AND status = 'HELD'")
    List<InventoryLock> selectHeldByBusinessNo(@Param("businessNo") String businessNo);

    @Select("SELECT * FROM wms_inventory_lock WHERE holder = #{holder} AND status = 'HELD'")
    List<InventoryLock> selectHeldByHolder(@Param("holder") String holder);

    @Select(
            "SELECT COUNT(*) FROM wms_inventory_lock WHERE lock_key = #{lockKey} AND status = 'HELD' AND lock_type = 'EXCLUSIVE'")
    int countExclusiveLocks(@Param("lockKey") String lockKey);

    @Select(
            "SELECT COUNT(*) FROM wms_inventory_lock WHERE lock_key = #{lockKey} AND status = 'HELD'")
    int countHeldLocks(@Param("lockKey") String lockKey);

    @Update(
            "UPDATE wms_inventory_lock SET status = #{status}, release_time = NOW() WHERE lock_id = #{lockId}")
    int updateStatus(@Param("lockId") String lockId, @Param("status") String status);

    @Update(
            "UPDATE wms_inventory_lock SET status = 'TIMEOUT' WHERE status = 'HELD' AND expire_time < NOW()")
    int timeoutExpiredLocks();
}
