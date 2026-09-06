package com.xwms.core.security.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.security.entity.DataPermission;

@Mapper
public interface DataPermissionMapper extends BaseMapper<DataPermission> {

    @Select("SELECT * FROM wms_data_permission WHERE permission_code = #{permissionCode}")
    DataPermission selectByPermissionCode(@Param("permissionCode") String permissionCode);

    @Select("SELECT * FROM wms_data_permission WHERE role_code = #{roleCode} AND status = 'ACTIVE'")
    List<DataPermission> selectByRole(@Param("roleCode") String roleCode);

    @Select("SELECT * FROM wms_data_permission WHERE user_code = #{userCode} AND status = 'ACTIVE'")
    List<DataPermission> selectByUser(@Param("userCode") String userCode);

    @Select(
            "SELECT * FROM wms_data_permission WHERE permission_type = #{permissionType} AND status = 'ACTIVE'")
    List<DataPermission> selectByType(@Param("permissionType") String permissionType);

    @Select(
            "SELECT * FROM wms_data_permission WHERE resource_type = #{resourceType} AND status = 'ACTIVE'")
    List<DataPermission> selectByResourceType(@Param("resourceType") String resourceType);
}
