package com.xwms.core.equipment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 设备维护记录 */
@Data
@TableName("wms_equipment_maintain")
public class EquipmentMaintain {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String maintainNo;
    private Long equipmentId;
    private String equipmentCode;

    /** 维护类型: DAILY/PREVENTIVE/CORRECTIVE/INSPECTION/UPGRADE */
    private String maintainType;

    /** 维护状态: PENDING/PROCESSING/COMPLETED/CANCELLED */
    private String maintainStatus;

    /** 故障描述 */
    private String faultDesc;

    /** 维护内容 */
    private String maintainDesc;

    private String maintainBy;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** 维护时长(分钟) */
    private Integer durationMin;

    /** 维护费用 */
    private BigDecimal costAmount;

    /** 更换配件(JSON) */
    private String partsReplaced;

    /** 结果: SUCCESS/FAILED/PARTIAL */
    private String result;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
