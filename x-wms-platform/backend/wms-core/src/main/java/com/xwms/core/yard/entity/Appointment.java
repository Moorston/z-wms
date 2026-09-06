package com.xwms.core.yard.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 预约单 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_appointment")
public class Appointment extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String appointNo;

    /** 预约类型: INBOUND/OUTBOUND/RETURN/TRANSFER */
    private String appointType;

    private String warehouseCode;

    /** 分配的月台 */
    private Long dockId;

    private String dockCode;

    private String carrierCode;
    private String carrierName;
    private String driverName;
    private String driverPhone;
    private String plateNo;

    /** 车型: 4.2米/6.8米/9.6米/13米/17.5米 */
    private String vehicleType;

    private BigDecimal vehicleLength;
    private BigDecimal vehicleWeight;

    private String contactName;
    private String contactPhone;

    /** 预约到达时间 */
    private LocalDateTime planArriveTime;

    /** 预计离开时间 */
    private LocalDateTime planLeaveTime;

    private LocalDateTime actualArriveTime;
    private LocalDateTime actualLeaveTime;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    /** 状态: PENDING/CONFIRMED/CANCELLED/ARRIVED/CHECKED_IN/LOADING/COMPLETED/NO_SHOW/OVERDUE */
    private String status;

    private String sourceOrderNo;
    private String sourceOrderType;

    private Integer palletCount;
    private Integer packageCount;
    private BigDecimal weight;
    private BigDecimal volume;

    private Integer priority;
    private String appointBy;
    private String confirmBy;
    private LocalDateTime confirmTime;
    private String cancelReason;
    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;
}
