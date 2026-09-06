package com.xwms.base.audit.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.audit.entity.SecurityAudit;

@Mapper
public interface SecurityAuditMapper extends BaseMapper<SecurityAudit> {

    @Select(
            "SELECT * FROM sys_security_audit WHERE status = 'PENDING' ORDER BY risk_level DESC, created_time ASC")
    List<SecurityAudit> selectPending();
}
