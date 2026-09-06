package com.xwms.core.approval.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.approval.entity.ApprovalRecord;

@Mapper
public interface ApprovalRecordMapper extends BaseMapper<ApprovalRecord> {

    @Select(
            "SELECT * FROM wms_approval_record WHERE instance_id = #{instanceId} ORDER BY node_order, approve_time")
    List<ApprovalRecord> selectByInstanceId(@Param("instanceId") String instanceId);

    @Select(
            "SELECT * FROM wms_approval_record WHERE instance_id = #{instanceId} AND node_code = #{nodeCode} ORDER BY approve_time")
    List<ApprovalRecord> selectByInstanceAndNode(
            @Param("instanceId") String instanceId, @Param("nodeCode") String nodeCode);

    @Select(
            "SELECT * FROM wms_approval_record WHERE approver = #{approver} ORDER BY approve_time DESC LIMIT #{limit}")
    List<ApprovalRecord> selectByApprover(
            @Param("approver") String approver, @Param("limit") int limit);

    @Select(
            "SELECT COUNT(*) FROM wms_approval_record WHERE instance_id = #{instanceId} AND node_code = #{nodeCode} AND action = 'APPROVE'")
    int countApproveByNode(
            @Param("instanceId") String instanceId, @Param("nodeCode") String nodeCode);
}
