package com.xwms.base.partner.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.partner.entity.Customer;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    @Select(
            "SELECT * FROM wms_customer WHERE owner_code_col = #{ownerCode} AND status = 'ACTIVE' ORDER BY customer_code")
    List<Customer> selectByOwner(@Param("ownerCode") String ownerCode);

    @Select("SELECT * FROM wms_customer WHERE customer_code = #{code}")
    Customer selectByCode(@Param("code") String code);
}
