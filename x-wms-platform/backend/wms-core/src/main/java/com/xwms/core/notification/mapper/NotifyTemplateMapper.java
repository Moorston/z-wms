package com.xwms.core.notification.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.notification.entity.NotifyTemplate;

@Mapper
public interface NotifyTemplateMapper extends BaseMapper<NotifyTemplate> {

    @Select(
            "SELECT * FROM wms_notify_template WHERE template_code = #{code} AND enabled = 1 AND deleted = 0")
    NotifyTemplate selectByCode(@Param("code") String code);
}
