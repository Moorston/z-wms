package com.xwms.core.putawayrule.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.putawayrule.entity.PutawayRuleLine;

/** 上架规则行Mapper */
@Mapper
public interface PutawayRuleLineMapper extends BaseMapper<PutawayRuleLine> {

    /** 根据规则ID查询所有规则行（按行号排序） */
    List<PutawayRuleLine> selectByRuleId(@Param("ruleId") Long ruleId);

    /** 根据规则ID删除所有规则行 */
    int deleteByRuleId(@Param("ruleId") Long ruleId);

    /** 批量插入规则行 */
    int batchInsert(@Param("list") List<PutawayRuleLine> list);
}
