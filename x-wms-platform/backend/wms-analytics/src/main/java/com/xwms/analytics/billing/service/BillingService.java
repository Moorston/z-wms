package com.xwms.analytics.billing.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.analytics.billing.entity.*;
import com.xwms.analytics.billing.enums.BillStatus;
import com.xwms.analytics.billing.mapper.*;
import com.xwms.common.exception.BizException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 费收计费核心服务 包含: 计费规则管理/费用计算/费用入账/账单生成/账单确认/结算付款 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillingRuleMapper ruleMapper;
    private final BillingFeeItemMapper feeItemMapper;
    private final BillingBillMapper billMapper;
    private final BillingBillItemMapper billItemMapper;
    private final BillingSettlementMapper settlementMapper;

    private static final AtomicInteger FEE_SEQ = new AtomicInteger(0);
    private static final AtomicInteger BILL_SEQ = new AtomicInteger(0);
    private static final AtomicInteger SETTLE_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 计费规则管理
    // ============================================================

    public BillingRule createRule(BillingRule rule) {
        rule.setStatus("ENABLED");
        ruleMapper.insert(rule);
        log.info("创建计费规则: {}", rule.getRuleCode());
        return rule;
    }

    public BillingRule getRule(Long id) {
        BillingRule rule = ruleMapper.selectById(id);
        if (rule == null) throw new BizException("计费规则不存在: " + id);
        return rule;
    }

    public Page<BillingRule> pageRules(Page<BillingRule> page, String feeType, String status) {
        LambdaQueryWrapper<BillingRule> wrapper = new LambdaQueryWrapper<>();
        if (feeType != null) wrapper.eq(BillingRule::getFeeType, feeType);
        if (status != null) wrapper.eq(BillingRule::getStatus, status);
        wrapper.orderByDesc(BillingRule::getCreatedAt);
        return ruleMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. 费用计算与入账
    // ============================================================

    /**
     * 计算并创建费用项目
     *
     * @param feeType 费用类型
     * @param ownerCode 货主
     * @param customerCode 客户
     * @param refType 关联业务类型
     * @param refNo 关联业务单号
     * @param quantity 计费数量
     * @param weight 计费重量
     * @param volume 计费体积
     * @param days 计费天数
     */
    @Transactional(rollbackFor = Exception.class)
    public BillingFeeItem calculateAndCreateFee(
            String feeType,
            String ownerCode,
            String customerCode,
            String warehouseCode,
            String refType,
            String refNo,
            BigDecimal quantity,
            BigDecimal weight,
            BigDecimal volume,
            Integer days,
            String sku,
            String productName) {
        // 1. 匹配计费规则
        BillingRule rule = matchRule(feeType, ownerCode, customerCode);
        if (rule == null) {
            log.warn(
                    "未找到计费规则: feeType={}, owner={}, customer={}", feeType, ownerCode, customerCode);
            return null;
        }

        // 2. 计算费用
        BigDecimal amount = calculateFee(rule, quantity, weight, volume, days);

        // 3. 创建费用项目
        String feeNo = generateFeeNo();
        BillingFeeItem fee = new BillingFeeItem();
        fee.setFeeNo(feeNo);
        fee.setFeeType(feeType);
        fee.setRuleId(rule.getId());
        fee.setRuleCode(rule.getRuleCode());
        fee.setOwnerCode(ownerCode);
        fee.setCustomerCode(customerCode);
        fee.setWarehouseCode(warehouseCode);
        fee.setRefType(refType);
        fee.setRefNo(refNo);
        fee.setSku(sku);
        fee.setProductName(productName);
        fee.setQuantity(quantity);
        fee.setWeight(weight);
        fee.setVolume(volume);
        fee.setDays(days);
        fee.setChargeMode(rule.getChargeMode());
        fee.setUnitPrice(rule.getUnitPrice());
        fee.setAmount(amount);
        fee.setCurrency(rule.getCurrency() != null ? rule.getCurrency() : "CNY");
        fee.setFeeDate(LocalDate.now());
        fee.setFeePeriod(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")));
        fee.setStatus("PENDING");
        fee.setOwnerCodeCol(ownerCode);
        fee.setWarehouseCodeCol(warehouseCode);
        feeItemMapper.insert(fee);

        log.info("费用入账: {}, 类型={}, 金额={}, 关联={}", feeNo, feeType, amount, refNo);
        return fee;
    }

    /** 匹配计费规则 (三级优先级) */
    private BillingRule matchRule(String feeType, String ownerCode, String customerCode) {
        if (ownerCode != null && customerCode != null) {
            BillingRule rule = ruleMapper.matchRule(feeType, ownerCode, customerCode);
            if (rule != null) return rule;
        }
        if (ownerCode != null) {
            BillingRule rule = ruleMapper.matchOwnerRule(feeType, ownerCode);
            if (rule != null) return rule;
        }
        return ruleMapper.matchDefaultRule(feeType);
    }

    /** 计算费用 */
    private BigDecimal calculateFee(
            BillingRule rule,
            BigDecimal quantity,
            BigDecimal weight,
            BigDecimal volume,
            Integer days) {
        BigDecimal amount = BigDecimal.ZERO;
        String mode = rule.getChargeMode();
        BigDecimal price = rule.getUnitPrice() != null ? rule.getUnitPrice() : BigDecimal.ZERO;

        // 扣除免计费数量
        BigDecimal billableQty = quantity;
        if (rule.getFreeQty() != null && quantity != null) {
            billableQty = quantity.subtract(rule.getFreeQty()).max(BigDecimal.ZERO);
        }

        switch (mode) {
            case "PER_UNIT" ->
                    amount = billableQty != null ? price.multiply(billableQty) : BigDecimal.ZERO;
            case "PER_ORDER" -> amount = price;
            case "PER_WEIGHT" -> amount = weight != null ? price.multiply(weight) : BigDecimal.ZERO;
            case "PER_VOLUME" -> amount = volume != null ? price.multiply(volume) : BigDecimal.ZERO;
            case "PER_DAY" ->
                    amount =
                            days != null && billableQty != null
                                    ? price.multiply(billableQty).multiply(new BigDecimal(days))
                                    : BigDecimal.ZERO;
            case "PER_MONTH" ->
                    amount = billableQty != null ? price.multiply(billableQty) : BigDecimal.ZERO;
            case "FLAT" -> amount = price;
            case "STEP" -> amount = calculateStepFee(rule, billableQty);
            default -> amount = BigDecimal.ZERO;
        }

        // 最低/最高收费限制
        if (rule.getMinCharge() != null && amount.compareTo(rule.getMinCharge()) < 0) {
            amount = rule.getMinCharge();
        }
        if (rule.getMaxCharge() != null && amount.compareTo(rule.getMaxCharge()) > 0) {
            amount = rule.getMaxCharge();
        }

        return amount;
    }

    /** 阶梯计费 (简化实现) */
    private BigDecimal calculateStepFee(BillingRule rule, BigDecimal quantity) {
        // TODO: 解析stepConfig JSON, 按阶梯计算
        return rule.getUnitPrice() != null && quantity != null
                ? rule.getUnitPrice().multiply(quantity)
                : BigDecimal.ZERO;
    }

    // ============================================================

    // 3. 账单生成 (月结)
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public BillingBill generateMonthlyBill(String ownerCode, String period) {
        // 检查是否已生成
        BillingBill existing =
                billMapper.selectOne(
                        new LambdaQueryWrapper<BillingBill>()
                                .eq(BillingBill::getOwnerCode, ownerCode)
                                .eq(BillingBill::getBillPeriod, period)
                                .ne(BillingBill::getStatus, BillStatus.CANCELLED.getCode()));
        if (existing != null) {
            throw new BizException("该货主" + period + "期间账单已存在: " + existing.getBillNo());
        }

        // 查询待入账费用
        List<BillingFeeItem> fees = feeItemMapper.selectPendingByPeriod(ownerCode, period);
        if (fees.isEmpty()) {
            throw new BizException("该期间无待入账费用");
        }

        String billNo = generateBillNo();
        BigDecimal totalAmount =
                fees.stream()
                        .map(BillingFeeItem::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 创建账单
        BillingBill bill = new BillingBill();
        bill.setBillNo(billNo);
        bill.setBillType("MONTHLY");
        bill.setOwnerCode(ownerCode);
        bill.setWarehouseCode(fees.get(0).getWarehouseCode());
        bill.setBillPeriod(period);
        bill.setStartDate(LocalDate.parse(period + "01", DateTimeFormatter.ofPattern("yyyyMMdd")));
        bill.setEndDate(bill.getStartDate().plusMonths(1).minusDays(1));
        bill.setTotalAmount(totalAmount);
        bill.setPaidAmount(BigDecimal.ZERO);
        bill.setUnpaidAmount(totalAmount);
        bill.setDiscountAmount(BigDecimal.ZERO);
        bill.setFinalAmount(totalAmount);
        bill.setCurrency("CNY");
        bill.setStatus(BillStatus.DRAFT.getCode());
        bill.setIssueDate(LocalDate.now());
        bill.setDueDate(LocalDate.now().plusDays(30));
        bill.setOwnerCodeCol(ownerCode);
        bill.setWarehouseCodeCol(fees.get(0).getWarehouseCode());
        billMapper.insert(bill);

        // 创建账单明细 & 更新费用状态
        for (BillingFeeItem fee : fees) {
            BillingBillItem item = new BillingBillItem();
            item.setBillId(bill.getId());
            item.setBillNo(billNo);
            item.setFeeItemId(fee.getId());
            item.setFeeType(fee.getFeeType());
            item.setFeeName(fee.getFeeType());
            item.setQuantity(fee.getQuantity());
            item.setUnitPrice(fee.getUnitPrice());
            item.setAmount(fee.getAmount());
            item.setCurrency(fee.getCurrency());
            item.setFeeDate(fee.getFeeDate());
            item.setRefNo(fee.getRefNo());
            item.setOwnerCodeCol(ownerCode);
            item.setWarehouseCodeCol(fee.getWarehouseCode());
            billItemMapper.insert(item);

            fee.setStatus("BILLED");
            fee.setBillId(bill.getId());
            feeItemMapper.updateById(fee);
        }

        log.info(
                "生成月结账单: {}, 货主={}, 期间={}, 金额={}, 明细={}条",
                billNo,
                ownerCode,
                period,
                totalAmount,
                fees.size());
        return bill;
    }

    // ============================================================

    // 4. 账单确认/开票
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public BillingBill confirmBill(Long billId) {
        BillingBill bill = getBill(billId);
        if (!BillStatus.DRAFT.getCode().equals(bill.getStatus())
                && !BillStatus.PENDING.getCode().equals(bill.getStatus())) {
            throw new BizException("账单状态不允许确认: " + bill.getStatus());
        }
        bill.setStatus(BillStatus.CONFIRMED.getCode());
        billMapper.updateById(bill);
        log.info("账单{}确认", bill.getBillNo());
        return bill;
    }

    @Transactional(rollbackFor = Exception.class)
    public BillingBill invoiceBill(Long billId, String invoiceNo) {
        BillingBill bill = getBill(billId);
        if (!BillStatus.CONFIRMED.getCode().equals(bill.getStatus())) {
            throw new BizException("只有已确认账单才能开票: " + bill.getStatus());
        }
        bill.setStatus(BillStatus.INVOICED.getCode());
        bill.setInvoiceNo(invoiceNo);
        billMapper.updateById(bill);
        log.info("账单{}开票: {}", bill.getBillNo(), invoiceNo);
        return bill;
    }

    // ============================================================

    // 5. 结算付款
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public BillingSettlement settleBill(
            Long billId,
            BigDecimal amount,
            String paymentMethod,
            String paymentRef,
            String operator) {
        BillingBill bill = getBill(billId);
        if (!BillStatus.CONFIRMED.getCode().equals(bill.getStatus())
                && !BillStatus.INVOICED.getCode().equals(bill.getStatus())
                && !BillStatus.PARTIAL_PAID.getCode().equals(bill.getStatus())) {
            throw new BizException("账单状态不允许结算: " + bill.getStatus());
        }

        if (amount.compareTo(bill.getUnpaidAmount()) > 0) {
            throw new BizException("付款金额不能超过未付金额: " + bill.getUnpaidAmount());
        }

        String settleNo = generateSettleNo();
        BillingSettlement settlement = new BillingSettlement();
        settlement.setSettlementNo(settleNo);
        settlement.setBillId(billId);
        settlement.setBillNo(bill.getBillNo());
        settlement.setOwnerCode(bill.getOwnerCode());
        settlement.setSettlementType(
                amount.compareTo(bill.getUnpaidAmount()) == 0 ? "FULL" : "PARTIAL");
        settlement.setAmount(amount);
        settlement.setCurrency(bill.getCurrency());
        settlement.setPaymentMethod(paymentMethod);
        settlement.setPaymentRef(paymentRef);
        settlement.setSettlementDate(LocalDate.now());
        settlement.setStatus("COMPLETED");
        settlement.setOperator(operator);
        settlement.setOwnerCodeCol(bill.getOwnerCodeCol());
        settlement.setWarehouseCodeCol(bill.getWarehouseCodeCol());
        settlementMapper.insert(settlement);

        // 更新账单付款状态
        bill.setPaidAmount(bill.getPaidAmount().add(amount));
        bill.setUnpaidAmount(bill.getUnpaidAmount().subtract(amount));
        if (bill.getUnpaidAmount().compareTo(BigDecimal.ZERO) == 0) {
            bill.setStatus(BillStatus.PAID.getCode());
            bill.setPaidDate(LocalDate.now());
        } else {
            bill.setStatus(BillStatus.PARTIAL_PAID.getCode());
        }
        billMapper.updateById(bill);

        log.info(
                "账单{}结算: 金额={}, 方式={}, 剩余未付={}",
                bill.getBillNo(),
                amount,
                paymentMethod,
                bill.getUnpaidAmount());
        return settlement;
    }

    // ============================================================

    // 6. 查询
    // ============================================================

    public BillingBill getBill(Long id) {
        BillingBill bill = billMapper.selectById(id);
        if (bill == null) throw new BizException("账单不存在: " + id);
        return bill;
    }

    public Page<BillingBill> pageBills(
            Page<BillingBill> page, String status, String ownerCode, String period) {
        LambdaQueryWrapper<BillingBill> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(BillingBill::getStatus, status);
        if (ownerCode != null) wrapper.eq(BillingBill::getOwnerCode, ownerCode);
        if (period != null) wrapper.eq(BillingBill::getBillPeriod, period);
        wrapper.orderByDesc(BillingBill::getCreatedAt);
        return billMapper.selectPage(page, wrapper);
    }

    public List<BillingBillItem> getBillItems(Long billId) {
        return billItemMapper.selectByBillId(billId);
    }

    public List<BillingSettlement> getSettlements(Long billId) {
        return settlementMapper.selectByBillId(billId);
    }

    public Page<BillingFeeItem> pageFees(
            Page<BillingFeeItem> page, String status, String ownerCode, String period) {
        LambdaQueryWrapper<BillingFeeItem> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(BillingFeeItem::getStatus, status);
        if (ownerCode != null) wrapper.eq(BillingFeeItem::getOwnerCode, ownerCode);
        if (period != null) wrapper.eq(BillingFeeItem::getFeePeriod, period);
        wrapper.orderByDesc(BillingFeeItem::getCreatedTime);
        return feeItemMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateFeeNo() {
        return "FEE"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", FEE_SEQ.incrementAndGet() % 1000);
    }

    private String generateBillNo() {
        return "BILL"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", BILL_SEQ.incrementAndGet() % 1000);
    }

    private String generateSettleNo() {
        return "SET"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SETTLE_SEQ.incrementAndGet() % 1000);
    }
}
