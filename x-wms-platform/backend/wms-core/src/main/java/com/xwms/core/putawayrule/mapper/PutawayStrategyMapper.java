package com.xwms.core.putawayrule.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.putawayrule.entity.PutawayStrategy;

/** 上架策略Mapper */
@Mapper
public interface PutawayStrategyMapper extends BaseMapper<PutawayStrategy> {

    /** 根据策略编码查询 */
    PutawayStrategy selectByStrategyCode(@Param("strategyCode") String strategyCode);

    /** 根据规则编码查询关联的策略 */
    List<PutawayStrategy> selectByRuleCode(@Param("ruleCode") String ruleCode);

    /** 查询所有启用的策略 */
    List<PutawayStrategy> selectAllEnabled();
}
