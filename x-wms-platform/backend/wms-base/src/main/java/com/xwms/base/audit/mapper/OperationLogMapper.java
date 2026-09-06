package com.xwms.base.audit.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.audit.entity.OperationLog;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    @Select("SELECT * FROM sys_operation_log WHERE user_id = #{userId} ORDER BY created_time DESC")
    List<OperationLog> selectByUserId(@Param("userId") String userId);

    @Select(
            "SELECT * FROM sys_operation_log WHERE business_type = #{bizType} AND business_no = #{bizNo} ORDER BY created_time")
    List<OperationLog> selectByBusiness(
            @Param("bizType") String bizType, @Param("bizNo") String bizNo);
}
