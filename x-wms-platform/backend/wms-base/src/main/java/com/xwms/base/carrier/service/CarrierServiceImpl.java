package com.xwms.base.carrier.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.carrier.entity.*;
import com.xwms.base.carrier.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 承运商管理核心服务 核心能力: 承运商档案/服务管理/价格计算/账户管理 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarrierServiceImpl {

    private final CarrierMapper carrierMapper;
    private final CarrierServiceMapper serviceMapper;
    private final CarrierPriceMapper priceMapper;
    private final CarrierAccountMapper accountMapper;

    // ============================================================
    // 1. 承运商档案管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Carrier createCarrier(Carrier carrier, String operator) {
        carrier.setStatus("ACTIVE");
        if (carrier.getPriority() == null) carrier.setPriority(5);
        carrier.setCreatedBy(operator);
        carrierMapper.insert(carrier);
        log.info("创建承运商: {}={}", carrier.getCarrierCode(), carrier.getCarrierName());
        return carrier;
    }

    @Transactional(rollbackFor = Exception.class)
    public Carrier updateCarrier(Carrier carrier) {
        carrierMapper.updateById(carrier);
        log.info("更新承运商: {}", carrier.getCarrierCode());
        return carrier;
    }

    public Page<Carrier> pageCarriers(Page<Carrier> page, String carrierType, String status) {
        LambdaQueryWrapper<Carrier> wrapper = new LambdaQueryWrapper<>();
        if (carrierType != null) wrapper.eq(Carrier::getCarrierType, carrierType);
        if (status != null) wrapper.eq(Carrier::getStatus, status);
        wrapper.orderByDesc(Carrier::getPriority);
        return carrierMapper.selectPage(page, wrapper);
    }

    public Carrier getCarrierByCode(String carrierCode) {
        return carrierMapper.selectByCarrierCode(carrierCode);
    }

    public List<Carrier> getActiveCarriers() {
        return carrierMapper.selectActiveCarriers();
    }

    // ============================================================
    // 2. 承运商服务管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public CarrierService createService(CarrierService service) {
        service.setStatus("ACTIVE");
        serviceMapper.insert(service);
        log.info("创建承运商服务: {}/{}", service.getCarrierCode(), service.getServiceCode());
        return service;
    }

    public List<CarrierService> getServicesByCarrier(String carrierCode) {
        return serviceMapper.selectByCarrierCode(carrierCode);
    }

    public CarrierService getService(String carrierCode, String serviceCode) {
        return serviceMapper.selectByCarrierAndService(carrierCode, serviceCode);
    }

    // ============================================================
    // 3. 运费计算
    // ============================================================

    /** 计算运费 公式: 首重价格 + (重量-首重)/续重单位 * 续重价格 + 基础费用 + 燃油附加费 */
    public BigDecimal calculateShippingFee(
            String carrierCode,
            String serviceCode,
            String regionCode,
            BigDecimal weight,
            BigDecimal volume) {
        CarrierPrice price = priceMapper.selectPrice(carrierCode, serviceCode, regionCode, weight);
        if (price == null) {
            log.warn(
                    "未找到价格配置: carrier={}, service={}, region={}, weight={}",
                    carrierCode,
                    serviceCode,
                    regionCode,
                    weight);
            return BigDecimal.ZERO;
        }

        BigDecimal fee = BigDecimal.ZERO;

        // 首重价格
        if (price.getFirstPrice() != null) {
            fee = fee.add(price.getFirstPrice());
        }

        // 续重价格
        if (weight != null
                && price.getFirstWeight() != null
                && price.getAdditionalWeight() != null
                && price.getAdditionalPrice() != null
                && weight.compareTo(price.getFirstWeight()) > 0) {
            BigDecimal additionalWeight = weight.subtract(price.getFirstWeight());
            BigDecimal additionalUnits =
                    additionalWeight.divide(price.getAdditionalWeight(), 0, RoundingMode.UP);
            fee = fee.add(additionalUnits.multiply(price.getAdditionalPrice()));
        }

        // 基础费用
        if (price.getBaseFee() != null) {
            fee = fee.add(price.getBaseFee());
        }

        // 燃油附加费
        if (price.getFuelSurcharge() != null
                && price.getFuelSurcharge().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal fuelFee =
                    fee.multiply(price.getFuelSurcharge())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            fee = fee.add(fuelFee);
        }

        // 其他费用
        if (price.getOtherFee() != null) {
            fee = fee.add(price.getOtherFee());
        }

        log.info(
                "计算运费: carrier={}, service={}, weight={}, fee={}",
                carrierCode,
                serviceCode,
                weight,
                fee);
        return fee;
    }

    /** 比价：选择最优承运商 */
    public Carrier selectBestCarrier(
            String regionCode, BigDecimal weight, String serviceType, String ownerCode) {
        List<Carrier> carriers = carrierMapper.selectActiveCarriers();
        Carrier bestCarrier = null;
        BigDecimal bestFee = null;

        for (Carrier carrier : carriers) {
            List<CarrierService> services =
                    serviceMapper.selectByCarrierCode(carrier.getCarrierCode());
            for (CarrierService service : services) {
                if (serviceType != null && !serviceType.equals(service.getServiceType())) {
                    continue;
                }
                BigDecimal fee =
                        calculateShippingFee(
                                carrier.getCarrierCode(),
                                service.getServiceCode(),
                                regionCode,
                                weight,
                                null);
                if (fee.compareTo(BigDecimal.ZERO) > 0
                        && (bestFee == null || fee.compareTo(bestFee) < 0)) {
                    bestFee = fee;
                    bestCarrier = carrier;
                }
            }
        }

        log.info(
                "比价选择最优承运商: region={}, weight={}, best={}, fee={}",
                regionCode,
                weight,
                bestCarrier != null ? bestCarrier.getCarrierCode() : null,
                bestFee);
        return bestCarrier;
    }

    // ============================================================
    // 4. 价格管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public CarrierPrice createPrice(CarrierPrice price) {
        price.setStatus("ACTIVE");
        priceMapper.insert(price);
        log.info(
                "创建承运商价格: {}/{}/{}",
                price.getCarrierCode(),
                price.getServiceCode(),
                price.getRegionCode());
        return price;
    }

    public List<CarrierPrice> getPricesByCarrierAndService(String carrierCode, String serviceCode) {
        return priceMapper.selectByCarrierAndService(carrierCode, serviceCode);
    }

    // ============================================================
    // 5. 账户管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public CarrierAccount createAccount(CarrierAccount account) {
        account.setStatus("ACTIVE");
        if (account.getBalance() == null) account.setBalance(BigDecimal.ZERO);
        accountMapper.insert(account);
        log.info("创建承运商账户: {}/{}", account.getCarrierCode(), account.getAccountNo());
        return account;
    }

    /** 扣减账户余额 */
    @Transactional(rollbackFor = Exception.class)
    public boolean deductBalance(String carrierCode, String accountNo, BigDecimal amount) {
        int rows = accountMapper.deductBalance(carrierCode, accountNo, amount);
        if (rows > 0) {
            log.info("扣减账户余额: {}/{}, amount={}", carrierCode, accountNo, amount);
            // 检查余额预警
            checkBalanceWarning(carrierCode, accountNo);
            return true;
        }
        log.warn("扣减账户余额失败: {}/{}, amount={}", carrierCode, accountNo, amount);
        return false;
    }

    /** 充值 */
    @Transactional(rollbackFor = Exception.class)
    public boolean recharge(String carrierCode, String accountNo, BigDecimal amount) {
        int rows = accountMapper.addBalance(carrierCode, accountNo, amount);
        if (rows > 0) {
            log.info("账户充值: {}/{}, amount={}", carrierCode, accountNo, amount);
            return true;
        }
        log.warn("账户充值失败: {}/{}, amount={}", carrierCode, accountNo, amount);
        return false;
    }

    /** 检查余额预警 */
    private void checkBalanceWarning(String carrierCode, String accountNo) {
        CarrierAccount account = accountMapper.selectByCarrierAndAccount(carrierCode, accountNo);
        if (account != null
                && account.getWarningBalance() != null
                && account.getBalance().compareTo(account.getWarningBalance()) <= 0) {
            log.warn(
                    "账户余额预警: {}/{}, balance={}, warning={}",
                    carrierCode,
                    accountNo,
                    account.getBalance(),
                    account.getWarningBalance());
            // TODO: 触发预警通知
        }
    }

    public List<CarrierAccount> getAccountsByCarrier(String carrierCode) {
        return accountMapper.selectByCarrierCode(carrierCode);
    }

    public CarrierAccount getAccount(String carrierCode, String accountNo) {
        return accountMapper.selectByCarrierAndAccount(carrierCode, accountNo);
    }
}
