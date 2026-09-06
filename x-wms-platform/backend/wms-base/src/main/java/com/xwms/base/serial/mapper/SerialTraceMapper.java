package com.xwms.base.serial.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.serial.entity.SerialTrace;

@Mapper
public interface SerialTraceMapper extends BaseMapper<SerialTrace> {

    @Select(
            "SELECT * FROM wms_serial_trace WHERE serial_no = #{serialNo} ORDER BY action_time DESC")
    List<SerialTrace> selectBySerialNo(@Param("serialNo") String serialNo);
}
