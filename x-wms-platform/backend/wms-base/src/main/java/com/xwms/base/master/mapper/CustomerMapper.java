package com.xwms.base.master.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.master.entity.Customer;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {}
