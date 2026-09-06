package com.xwms.base.tenant.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.tenant.entity.TenantQuota;

@Mapper
public interface TenantQuotaMapper extends BaseMapper<TenantQuota> {

    @Select(
            "SELECT * FROM wms_tenant_quota WHERE tenant_code = #{tenantCode} ORDER BY resource_type")
    List<TenantQuota> selectByTenant(@Param("tenantCode") String tenantCode);

    @Select(
            "SELECT * FROM wms_tenant_quota WHERE tenant_code = #{tenantCode} AND resource_type = #{type}")
    TenantQuota selectByTenantAndType(
            @Param("tenantCode") String tenantCode, @Param("type") String type);
}
