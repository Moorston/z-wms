package com.xwms.base.partner.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.partner.entity.*;
import com.xwms.base.partner.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 客户/货主档案管理核心服务 包含: 货主/客户/供应商/联系人 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerManagementService {

    private final OwnerMapper ownerMapper;
    private final CustomerMapper customerMapper;
    private final SupplierMapper supplierMapper;
    private final ContactMapper contactMapper;

    // ============================================================
    // 1. 货主管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Owner createOwner(Owner owner) {
        if (owner.getStatus() == null) owner.setStatus("ACTIVE");
        if (owner.getCreditUsed() == null) owner.setCreditUsed(BigDecimal.ZERO);
        ownerMapper.insert(owner);
        log.info("创建货主: {}={}", owner.getOwnerCode(), owner.getOwnerName());
        return owner;
    }

    @Transactional(rollbackFor = Exception.class)
    public Owner updateOwner(Owner owner) {
        ownerMapper.updateById(owner);
        return owner;
    }

    public Page<Owner> pageOwners(Page<Owner> page, String ownerType, String status) {
        LambdaQueryWrapper<Owner> wrapper = new LambdaQueryWrapper<>();
        if (ownerType != null) wrapper.eq(Owner::getOwnerType, ownerType);
        if (status != null) wrapper.eq(Owner::getStatus, status);
        wrapper.orderByAsc(Owner::getOwnerCode);
        return ownerMapper.selectPage(page, wrapper);
    }

    public List<Owner> getActiveOwners() {
        return ownerMapper.selectActiveOwners();
    }

    public Owner getOwnerByCode(String ownerCode) {
        return ownerMapper.selectByCode(ownerCode);
    }

    /** 冻结/解冻货主 */
    @Transactional(rollbackFor = Exception.class)
    public Owner freezeOwner(String ownerCode, boolean freeze) {
        Owner owner = getOwnerByCode(ownerCode);
        if (owner == null) throw new RuntimeException("货主不存在: " + ownerCode);
        owner.setStatus(freeze ? "FROZEN" : "ACTIVE");
        ownerMapper.updateById(owner);
        log.info("货主{}: {}", freeze ? "冻结" : "解冻", ownerCode);
        return owner;
    }

    /** 更新信用额度使用 */
    @Transactional(rollbackFor = Exception.class)
    public Owner updateCreditUsed(String ownerCode, BigDecimal amount, boolean add) {
        Owner owner = getOwnerByCode(ownerCode);
        if (owner == null) throw new RuntimeException("货主不存在: " + ownerCode);
        BigDecimal current =
                owner.getCreditUsed() != null ? owner.getCreditUsed() : BigDecimal.ZERO;
        owner.setCreditUsed(add ? current.add(amount) : current.subtract(amount));
        // 信用额度检查
        if (owner.getCreditLimit() != null
                && owner.getCreditUsed().compareTo(owner.getCreditLimit()) > 0) {
            log.warn(
                    "货主信用额度超限: owner={}, used={}, limit={}",
                    ownerCode,
                    owner.getCreditUsed(),
                    owner.getCreditLimit());
        }
        ownerMapper.updateById(owner);
        return owner;
    }

    // ============================================================
    // 2. 客户管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Customer createCustomer(Customer customer) {
        if (customer.getStatus() == null) customer.setStatus("ACTIVE");
        customerMapper.insert(customer);
        log.info("创建客户: {}={}", customer.getCustomerCode(), customer.getCustomerName());
        return customer;
    }

    @Transactional(rollbackFor = Exception.class)
    public Customer updateCustomer(Customer customer) {
        customerMapper.updateById(customer);
        return customer;
    }

    public Page<Customer> pageCustomers(
            Page<Customer> page, String ownerCode, String customerType, String status) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (ownerCode != null) wrapper.eq(Customer::getOwnerCodeCol, ownerCode);
        if (customerType != null) wrapper.eq(Customer::getCustomerType, customerType);
        if (status != null) wrapper.eq(Customer::getStatus, status);
        wrapper.orderByAsc(Customer::getOwnerCodeCol).orderByAsc(Customer::getCustomerCode);
        return customerMapper.selectPage(page, wrapper);
    }

    public List<Customer> getCustomersByOwner(String ownerCode) {
        return customerMapper.selectByOwner(ownerCode);
    }

    public Customer getCustomerByCode(String customerCode) {
        return customerMapper.selectByCode(customerCode);
    }

    // ============================================================
    // 3. 供应商管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Supplier createSupplier(Supplier supplier) {
        if (supplier.getStatus() == null) supplier.setStatus("ACTIVE");
        supplierMapper.insert(supplier);
        log.info("创建供应商: {}={}", supplier.getSupplierCode(), supplier.getSupplierName());
        return supplier;
    }

    @Transactional(rollbackFor = Exception.class)
    public Supplier updateSupplier(Supplier supplier) {
        supplierMapper.updateById(supplier);
        return supplier;
    }

    public Page<Supplier> pageSuppliers(
            Page<Supplier> page, String ownerCode, String supplierType, String status) {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        if (ownerCode != null) wrapper.eq(Supplier::getOwnerCodeCol, ownerCode);
        if (supplierType != null) wrapper.eq(Supplier::getSupplierType, supplierType);
        if (status != null) wrapper.eq(Supplier::getStatus, status);
        wrapper.orderByAsc(Supplier::getOwnerCodeCol).orderByAsc(Supplier::getSupplierCode);
        return supplierMapper.selectPage(page, wrapper);
    }

    public List<Supplier> getSuppliersByOwner(String ownerCode) {
        return supplierMapper.selectByOwner(ownerCode);
    }

    public Supplier getSupplierByCode(String supplierCode) {
        return supplierMapper.selectByCode(supplierCode);
    }

    // ============================================================
    // 4. 联系人管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Contact createContact(Contact contact) {
        if (contact.getStatus() == null) contact.setStatus("ACTIVE");
        if (contact.getIsPrimary() == null) contact.setIsPrimary(0);
        // 如果是主联系人，取消其他主联系人
        if (contact.getIsPrimary() == 1) {
            cancelPrimaryContact(contact.getPartnerType(), contact.getPartnerCode());
        }
        contactMapper.insert(contact);
        log.info("创建联系人: {}={}", contact.getContactCode(), contact.getContactName());
        return contact;
    }

    @Transactional(rollbackFor = Exception.class)
    public Contact updateContact(Contact contact) {
        if (contact.getIsPrimary() != null && contact.getIsPrimary() == 1) {
            cancelPrimaryContact(contact.getPartnerType(), contact.getPartnerCode());
        }
        contactMapper.updateById(contact);
        return contact;
    }

    public Page<Contact> pageContacts(
            Page<Contact> page, String partnerType, String partnerCode, String status) {
        LambdaQueryWrapper<Contact> wrapper = new LambdaQueryWrapper<>();
        if (partnerType != null) wrapper.eq(Contact::getPartnerType, partnerType);
        if (partnerCode != null) wrapper.eq(Contact::getPartnerCode, partnerCode);
        if (status != null) wrapper.eq(Contact::getStatus, status);
        wrapper.orderByDesc(Contact::getIsPrimary).orderByAsc(Contact::getContactCode);
        return contactMapper.selectPage(page, wrapper);
    }

    public List<Contact> getContactsByPartner(String partnerType, String partnerCode) {
        return contactMapper.selectByPartner(partnerType, partnerCode);
    }

    public Contact getContactByCode(String contactCode) {
        return contactMapper.selectByCode(contactCode);
    }

    /** 获取主联系人 */
    public Contact getPrimaryContact(String partnerType, String partnerCode) {
        return contactMapper.selectOne(
                new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getPartnerType, partnerType)
                        .eq(Contact::getPartnerCode, partnerCode)
                        .eq(Contact::getIsPrimary, 1)
                        .last("LIMIT 1"));
    }

    private void cancelPrimaryContact(String partnerType, String partnerCode) {
        List<Contact> primaries =
                contactMapper.selectList(
                        new LambdaQueryWrapper<Contact>()
                                .eq(Contact::getPartnerType, partnerType)
                                .eq(Contact::getPartnerCode, partnerCode)
                                .eq(Contact::getIsPrimary, 1));
        for (Contact c : primaries) {
            c.setIsPrimary(0);
            contactMapper.updateById(c);
        }
    }
}
