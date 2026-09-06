package com.xwms.base.carrier.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 承运商服务 */
@Data
@TableName("wms_carrier_service")
public class CarrierService {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String carrierCode;
    private String serviceCode;
    private String serviceName;

    /** 服务类型: STANDARD/EXPRESS/NEXT_DAY/SAME_DAY/ECONOMY */
    private String serviceType;

    /** 预计时效(天) */
    private Integer estimatedDays;

    /** 支持区域 */
    private String supportedRegions;

    private BigDecimal weightLimit;
    private BigDecimal volumeLimit;

    /** 状态: ACTIVE/INACTIVE */
    private String status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
