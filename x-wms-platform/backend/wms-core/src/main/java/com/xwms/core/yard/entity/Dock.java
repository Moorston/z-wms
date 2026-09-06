package com.xwms.core.yard.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.*;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 月台定义 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_dock")
public class Dock extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String dockCode;
    private String dockName;
    private String warehouseCode;
    private String areaCode;

    /** 月台类型: INBOUND/OUTBOUND/BOTH/REVERSE */
    private String dockType;

    /** 状态: IDLE/OCCUPIED/RESERVED/MAINTENANCE/DISABLED */
    private String status;

    /** 同时容纳车辆数 */
    private Integer capacity;

    private Integer hasDockLeveler;
    private Integer hasDockShelter;
    private Integer hasForklift;

    /** 最大车长(米) */
    private BigDecimal maxVehicleLength;

    /** 最大载重(吨) */
    private BigDecimal maxVehicleWeight;

    private Integer sortOrder;
    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;
}
