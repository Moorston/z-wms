package com.xwms.core.serial.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.serial.entity.SerialRule;

/** 序列号规则Mapper */
@Mapper
public interface SerialRuleMapper extends BaseMapper<SerialRule> {

    /** 根据规则编码查询 */
    SerialRule selectByRuleCode(@Param("ruleCode") String ruleCode);

    /** 根据商品和货主查询规则（优先商品级，其次通用） */
    SerialRule selectBySkuAndOwner(
            @Param("skuCode") String skuCode, @Param("ownerCode") String ownerCode);

    /** 查询所有启用的规则 */
    List<SerialRule> selectAllEnabled();
}
