package com.xwms.core.statemachine.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.statemachine.entity.StateTransitionLog;

@Mapper
public interface StateTransitionLogMapper extends BaseMapper<StateTransitionLog> {}
