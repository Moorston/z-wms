package com.xwms.analytics.billing.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.analytics.billing.entity.BillingBillItem;

@Mapper
public interface BillingBillItemMapper extends BaseMapper<BillingBillItem> {

    @Select(
            "SELECT * FROM wms_billing_bill_item WHERE bill_id = #{billId} ORDER BY fee_type, fee_date")
    List<BillingBillItem> selectByBillId(@Param("billId") Long billId);
}
