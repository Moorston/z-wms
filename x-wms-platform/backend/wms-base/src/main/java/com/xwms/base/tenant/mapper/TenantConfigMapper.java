package com.xwms.base.tenant.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.tenant.entity.TenantConfig;

@Mapper
public interface TenantConfigMapper extends BaseMapper<TenantConfig> {

    @Select("SELECT * FROM wms_tenant_config WHERE tenant_code = #{tenantCode} ORDER BY config_key")
    List<TenantConfig> selectByTenant(@Param("tenantCode") String tenantCode);

    @Select(
            "SELECT * FROM wms_tenant_config WHERE tenant_code = #{tenantCode} AND config_key = #{key}")
    TenantConfig selectByTenantAndKey(
            @Param("tenantCode") String tenantCode, @Param("key") String key);
}
