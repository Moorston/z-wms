package com.xwms.core.dataquality.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.dataquality.entity.DqIssue;

@Mapper
public interface DqIssueMapper extends BaseMapper<DqIssue> {
    @Select("SELECT * FROM wms_dq_issue WHERE issue_id = #{issueId}")
    DqIssue selectByIssueId(@Param("issueId") String issueId);

    @Select(
            "SELECT * FROM wms_dq_issue WHERE check_id = #{checkId} ORDER BY severity, created_time")
    List<DqIssue> selectByCheckId(@Param("checkId") String checkId);

    @Select(
            "SELECT * FROM wms_dq_issue WHERE warehouse_code = #{warehouseCode} AND status = #{status} ORDER BY severity, priority, created_time")
    List<DqIssue> selectByWarehouseAndStatus(
            @Param("warehouseCode") String warehouseCode, @Param("status") String status);

    @Select(
            "SELECT * FROM wms_dq_issue WHERE assignee = #{assignee} AND status NOT IN ('CLOSED','IGNORED') ORDER BY due_time")
    List<DqIssue> selectOpenByAssignee(@Param("assignee") String assignee);

    @Select(
            "SELECT * FROM wms_dq_issue WHERE severity = #{severity} AND status NOT IN ('CLOSED','IGNORED') ORDER BY created_time DESC")
    List<DqIssue> selectOpenBySeverity(@Param("severity") String severity);
}
