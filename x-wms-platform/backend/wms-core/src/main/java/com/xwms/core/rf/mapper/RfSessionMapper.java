package com.xwms.core.rf.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.rf.entity.RfSession;

@Mapper
public interface RfSessionMapper extends BaseMapper<RfSession> {

    @Select("SELECT * FROM wms_rf_session WHERE session_id = #{sessionId}")
    RfSession selectBySessionId(@Param("sessionId") String sessionId);

    @Select("SELECT * FROM wms_rf_session WHERE user_id = #{userId} AND status = 'ACTIVE' LIMIT 1")
    RfSession selectActiveByUserId(@Param("userId") String userId);
}
