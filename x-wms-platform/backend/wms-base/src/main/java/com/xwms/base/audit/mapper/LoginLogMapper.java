package com.xwms.base.audit.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.audit.entity.LoginLog;

@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {

    @Select("SELECT * FROM sys_login_log WHERE user_id = #{userId} ORDER BY created_time DESC")
    List<LoginLog> selectByUserId(@Param("userId") String userId);
}
