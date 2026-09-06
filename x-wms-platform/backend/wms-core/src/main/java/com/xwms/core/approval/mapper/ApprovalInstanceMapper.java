package com.xwms.core.approval.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.approval.entity.ApprovalInstance;

@Mapper
public interface ApprovalInstanceMapper extends BaseMapper<ApprovalInstance> {

    @Select("SELECT * FROM wms_approval_instance WHERE instance_id = #{instanceId}")
    ApprovalInstance selectByInstanceId(@Param("instanceId") String instanceId);

    @Select(
            "SELECT * FROM wms_approval_instance WHERE biz_type = #{bizType} AND biz_no = #{bizNo} ORDER BY created_time DESC")
    List<ApprovalInstance> selectByBiz(
            @Param("bizType") String bizType, @Param("bizNo") String bizNo);

    @Select(
            "SELECT * FROM wms_approval_instance WHERE submitter = #{submitter} ORDER BY created_time DESC LIMIT #{limit}")
    List<ApprovalInstance> selectBySubmitter(
            @Param("submitter") String submitter, @Param("limit") int limit);

    @Select(
            "SELECT * FROM wms_approval_instance WHERE current_approvers LIKE '%' || #{approver} || '%' AND status IN ('PENDING','APPROVING') ORDER BY created_time")
    List<ApprovalInstance> selectTodoByApprover(@Param("approver") String approver);

    @Select(
            "SELECT COUNT(*) FROM wms_approval_instance WHERE current_approvers LIKE '%' || #{approver} || '%' AND status IN ('PENDING','APPROVING')")
    int countTodoByApprover(@Param("approver") String approver);

    @Update(
            "UPDATE wms_approval_instance SET status = #{status}, current_node = #{currentNode}, current_node_name = #{currentNodeName}, current_approvers = #{currentApprovers}, approve_count = #{approveCount}, total_approvers = #{totalApprovers}, updated_time = NOW() WHERE instance_id = #{instanceId}")
    int updateCurrentNode(
            @Param("instanceId") String instanceId,
            @Param("status") String status,
            @Param("currentNode") String currentNode,
            @Param("currentNodeName") String currentNodeName,
            @Param("currentApprovers") String currentApprovers,
            @Param("approveCount") Integer approveCount,
            @Param("totalApprovers") Integer totalApprovers);

    @Update(
            "UPDATE wms_approval_instance SET status = #{status}, approve_time = NOW(), duration_ms = #{durationMs}, updated_time = NOW() WHERE instance_id = #{instanceId}")
    int completeInstance(
            @Param("instanceId") String instanceId,
            @Param("status") String status,
            @Param("durationMs") Long durationMs);
}
