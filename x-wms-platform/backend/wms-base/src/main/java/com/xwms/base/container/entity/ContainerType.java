package com.xwms.base.container.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 容器类型 */
@Data
@TableName("wms_container_type")
public class ContainerType {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String typeCode;
    private String typeName;

    /** 类别: PALLET/BOX/BAG/TOTE/CAGE */
    private String category;

    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;
    private BigDecimal maxWeight;
    private BigDecimal maxVolume;

    /** 皮重 */
    private BigDecimal tareWeight;

    /** 是否可循环使用 */
    private Integer reusable;

    /** 状态: ACTIVE/INACTIVE */
    private String status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
