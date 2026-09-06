package com.xwms.base.container.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.container.entity.ContainerBind;

@Mapper
public interface ContainerBindMapper extends BaseMapper<ContainerBind> {

    @Select(
            "SELECT * FROM wms_container_bind WHERE container_no = #{containerNo} AND status = 'BOUND' ORDER BY bind_time")
    List<ContainerBind> selectActiveByContainer(@Param("containerNo") String containerNo);

    @Select(
            "SELECT * FROM wms_container_bind WHERE ref_type = #{refType} AND ref_no = #{refNo} ORDER BY bind_time")
    List<ContainerBind> selectByRef(@Param("refType") String refType, @Param("refNo") String refNo);
}
