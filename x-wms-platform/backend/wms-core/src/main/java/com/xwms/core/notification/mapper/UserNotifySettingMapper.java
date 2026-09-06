package com.xwms.core.notification.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.notification.entity.UserNotifySetting;

@Mapper
public interface UserNotifySettingMapper extends BaseMapper<UserNotifySetting> {

    @Select("SELECT * FROM wms_user_notify_setting WHERE user_id = #{userId}")
    List<UserNotifySetting> selectByUserId(@Param("userId") Long userId);
}
