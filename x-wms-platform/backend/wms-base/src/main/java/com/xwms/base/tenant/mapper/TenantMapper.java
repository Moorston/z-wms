package com.xwms.base.tenant.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.tenant.entity.Tenant;

@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {

    @Select("SELECT * FROM wms_tenant WHERE tenant_code = #{code}")
    Tenant selectByCode(@Param("code") String code);

    @Select("SELECT * FROM wms_tenant WHERE status = 'ACTIVE' ORDER BY tenant_code")
    List<Tenant> selectActiveTenants();
}
