package com.xwms.core.expiry.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.expiry.entity.CustomerExpiry;

/** 收货人效期管理Mapper */
@Mapper
public interface CustomerExpiryMapper extends BaseMapper<CustomerExpiry> {

    /** 根据收货人和商品查询效期要求（优先商品级，其次通用） */
    CustomerExpiry selectByCustomerAndSku(
            @Param("customerCode") String customerCode, @Param("skuCode") String skuCode);

    /** 根据收货人查询所有效期要求 */
    List<CustomerExpiry> selectByCustomer(@Param("customerCode") String customerCode);

    /** 查询所有启用的效期要求 */
    List<CustomerExpiry> selectAllEnabled();
}
