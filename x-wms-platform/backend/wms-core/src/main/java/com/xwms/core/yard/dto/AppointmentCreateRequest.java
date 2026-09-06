package com.xwms.core.yard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/** 创建预约请求 */
@Data
public class AppointmentCreateRequest {
    private String appointType; // INBOUND/OUTBOUND/RETURN/TRANSFER
    private String warehouseCode;
    private String ownerCode;
    private String carrierCode;
    private String carrierName;
    private String driverName;
    private String driverPhone;
    private String plateNo;
    private String vehicleType;
    private BigDecimal vehicleLength;
    private BigDecimal vehicleWeight;
    private String contactName;
    private String contactPhone;
    private LocalDateTime planArriveTime;
    private LocalDateTime planLeaveTime;
    private String sourceOrderNo;
    private String sourceOrderType;
    private Integer palletCount;
    private Integer packageCount;
    private BigDecimal weight;
    private BigDecimal volume;
    private Integer priority;
    private String appointBy;
    private String remark;
    private Boolean autoAssignDock; // 是否自动分配月台
}
