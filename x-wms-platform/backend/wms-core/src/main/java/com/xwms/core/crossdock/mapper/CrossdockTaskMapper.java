package com.xwms.core.crossdock.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.crossdock.entity.CrossdockTask;

@Mapper
public interface CrossdockTaskMapper extends BaseMapper<CrossdockTask> {

    @Select(
            "SELECT * FROM wms_crossdock_task WHERE crossdock_no = #{crossdockNo} ORDER BY created_time")
    List<CrossdockTask> selectByCrossdockNo(@Param("crossdockNo") String crossdockNo);

    @Select("SELECT * FROM wms_crossdock_task WHERE task_no = #{taskNo}")
    CrossdockTask selectByTaskNo(@Param("taskNo") String taskNo);
}
