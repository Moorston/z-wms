package com.xwms.core.simulation.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_scenario_simulation")
public class ScenarioSimulation {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String scenarioId;
    private String scenarioName;
    private String warehouseCode;
    private String ownerCode;
    private String scenarioType;
    private String description;
    private String scenarioConfig;
    private String scenarioEvents;
    private String initialInventory;
    private String expectedResult;
    private String actualResult;
    private String deviationAnalysis;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private String operator;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
