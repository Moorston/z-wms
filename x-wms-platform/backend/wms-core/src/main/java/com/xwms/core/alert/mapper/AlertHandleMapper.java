package com.xwms.core.alert.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.alert.entity.AlertHandle;

@Mapper
public interface AlertHandleMapper extends BaseMapper<AlertHandle> {

    @Select("SELECT * FROM wms_alert_handle WHERE alert_id = #{alertId} ORDER BY handle_time")
    List<AlertHandle> selectByAlertId(@Param("alertId") String alertId);

    @Select("SELECT * FROM wms_alert_handle WHERE handle_id = #{handleId}")
    AlertHandle selectByHandleId(@Param("handleId") String handleId);
}
