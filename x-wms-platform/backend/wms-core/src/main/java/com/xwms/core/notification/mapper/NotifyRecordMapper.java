package com.xwms.core.notification.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.notification.entity.NotifyRecord;

@Mapper
public interface NotifyRecordMapper extends BaseMapper<NotifyRecord> {

    @Select(
            "SELECT * FROM wms_notify_record WHERE receiver_id = #{receiverId} AND status IN ('SENT','READ') ORDER BY created_time DESC")
    List<NotifyRecord> selectByReceiver(@Param("receiverId") String receiverId);

    @Select(
            "SELECT COUNT(*) FROM wms_notify_record WHERE receiver_id = #{receiverId} AND status = 'SENT'")
    int countUnread(@Param("receiverId") String receiverId);

    @Select(
            "SELECT * FROM wms_notify_record WHERE status = 'PENDING' AND retry_count < max_retry ORDER BY priority DESC, created_time ASC")
    List<NotifyRecord> selectPending();
}
