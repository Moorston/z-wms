package com.xwms.core.palletbudget.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.palletbudget.entity.PalletBudget;

@Mapper
public interface PalletBudgetMapper extends BaseMapper<PalletBudget> {
    PalletBudget selectByBudgetNo(@Param("budgetNo") String budgetNo);

    List<PalletBudget> selectByAsnNo(@Param("asnNo") String asnNo);
}
