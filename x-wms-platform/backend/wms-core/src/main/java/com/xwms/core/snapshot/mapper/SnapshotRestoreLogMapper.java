package com.xwms.core.snapshot.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.snapshot.entity.SnapshotRestoreLog;

@Mapper
public interface SnapshotRestoreLogMapper extends BaseMapper<SnapshotRestoreLog> {

    @Select("SELECT * FROM wms_snapshot_restore_log WHERE restore_no = #{restoreNo}")
    SnapshotRestoreLog selectByRestoreNo(@Param("restoreNo") String restoreNo);

    @Update(
            "UPDATE wms_snapshot_restore_log SET status = #{status}, restore_time = NOW(), fail_reason = #{failReason} WHERE restore_no = #{restoreNo}")
    int updateStatus(
            @Param("restoreNo") String restoreNo,
            @Param("status") String status,
            @Param("failReason") String failReason);
}
