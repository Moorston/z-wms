package com.xwms.core.security.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.security.entity.OperationAudit;

@Mapper
public interface OperationAuditMapper extends BaseMapper<OperationAudit> {

    @Select("SELECT * FROM wms_operation_audit WHERE audit_id = #{auditId}")
    OperationAudit selectByAuditId(@Param("auditId") String auditId);

    @Select(
            "SELECT * FROM wms_operation_audit WHERE user_code = #{userCode} AND operation_time >= #{startTime} ORDER BY operation_time DESC LIMIT #{limit}")
    List<OperationAudit> selectByUserAndTime(
            @Param("userCode") String userCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("limit") int limit);

    @Select(
            "SELECT * FROM wms_operation_audit WHERE module = #{module} AND operation_time >= #{startTime} ORDER BY operation_time DESC LIMIT #{limit}")
    List<OperationAudit> selectByModuleAndTime(
            @Param("module") String module,
            @Param("startTime") LocalDateTime startTime,
            @Param("limit") int limit);

    @Select(
            "SELECT * FROM wms_operation_audit WHERE biz_type = #{bizType} AND biz_no = #{bizNo} ORDER BY operation_time")
    List<OperationAudit> selectByBiz(
            @Param("bizType") String bizType, @Param("bizNo") String bizNo);

    @Select(
            "SELECT COUNT(*) FROM wms_operation_audit WHERE user_code = #{userCode} AND operation_type = #{operationType} AND operation_time >= #{startTime}")
    int countByUserAndOperation(
            @Param("userCode") String userCode,
            @Param("operationType") String operationType,
            @Param("startTime") LocalDateTime startTime);
}
