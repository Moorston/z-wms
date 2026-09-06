package com.xwms.base.partner.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.partner.entity.*;
import com.xwms.base.partner.service.PartnerManagementService;
import com.xwms.common.core.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 客户/货主档案管理 Controller */
@Tag(name = "客户/货主档案", description = "货主/客户/供应商/联系人")
@RestController
@RequestMapping("/api/partner")
@RequiredArgsConstructor
public class PartnerManagementController {

    private final PartnerManagementService partnerManagementService;

    // ============================================================
    // 货主管理
    // ============================================================

    @Operation(summary = "创建货主")
    @PostMapping("/owner")
    public Result<Owner> createOwner(@RequestBody Owner owner) {
        return Result.success(partnerManagementService.createOwner(owner));
    }

    @Operation(summary = "更新货主")
    @PutMapping("/owner")
    public Result<Owner> updateOwner(@RequestBody Owner owner) {
        return Result.success(partnerManagementService.updateOwner(owner));
    }

    @Operation(summary = "分页查询货主")
    @GetMapping("/owner")
    public Result<Page<Owner>> pageOwners(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String ownerType,
            @RequestParam(required = false) String status) {
        return Result.success(
                partnerManagementService.pageOwners(new Page<>(page, size), ownerType, status));
    }

    @Operation(summary = "查询活跃货主")
    @GetMapping("/owner/active")
    public Result<List<Owner>> getActiveOwners() {
        return Result.success(partnerManagementService.getActiveOwners());
    }

    @Operation(summary = "按编码查询货主")
    @GetMapping("/owner/{code}")
    public Result<Owner> getOwnerByCode(@PathVariable String code) {
        return Result.success(partnerManagementService.getOwnerByCode(code));
    }

    @Operation(summary = "冻结/解冻货主")
    @PutMapping("/owner/{code}/freeze")
    public Result<Owner> freezeOwner(
            @PathVariable String code, @RequestParam(defaultValue = "true") boolean freeze) {
        return Result.success(partnerManagementService.freezeOwner(code, freeze));
    }

    @Operation(summary = "更新信用额度使用")
    @PutMapping("/owner/{code}/credit")
    public Result<Owner> updateCreditUsed(
            @PathVariable String code,
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "true") boolean add) {
        return Result.success(partnerManagementService.updateCreditUsed(code, amount, add));
    }

    // ============================================================
    // 客户管理
    // ============================================================

    @Operation(summary = "创建客户")
    @PostMapping("/customer")
    public Result<Customer> createCustomer(@RequestBody Customer customer) {
        return Result.success(partnerManagementService.createCustomer(customer));
    }

    @Operation(summary = "更新客户")
    @PutMapping("/customer")
    public Result<Customer> updateCustomer(@RequestBody Customer customer) {
        return Result.success(partnerManagementService.updateCustomer(customer));
    }

    @Operation(summary = "分页查询客户")
    @GetMapping("/customer")
    public Result<Page<Customer>> pageCustomers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String customerType,
            @RequestParam(required = false) String status) {
        return Result.success(
                partnerManagementService.pageCustomers(
                        new Page<>(page, size), ownerCode, customerType, status));
    }

    @Operation(summary = "按货主查询客户")
    @GetMapping("/customer/owner/{ownerCode}")
    public Result<List<Customer>> getCustomersByOwner(@PathVariable String ownerCode) {
        return Result.success(partnerManagementService.getCustomersByOwner(ownerCode));
    }

    @Operation(summary = "按编码查询客户")
    @GetMapping("/customer/{code}")
    public Result<Customer> getCustomerByCode(@PathVariable String code) {
        return Result.success(partnerManagementService.getCustomerByCode(code));
    }

    // ============================================================
    // 供应商管理
    // ============================================================

    @Operation(summary = "创建供应商")
    @PostMapping("/supplier")
    public Result<Supplier> createSupplier(@RequestBody Supplier supplier) {
        return Result.success(partnerManagementService.createSupplier(supplier));
    }

    @Operation(summary = "更新供应商")
    @PutMapping("/supplier")
    public Result<Supplier> updateSupplier(@RequestBody Supplier supplier) {
        return Result.success(partnerManagementService.updateSupplier(supplier));
    }

    @Operation(summary = "分页查询供应商")
    @GetMapping("/supplier")
    public Result<Page<Supplier>> pageSuppliers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String supplierType,
            @RequestParam(required = false) String status) {
        return Result.success(
                partnerManagementService.pageSuppliers(
                        new Page<>(page, size), ownerCode, supplierType, status));
    }

    @Operation(summary = "按货主查询供应商")
    @GetMapping("/supplier/owner/{ownerCode}")
    public Result<List<Supplier>> getSuppliersByOwner(@PathVariable String ownerCode) {
        return Result.success(partnerManagementService.getSuppliersByOwner(ownerCode));
    }

    @Operation(summary = "按编码查询供应商")
    @GetMapping("/supplier/{code}")
    public Result<Supplier> getSupplierByCode(@PathVariable String code) {
        return Result.success(partnerManagementService.getSupplierByCode(code));
    }

    // ============================================================
    // 联系人管理
    // ============================================================

    @Operation(summary = "创建联系人")
    @PostMapping("/contact")
    public Result<Contact> createContact(@RequestBody Contact contact) {
        return Result.success(partnerManagementService.createContact(contact));
    }

    @Operation(summary = "更新联系人")
    @PutMapping("/contact")
    public Result<Contact> updateContact(@RequestBody Contact contact) {
        return Result.success(partnerManagementService.updateContact(contact));
    }

    @Operation(summary = "分页查询联系人")
    @GetMapping("/contact")
    public Result<Page<Contact>> pageContacts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String partnerType,
            @RequestParam(required = false) String partnerCode,
            @RequestParam(required = false) String status) {
        return Result.success(
                partnerManagementService.pageContacts(
                        new Page<>(page, size), partnerType, partnerCode, status));
    }

    @Operation(summary = "按合作伙伴查询联系人")
    @GetMapping("/contact/partner")
    public Result<List<Contact>> getContactsByPartner(
            @RequestParam String partnerType, @RequestParam String partnerCode) {
        return Result.success(
                partnerManagementService.getContactsByPartner(partnerType, partnerCode));
    }

    @Operation(summary = "按编码查询联系人")
    @GetMapping("/contact/{code}")
    public Result<Contact> getContactByCode(@PathVariable String code) {
        return Result.success(partnerManagementService.getContactByCode(code));
    }

    @Operation(summary = "获取主联系人")
    @GetMapping("/contact/primary")
    public Result<Contact> getPrimaryContact(
            @RequestParam String partnerType, @RequestParam String partnerCode) {
        return Result.success(partnerManagementService.getPrimaryContact(partnerType, partnerCode));
    }
}
