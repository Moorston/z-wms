package com.xwms.core.returnorder.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 播种任务表 退货ASN编组后的播种分货任务，支持初分和二分 */
@Data
@TableName("wms_sowing_task")
public class SowingTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 播种任务号 */
    private String taskNo;

    /** 编组号 */
    private String groupNo;

    /** 播种阶段：FIRST初分/SECOND二分 */
    private String sowingStage;

    /** 商品编码 */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 批次号 */
    private String batchNo;

    /** 总数量 */
    private BigDecimal totalQty;

    /** 已播种数量 */
    private BigDecimal sowedQty;

    /** 剩余数量 */
    private BigDecimal remainingQty;

    /** 播种位编码 */
    private String sowingLocation;

    /** 目标库位（二分后上架库位） */
    private String targetLocation;

    /** 状态：PENDING待播种/SEEDING播种中/SEEDED播种完成/COMPLETED已完成/CANCELLED已取消 */
    private String status;

    /** 播种模式：STATIC静态/DYNAMIC动态 */
    private String sowingMode;

    /** 播种次序（动态播种时临时绑定） */
    private Integer sowingSeq;

    /** 操作人 */
    private String operator;

    /** 播种开始时间 */
    private LocalDateTime startTime;

    /** 播种完成时间 */
    private LocalDateTime finishTime;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新人 */
    private String updatedBy;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
