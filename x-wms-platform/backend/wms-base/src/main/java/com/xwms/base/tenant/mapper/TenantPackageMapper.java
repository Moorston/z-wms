package com.xwms.base.tenant.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.tenant.entity.TenantPackage;

@Mapper
public interface TenantPackageMapper extends BaseMapper<TenantPackage> {

    @Select("SELECT * FROM wms_tenant_package WHERE enabled = 1 ORDER BY sort_order")
    List<TenantPackage> selectEnabledPackages();
}
