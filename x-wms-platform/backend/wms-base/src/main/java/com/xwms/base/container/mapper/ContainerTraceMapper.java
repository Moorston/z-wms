package com.xwms.base.container.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.container.entity.ContainerTrace;

@Mapper
public interface ContainerTraceMapper extends BaseMapper<ContainerTrace> {

    @Select(
            "SELECT * FROM wms_container_trace WHERE container_no = #{containerNo} ORDER BY action_time DESC")
    List<ContainerTrace> selectByContainerNo(@Param("containerNo") String containerNo);
}
