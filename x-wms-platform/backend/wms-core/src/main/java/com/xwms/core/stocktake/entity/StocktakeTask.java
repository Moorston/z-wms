package com.xwms.core.stocktake.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 盘点任务 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_stocktake_task")
public class StocktakeTask extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String taskNo;
    private String taskName;

    /** 盘点类型: FULL/AREA/SKU/CYCLE/RANDOM */
    private String stocktakeType;

    private String warehouseCode;
    private String areaCode;
    private String locationFrom;
    private String locationTo;

    /** 指定SKU列表(JSON) */
    private String skuList;

    private String ownerCode;
    private String batchNo;

    /** ABC分类(循环盘点) */
    private String abcClass;

    /** 循环盘点计划ID */
    private Long cycleCountId;

    private LocalDateTime planStartTime;
    private LocalDateTime planEndTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;

    /** 状态: DRAFT/PENDING/COUNTING/RECOUNTING/ADJUSTING/COMPLETED/CANCELLED */
    private String status;

    /** 是否冻结库存 */
    private Integer freezeFlag;

    /** 是否盲盘 */
    private Integer blindCount;

    /** 复盘阈值 */
    private BigDecimal recountThreshold;

    private Integer priority;
    private String assignee;
    private String checker;

    private Integer totalSkuCount;
    private Integer totalLocationCount;
    private Integer countedSkuCount;
    private Integer diffSkuCount;
    private BigDecimal diffQty;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;
}
