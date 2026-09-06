package com.xwms.base.serial.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.serial.entity.SerialBind;

@Mapper
public interface SerialBindMapper extends BaseMapper<SerialBind> {

    @Select(
            "SELECT * FROM wms_serial_bind WHERE serial_no = #{serialNo} AND status = 'BOUND' ORDER BY bind_time")
    List<SerialBind> selectActiveBySerial(@Param("serialNo") String serialNo);

    @Select(
            "SELECT * FROM wms_serial_bind WHERE ref_type = #{refType} AND ref_no = #{refNo} ORDER BY bind_time")
    List<SerialBind> selectByRef(@Param("refType") String refType, @Param("refNo") String refNo);
}
