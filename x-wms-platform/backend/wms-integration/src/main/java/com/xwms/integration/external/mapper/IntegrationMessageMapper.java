package com.xwms.integration.external.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.integration.external.entity.IntegrationMessage;

@Mapper
public interface IntegrationMessageMapper extends BaseMapper<IntegrationMessage> {

    @Select(
            "SELECT * FROM wms_integration_message WHERE status = 'PENDING' AND next_retry_time <= NOW() ORDER BY created_time ASC")
    List<IntegrationMessage> selectPendingMessages();

    @Select(
            "SELECT * FROM wms_integration_message WHERE system_code = #{systemCode} AND message_type = #{messageType} ORDER BY created_time DESC")
    List<IntegrationMessage> selectBySystemAndType(
            @Param("systemCode") String systemCode, @Param("messageType") String messageType);
}
