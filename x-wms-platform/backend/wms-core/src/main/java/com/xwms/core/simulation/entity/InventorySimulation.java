package com.xwms.core.simulation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_inventory_simulation")
public class InventorySimulation {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String simulationId;
    private String simulationName;
    private String warehouseCode;
    private String ownerCode;
    private String simulationType;
    private String description;
    private String initialState;
    private String simulationConfig;
    private String simulationParams;
    private String simulationResult;
    private String simulationMetrics;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private BigDecimal progress;
    private String errorMessage;
    private String operator;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
