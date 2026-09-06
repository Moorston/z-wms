package com.xwms.core.security.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.security.entity.SecurityPolicy;

@Mapper
public interface SecurityPolicyMapper extends BaseMapper<SecurityPolicy> {

    @Select("SELECT * FROM wms_security_policy WHERE policy_code = #{policyCode}")
    SecurityPolicy selectByPolicyCode(@Param("policyCode") String policyCode);

    @Select(
            "SELECT * FROM wms_security_policy WHERE policy_type = #{policyType} AND is_enabled = 'Y' ORDER BY priority")
    List<SecurityPolicy> selectEnabledByType(@Param("policyType") String policyType);

    @Select(
            "SELECT * FROM wms_security_policy WHERE policy_scope = #{policyScope} AND scope_value = #{scopeValue} AND is_enabled = 'Y' ORDER BY priority")
    List<SecurityPolicy> selectByScope(
            @Param("policyScope") String policyScope, @Param("scopeValue") String scopeValue);

    @Select(
            "SELECT * FROM wms_security_policy WHERE is_enabled = 'Y' ORDER BY policy_type, priority")
    List<SecurityPolicy> selectAllEnabled();
}
