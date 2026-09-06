package com.xwms.base.master.controller;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.master.entity.Customer;
import com.xwms.base.master.mapper.CustomerMapper;
import com.xwms.common.core.PageResult;
import com.xwms.common.core.Result;

import lombok.RequiredArgsConstructor;

/** 客户档案Controller 出库收货方管理 */
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerMapper customerMapper;

    @PostMapping
    public Result<Customer> create(@RequestBody Customer customer) {
        customerMapper.insert(customer);
        return Result.success(customer);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Customer customer) {
        customer.setId(id);
        customerMapper.updateById(customer);
        return Result.success();
    }

    @GetMapping("/page")
    public Result<PageResult<Customer>> page(
            @RequestParam(required = false) String customerCode,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String customerType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<Customer> page =
                customerMapper.selectPage(
                        new Page<>(pageNum, pageSize),
                        new LambdaQueryWrapper<Customer>()
                                .like(customerCode != null, Customer::getCustomerCode, customerCode)
                                .like(customerName != null, Customer::getCustomerName, customerName)
                                .eq(ownerCode != null, Customer::getOwnerCode, ownerCode)
                                .eq(customerType != null, Customer::getCustomerType, customerType)
                                .orderByDesc(Customer::getCreatedAt));
        return Result.success(
                PageResult.of(
                        page.getRecords(),
                        page.getTotal(),
                        (int) page.getCurrent(),
                        (int) page.getSize()));
    }
}
