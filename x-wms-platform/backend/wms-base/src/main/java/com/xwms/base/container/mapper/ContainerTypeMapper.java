package com.xwms.base.container.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.container.entity.ContainerType;

@Mapper
public interface ContainerTypeMapper extends BaseMapper<ContainerType> {

    @Select("SELECT * FROM wms_container_type WHERE type_code = #{typeCode}")
    ContainerType selectByTypeCode(@Param("typeCode") String typeCode);

    @Select("SELECT * FROM wms_container_type WHERE category = #{category} AND status = 'ACTIVE'")
    List<ContainerType> selectByCategory(@Param("category") String category);
}
