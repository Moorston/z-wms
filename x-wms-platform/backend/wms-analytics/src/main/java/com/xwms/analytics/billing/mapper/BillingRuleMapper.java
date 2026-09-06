package com.xwms.analytics.billing.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.analytics.billing.entity.BillingRule;

@Mapper
public interface BillingRuleMapper extends BaseMapper<BillingRule> {

    /** 匹配计费规则: 货主+客户+类型 > 货主+类型 > 类型 > 默认 */
    @Select(
            "SELECT * FROM wms_billing_rule WHERE fee_type = #{feeType} AND owner_code = #{ownerCode} AND customer_code = #{customerCode} AND status = 'ENABLED' AND deleted = 0 LIMIT 1 ORDER BY priority DESC")
    BillingRule matchRule(
            @Param("feeType") String feeType,
            @Param("ownerCode") String ownerCode,
            @Param("customerCode") String customerCode);

    @Select(
            "SELECT * FROM wms_billing_rule WHERE fee_type = #{feeType} AND owner_code = #{ownerCode} AND (customer_code IS NULL OR customer_code = '') AND status = 'ENABLED' AND deleted = 0 LIMIT 1 ORDER BY priority DESC")
    BillingRule matchOwnerRule(
            @Param("feeType") String feeType, @Param("ownerCode") String ownerCode);

    @Select(
            "SELECT * FROM wms_billing_rule WHERE fee_type = #{feeType} AND (owner_code IS NULL OR owner_code = '') AND status = 'ENABLED' AND deleted = 0 LIMIT 1 ORDER BY priority DESC")
    BillingRule matchDefaultRule(@Param("feeType") String feeType);
}
