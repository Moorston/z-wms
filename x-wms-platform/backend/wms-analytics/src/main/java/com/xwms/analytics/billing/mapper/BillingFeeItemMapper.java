package com.xwms.analytics.billing.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.analytics.billing.entity.BillingFeeItem;

@Mapper
public interface BillingFeeItemMapper extends BaseMapper<BillingFeeItem> {

    @Select(
            "SELECT * FROM wms_billing_fee_item WHERE owner_code = #{ownerCode} AND fee_period = #{period} AND status = 'PENDING' ORDER BY fee_date")
    List<BillingFeeItem> selectPendingByPeriod(
            @Param("ownerCode") String ownerCode, @Param("period") String period);

    @Select(
            "SELECT IFNULL(SUM(amount),0) FROM wms_billing_fee_item WHERE owner_code = #{ownerCode} AND fee_period = #{period} AND status = 'PENDING'")
    BigDecimal sumPendingByPeriod(
            @Param("ownerCode") String ownerCode, @Param("period") String period);
}
