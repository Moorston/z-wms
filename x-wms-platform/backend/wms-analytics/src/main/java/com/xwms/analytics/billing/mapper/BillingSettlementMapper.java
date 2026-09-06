package com.xwms.analytics.billing.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.analytics.billing.entity.BillingSettlement;

@Mapper
public interface BillingSettlementMapper extends BaseMapper<BillingSettlement> {

    @Select(
            "SELECT * FROM wms_billing_settlement WHERE bill_id = #{billId} ORDER BY settlement_date")
    List<BillingSettlement> selectByBillId(@Param("billId") Long billId);
}
