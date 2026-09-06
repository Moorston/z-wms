package com.xwms.core.allocation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.allocation.entity.AllocationStrategy;

@Mapper
public interface AllocationStrategyMapper extends BaseMapper<AllocationStrategy> {

    @Select(
            "SELECT * FROM wms_allocation_strategy WHERE rule_code = #{ruleCode} AND status = 'ACTIVE'")
    AllocationStrategy selectByRuleCode(@Param("ruleCode") String ruleCode);
}
