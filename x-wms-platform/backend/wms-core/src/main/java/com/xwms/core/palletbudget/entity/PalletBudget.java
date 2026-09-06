package com.xwms.core.palletbudget.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 码盘预算表 客户现场有较为细致的分货堆码要求，例如库内存储位高度不同，要求放到不同库位的托盘堆码层数不同 */
@Data
@TableName("wms_pallet_budget")
public class PalletBudget {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String budgetNo;
    private String asnNo;
    private String inboundNo;
    private String skuCode;
    private String skuName;
    private BigDecimal totalQty;
    private Integer boxQty;
    private Integer palletQty;
    private String locationCode;
    private BigDecimal locationHeight;
    private Integer layersPerPallet;
    private Integer boxesPerLayer;
    private BigDecimal boxHeight;
    private BigDecimal boxWeight;
    private String status; // PENDING待预算/BUDGETED已预算/EXECUTED已执行
    private String operator;
    private LocalDateTime budgetTime;
    private String remark;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;
}
