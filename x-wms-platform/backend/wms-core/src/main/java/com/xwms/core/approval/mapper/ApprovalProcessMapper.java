package com.xwms.core.approval.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.approval.entity.ApprovalProcess;

@Mapper
public interface ApprovalProcessMapper extends BaseMapper<ApprovalProcess> {

    @Select(
            "SELECT * FROM wms_approval_process WHERE process_code = #{processCode} AND version = #{version}")
    ApprovalProcess selectByCodeAndVersion(
            @Param("processCode") String processCode, @Param("version") Integer version);

    @Select(
            "SELECT * FROM wms_approval_process WHERE process_type = #{processType} AND status = 'ACTIVE' ORDER BY version DESC LIMIT 1")
    ApprovalProcess selectLatestByType(@Param("processType") String processType);

    @Select(
            "SELECT * FROM wms_approval_process WHERE warehouse_code = #{warehouseCode} AND process_type = #{processType} AND status = 'ACTIVE' ORDER BY version DESC LIMIT 1")
    ApprovalProcess selectLatestByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode, @Param("processType") String processType);

    @Select(
            "SELECT * FROM wms_approval_process WHERE process_type = #{processType} AND status = 'ACTIVE'")
    List<ApprovalProcess> selectByType(@Param("processType") String processType);
}
