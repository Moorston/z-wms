package com.xwms.core.decision.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.decision.entity.InventoryDiagnosis;

@Mapper
public interface InventoryDiagnosisMapper extends BaseMapper<InventoryDiagnosis> {
    @Select("SELECT * FROM wms_inventory_diagnosis WHERE diagnosis_id = #{diagnosisId}")
    InventoryDiagnosis selectByDiagnosisId(@Param("diagnosisId") String diagnosisId);

    @Select(
            "SELECT * FROM wms_inventory_diagnosis WHERE warehouse_code = #{warehouseCode} AND diagnosis_type = #{diagnosisType} ORDER BY diagnosis_time DESC")
    List<InventoryDiagnosis> selectByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode,
            @Param("diagnosisType") String diagnosisType);

    @Select(
            "SELECT * FROM wms_inventory_diagnosis WHERE severity = #{severity} AND status = 'PENDING' ORDER BY priority DESC, diagnosis_time DESC")
    List<InventoryDiagnosis> selectPendingBySeverity(@Param("severity") String severity);

    @Select(
            "SELECT * FROM wms_inventory_diagnosis WHERE warehouse_code = #{warehouseCode} AND status = #{status} ORDER BY severity DESC, diagnosis_time DESC")
    List<InventoryDiagnosis> selectByWarehouseAndStatus(
            @Param("warehouseCode") String warehouseCode, @Param("status") String status);
}
