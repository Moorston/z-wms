package com.xwms.core.pack.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 包裹 */
@Data
@TableName("wms_package")
public class PackageEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String packageNo;
    private String packNo;
    private String outboundNo;

    /** 包裹类型: BOX/BAG/PALLET/TUBE/CUSTOM */
    private String packageType;

    private String packageSize;
    private BigDecimal weight;
    private BigDecimal volume;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;
    private Integer itemCount;
    private Integer skuCount;

    private String trackingNo;
    private String carrier;

    /** 状态: CREATED/PACKED/WEIGHED/LABELED/SHIPPED */
    private String status;

    private String packer;
    private LocalDateTime packTime;
    private LocalDateTime weighTime;
    private LocalDateTime labelTime;
    private LocalDateTime shipTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
