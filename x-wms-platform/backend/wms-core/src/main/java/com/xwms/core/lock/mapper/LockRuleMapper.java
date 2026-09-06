package com.xwms.core.lock.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.lock.entity.LockRule;

@Mapper
public interface LockRuleMapper extends BaseMapper<LockRule> {

    @Select("SELECT * FROM wms_lock_rule WHERE rule_code = #{ruleCode}")
    LockRule selectByRuleCode(@Param("ruleCode") String ruleCode);

    @Select(
            "SELECT * FROM wms_lock_rule WHERE lock_scope = #{lockScope} AND lock_type = #{lockType} AND status = 'ACTIVE' LIMIT 1")
    LockRule matchRule(@Param("lockScope") String lockScope, @Param("lockType") String lockType);
}
