package com.xwms.base.container.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 容器档案 */
@Data
@TableName("wms_container")
public class Container {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 容器编号(条码) */
    private String containerNo;

    private String typeCode;
    private String warehouseCode;

    /** 状态: EMPTY/OCCUPIED/IN_TRANSIT/REPAIR/DISCARD */
    private String status;

    /** 当前位置(库位/月台/暂存区) */
    private String currentLocation;

    private BigDecimal currentLoad;
    private BigDecimal currentVolume;
    private Integer skuCount;
    private Integer itemCount;
    private Integer batchCount;

    private LocalDateTime lastCleanTime;
    private LocalDateTime lastCheckTime;

    /** 使用次数 */
    private Integer useCount;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
