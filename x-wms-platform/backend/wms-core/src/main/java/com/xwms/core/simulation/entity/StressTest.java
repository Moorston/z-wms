package com.xwms.core.simulation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_stress_test")
public class StressTest {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String testId;
    private String testName;
    private String warehouseCode;
    private String ownerCode;
    private String testType;
    private String description;
    private String testConfig;
    private Integer concurrency;
    private Long totalRequests;
    private Long successCount;
    private Long failureCount;
    private BigDecimal avgResponseTime;
    private BigDecimal maxResponseTime;
    private BigDecimal minResponseTime;
    private BigDecimal p50ResponseTime;
    private BigDecimal p95ResponseTime;
    private BigDecimal p99ResponseTime;
    private BigDecimal throughput;
    private BigDecimal errorRate;
    private String testResult;
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
