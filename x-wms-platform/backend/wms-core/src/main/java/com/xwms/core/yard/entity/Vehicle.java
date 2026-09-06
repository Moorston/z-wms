package com.xwms.core.yard.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 车辆登记 */
@Data
@TableName("wms_vehicle")
public class Vehicle {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String plateNo;
    private String vehicleType;
    private BigDecimal vehicleLength;
    private BigDecimal vehicleWeight;
    private String carrierCode;
    private String carrierName;
    private String driverName;
    private String driverPhone;
    private String driverLicense;

    /** 状态: ACTIVE/DISABLED */
    private String status;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
