package com.xwms.core.print.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.print.entity.PrintTask;

@Mapper
public interface PrintTaskMapper extends BaseMapper<PrintTask> {

    @Select(
            "SELECT * FROM wms_print_task WHERE business_type = #{bizType} AND business_no = #{bizNo} ORDER BY created_time DESC")
    List<PrintTask> selectByBusiness(
            @Param("bizType") String bizType, @Param("bizNo") String bizNo);

    @Select("SELECT * FROM wms_print_task WHERE status = 'PENDING' ORDER BY created_time ASC")
    List<PrintTask> selectPendingTasks();
}
