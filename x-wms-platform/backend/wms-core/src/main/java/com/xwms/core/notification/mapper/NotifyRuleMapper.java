package com.xwms.core.notification.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.notification.entity.NotifyRule;

@Mapper
public interface NotifyRuleMapper extends BaseMapper<NotifyRule> {

    @Select(
            "SELECT * FROM wms_notify_rule WHERE event_type = #{eventType} AND enabled = 1 ORDER BY priority DESC")
    List<NotifyRule> selectByEventType(@Param("eventType") String eventType);
}
