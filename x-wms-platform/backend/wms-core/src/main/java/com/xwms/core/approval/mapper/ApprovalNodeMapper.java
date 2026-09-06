package com.xwms.core.approval.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.approval.entity.ApprovalNode;

@Mapper
public interface ApprovalNodeMapper extends BaseMapper<ApprovalNode> {

    @Select(
            "SELECT * FROM wms_approval_node WHERE process_code = #{processCode} AND process_version = #{version} ORDER BY node_order")
    List<ApprovalNode> selectByProcess(
            @Param("processCode") String processCode, @Param("version") Integer version);

    @Select(
            "SELECT * FROM wms_approval_node WHERE process_code = #{processCode} AND process_version = #{version} AND node_code = #{nodeCode}")
    ApprovalNode selectByProcessAndNode(
            @Param("processCode") String processCode,
            @Param("version") Integer version,
            @Param("nodeCode") String nodeCode);

    @Select(
            "SELECT * FROM wms_approval_node WHERE process_code = #{processCode} AND process_version = #{version} AND node_type = 'START'")
    ApprovalNode selectStartNode(
            @Param("processCode") String processCode, @Param("version") Integer version);

    @Select(
            "SELECT * FROM wms_approval_node WHERE process_code = #{processCode} AND process_version = #{version} AND node_type = 'END'")
    ApprovalNode selectEndNode(
            @Param("processCode") String processCode, @Param("version") Integer version);
}
