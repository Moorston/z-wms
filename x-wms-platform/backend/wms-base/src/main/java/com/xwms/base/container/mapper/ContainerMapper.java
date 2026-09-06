package com.xwms.base.container.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.container.entity.Container;

@Mapper
public interface ContainerMapper extends BaseMapper<Container> {

    @Select("SELECT * FROM wms_container WHERE container_no = #{containerNo}")
    Container selectByContainerNo(@Param("containerNo") String containerNo);

    @Select(
            "SELECT * FROM wms_container WHERE type_code = #{typeCode} AND status = #{status} ORDER BY container_no")
    List<Container> selectByTypeAndStatus(
            @Param("typeCode") String typeCode, @Param("status") String status);

    @Select(
            "SELECT * FROM wms_container WHERE current_location = #{location} ORDER BY container_no")
    List<Container> selectByLocation(@Param("location") String location);

    @Select(
            "SELECT * FROM wms_container WHERE status = 'EMPTY' AND type_code = #{typeCode} AND warehouse_code = #{warehouseCode} ORDER BY container_no LIMIT 1")
    Container selectEmptyContainer(
            @Param("typeCode") String typeCode, @Param("warehouseCode") String warehouseCode);
}
