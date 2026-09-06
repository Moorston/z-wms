package com.xwms.base.carrier.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.carrier.entity.CarrierAccount;

@Mapper
public interface CarrierAccountMapper extends BaseMapper<CarrierAccount> {

    @Select(
            "SELECT * FROM wms_carrier_account WHERE carrier_code = #{carrierCode} AND status = 'ACTIVE'")
    List<CarrierAccount> selectByCarrierCode(@Param("carrierCode") String carrierCode);

    @Select(
            "SELECT * FROM wms_carrier_account WHERE carrier_code = #{carrierCode} AND account_no = #{accountNo}")
    CarrierAccount selectByCarrierAndAccount(
            @Param("carrierCode") String carrierCode, @Param("accountNo") String accountNo);

    /** 扣减账户余额（原子SQL+乐观锁） */
    @Update(
            "UPDATE wms_carrier_account SET balance = balance - #{amount}, updated_time = NOW() WHERE carrier_code = #{carrierCode} AND account_no = #{accountNo} AND balance >= #{amount} AND status = 'ACTIVE'")
    int deductBalance(
            @Param("carrierCode") String carrierCode,
            @Param("accountNo") String accountNo,
            @Param("amount") BigDecimal amount);

    /** 增加账户余额 */
    @Update(
            "UPDATE wms_carrier_account SET balance = balance + #{amount}, last_recharge_time = NOW(), updated_time = NOW() WHERE carrier_code = #{carrierCode} AND account_no = #{accountNo} AND status = 'ACTIVE'")
    int addBalance(
            @Param("carrierCode") String carrierCode,
            @Param("accountNo") String accountNo,
            @Param("amount") BigDecimal amount);
}
