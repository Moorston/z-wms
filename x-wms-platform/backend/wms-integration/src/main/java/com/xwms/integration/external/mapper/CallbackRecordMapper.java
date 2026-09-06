package com.xwms.integration.external.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.integration.external.entity.CallbackRecord;

@Mapper
public interface CallbackRecordMapper extends BaseMapper<CallbackRecord> {

    @Select(
            "SELECT * FROM wms_callback_record WHERE status = 'PENDING' AND next_retry_time <= NOW() ORDER BY created_time ASC")
    List<CallbackRecord> selectPendingCallbacks();
}
