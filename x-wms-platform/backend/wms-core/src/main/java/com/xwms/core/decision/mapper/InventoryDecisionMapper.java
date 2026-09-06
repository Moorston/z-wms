package com.xwms.core.decision.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.decision.entity.InventoryDecision;

@Mapper
public interface InventoryDecisionMapper extends BaseMapper<InventoryDecision> {
    @Select("SELECT * FROM wms_inventory_decision WHERE decision_id = #{decisionId}")
    InventoryDecision selectByDecisionId(@Param("decisionId") String decisionId);

    @Select(
            "SELECT * FROM wms_inventory_decision WHERE warehouse_code = #{warehouseCode} AND decision_type = #{decisionType} ORDER BY decision_time DESC")
    List<InventoryDecision> selectByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode,
            @Param("decisionType") String decisionType);

    @Select(
            "SELECT * FROM wms_inventory_decision WHERE warehouse_code = #{warehouseCode} AND status = #{status} ORDER BY priority DESC, decision_time DESC")
    List<InventoryDecision> selectByWarehouseAndStatus(
            @Param("warehouseCode") String warehouseCode, @Param("status") String status);

    @Select(
            "SELECT * FROM wms_inventory_decision WHERE sku_code = #{skuCode} ORDER BY decision_time DESC")
    List<InventoryDecision> selectBySku(@Param("skuCode") String skuCode);
}
